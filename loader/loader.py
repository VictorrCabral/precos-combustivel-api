"""
Carrega o Levantamento de Precos de Combustiveis da ANP no Postgres.

O arquivo da ANP costuma vir com estas armadilhas:
  - vem em XLSX (planilha), com linhas de titulo antes da tabela
  - a serie historica vem em CSV com separador ponto e virgula
  - encoding latin-1 (cp1252), nao utf-8
  - decimal com virgula ("6,49")

Uso:
    python loader.py --arquivo "dados/precos-postos-2026-08.xlsx"
    python loader.py --url https://.../arquivo.csv

O nome das colunas ja mudou algumas vezes ao longo dos anos, entao a busca de
coluna e tolerante: ignora acento, caixa e espacos.
"""

from __future__ import annotations

import argparse
import io
import os
import sys
import unicodedata
from datetime import date
from urllib.parse import urlparse

import pandas as pd
import psycopg
import requests

# --------------------------------------------------------------------------- #
# Normalizacao
# --------------------------------------------------------------------------- #

def sem_acento(texto: str) -> str:
    """Remove acentos: 'Município' -> 'Municipio'."""
    nfkd = unicodedata.normalize("NFKD", str(texto))
    return "".join(c for c in nfkd if not unicodedata.combining(c))


def chave_coluna(nome: str) -> str:
    """Normaliza o nome de uma coluna para comparacao tolerante.

    'Regiao - Sigla' e 'REGIAO_SIGLA' viram os dois 'regiao sigla'.
    """
    texto = sem_acento(nome).lower().replace("-", " ").replace("_", " ")
    return " ".join(texto.split())


def para_decimal(valor) -> float | None:
    """'6,49' -> 6.49 ; '' -> None. A ANP usa virgula como separador decimal."""
    if valor is None or (isinstance(valor, float) and pd.isna(valor)):
        return None
    texto = str(valor).strip().replace("R$", "").strip()
    if not texto:
        return None
    texto = texto.replace(".", "").replace(",", ".") if "," in texto else texto
    try:
        return float(texto)
    except ValueError:
        return None


def normalizar_municipio(nome: str) -> str:
    """'  são josé dos campos ' -> 'SAO JOSE DOS CAMPOS' (busca previsivel)."""
    return sem_acento(nome).upper().strip()


# --------------------------------------------------------------------------- #
# Leitura do arquivo da ANP
# --------------------------------------------------------------------------- #

# nome logico -> possiveis nomes no arquivo da ANP (comparados sem acento/caixa)
COLUNAS = {
    "regiao":         ["regiao sigla", "regiao"],
    "uf":             ["estado sigla", "uf", "estado"],
    "municipio":      ["municipio"],
    "revenda":        ["revenda"],
    "cnpj_revenda":   ["cnpj da revenda", "cnpj revenda"],
    "produto":        ["produto"],
    "data_coleta":    ["data da coleta", "data coleta"],
    "valor_venda":    ["valor de venda", "valor venda"],
    "unidade_medida": ["unidade de medida", "unidade medida"],
    "bandeira":       ["bandeira"],
}


def mapear_colunas(df: pd.DataFrame) -> dict[str, str]:
    """Descobre qual coluna do arquivo corresponde a cada campo logico."""
    disponiveis = {chave_coluna(c): c for c in df.columns}
    mapa: dict[str, str] = {}
    for logico, candidatos in COLUNAS.items():
        for candidato in candidatos:
            if candidato in disponiveis:
                mapa[logico] = disponiveis[candidato]
                break
    faltando = [c for c in ("uf", "municipio", "produto", "data_coleta", "valor_venda")
                if c not in mapa]
    if faltando:
        raise SystemExit(
            f"Colunas obrigatorias nao encontradas no arquivo: {faltando}\n"
            f"Colunas presentes: {list(df.columns)}"
        )
    return mapa


def localizar_cabecalho(previa: pd.DataFrame) -> int:
    """Descobre em qual linha estao os nomes das colunas.

    As planilhas da ANP costumam ter linhas de titulo antes da tabela de fato.
    Procuramos a primeira linha que contenha 'municipio' e 'produto'.
    """
    procuradas = {"municipio", "produto"}
    for indice in range(len(previa)):
        celulas = {chave_coluna(str(c)) for c in previa.iloc[indice].tolist()}
        if procuradas.issubset(celulas):
            return indice
    return 0


def _ler_excel(fonte) -> pd.DataFrame:
    previa = pd.read_excel(fonte, header=None, nrows=20, dtype=str)
    linha = localizar_cabecalho(previa)
    if linha:
        print(f"cabecalho encontrado na linha {linha + 1} da planilha")
    if hasattr(fonte, "seek"):
        fonte.seek(0)
    return pd.read_excel(fonte, header=linha, dtype=str)


def _ler_csv(fonte) -> pd.DataFrame:
    # latin-1 nunca falha na decodificacao; se o arquivo for utf-8 o pandas
    # tambem le, entao tentamos utf-8 primeiro e caimos para latin-1.
    for encoding in ("utf-8", "latin-1"):
        try:
            if hasattr(fonte, "seek"):
                fonte.seek(0)
            return pd.read_csv(fonte, sep=";", encoding=encoding, dtype=str)
        except UnicodeDecodeError:
            continue
    raise SystemExit("nao foi possivel decodificar o arquivo")


def ler_arquivo(caminho: str | None, url: str | None) -> pd.DataFrame:
    if url:
        print(f"baixando {url}")
        resposta = requests.get(url, timeout=120)
        resposta.raise_for_status()
        fonte = io.BytesIO(resposta.content)
        extensao = os.path.splitext(urlparse(url).path)[1].lower()
    else:
        if not os.path.isfile(caminho):
            disponiveis = []
            pasta = os.path.dirname(caminho) or "."
            if os.path.isdir(pasta):
                disponiveis = sorted(
                    a for a in os.listdir(pasta)
                    if a.lower().endswith((".csv", ".xlsx", ".xls"))
                )
            raise SystemExit(
                f"Arquivo nao encontrado: {caminho}\n"
                + (f"Disponiveis em {pasta}/: {', '.join(disponiveis)}"
                   if disponiveis
                   else f"Nenhuma planilha em {pasta}/. Baixe um arquivo da ANP primeiro.")
            )
        fonte = caminho
        extensao = os.path.splitext(caminho)[1].lower()

    if extensao in (".xlsx", ".xls"):
        return _ler_excel(fonte)
    return _ler_csv(fonte)


def preparar(df: pd.DataFrame) -> pd.DataFrame:
    mapa = mapear_colunas(df)
    saida = pd.DataFrame()

    for logico, coluna in mapa.items():
        saida[logico] = df[coluna]

    for opcional in ("regiao", "revenda", "cnpj_revenda", "bandeira", "unidade_medida"):
        if opcional not in saida.columns:
            saida[opcional] = None

    saida["valor_venda"] = saida["valor_venda"].map(para_decimal)
    saida["municipio"] = saida["municipio"].map(normalizar_municipio)
    saida["produto"] = saida["produto"].map(lambda p: sem_acento(p).upper().strip())
    saida["uf"] = saida["uf"].str.strip().str.upper()
    saida["data_coleta"] = pd.to_datetime(
        saida["data_coleta"], dayfirst=True, errors="coerce"
    ).dt.date

    antes = len(saida)
    saida = saida.dropna(subset=["valor_venda", "data_coleta", "municipio", "produto"])
    print(f"{antes - len(saida)} linhas descartadas por dado invalido")
    return saida


# --------------------------------------------------------------------------- #
# Gravacao
# --------------------------------------------------------------------------- #

INSERT = """
INSERT INTO preco_coleta
    (data_coleta, regiao, uf, municipio, produto, revenda,
     cnpj_revenda, bandeira, valor_venda, unidade_medida)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
ON CONFLICT DO NOTHING
"""

AGREGA = """
INSERT INTO resumo_semanal
    (semana_inicio, semana_fim, uf, municipio, produto,
     preco_medio, preco_minimo, preco_maximo, postos_pesquisados)
SELECT
    (date_trunc('week', data_coleta))::date,
    (date_trunc('week', data_coleta) + INTERVAL '6 days')::date,
    uf, municipio, produto,
    ROUND(AVG(valor_venda), 3), MIN(valor_venda), MAX(valor_venda), COUNT(*)
FROM preco_coleta
WHERE data_coleta BETWEEN %s AND %s
GROUP BY date_trunc('week', data_coleta), uf, municipio, produto
ON CONFLICT (semana_inicio, uf, municipio, produto) DO UPDATE SET
    semana_fim         = EXCLUDED.semana_fim,
    preco_medio        = EXCLUDED.preco_medio,
    preco_minimo       = EXCLUDED.preco_minimo,
    preco_maximo       = EXCLUDED.preco_maximo,
    postos_pesquisados = EXCLUDED.postos_pesquisados,
    atualizado_em      = NOW()
"""


def gravar(df: pd.DataFrame, dsn: str) -> None:
    registros = [
        (r.data_coleta, r.regiao, r.uf, r.municipio, r.produto, r.revenda,
         r.cnpj_revenda, r.bandeira, r.valor_venda, r.unidade_medida)
        for r in df.itertuples(index=False)
    ]
    inicio: date = df["data_coleta"].min()
    fim: date = df["data_coleta"].max()

    with psycopg.connect(dsn) as conexao:
        with conexao.cursor() as cursor:
            cursor.executemany(INSERT, registros)
            print(f"{len(registros)} linhas enviadas para preco_coleta")
            cursor.execute(AGREGA, (inicio, fim))
            print(f"resumo semanal recalculado de {inicio} a {fim}")
        conexao.commit()


def main() -> None:
    parser = argparse.ArgumentParser(description="Carrega precos da ANP no Postgres")
    grupo = parser.add_mutually_exclusive_group(required=True)
    grupo.add_argument("--arquivo", help="caminho do CSV baixado da ANP")
    grupo.add_argument("--url", help="URL do arquivo da ANP")
    parser.add_argument(
        "--dsn",
        default=os.getenv("DATABASE_URL", "postgresql://precos:precos@localhost:5432/precos"),
        help="string de conexao do Postgres (ou variavel DATABASE_URL)",
    )
    args = parser.parse_args()

    bruto = ler_arquivo(args.arquivo, args.url)
    print(f"{len(bruto)} linhas lidas")
    limpo = preparar(bruto)
    gravar(limpo, args.dsn)
    print("pronto")


if __name__ == "__main__":
    sys.exit(main())
