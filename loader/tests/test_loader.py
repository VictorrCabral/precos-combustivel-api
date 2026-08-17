"""Testes das funcoes de normalizacao — rode com: pytest"""

import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from loader import (  # noqa: E402
    chave_coluna,
    mapear_colunas,
    normalizar_municipio,
    para_decimal,
    sem_acento,
)


@pytest.mark.parametrize(
    "entrada,esperado",
    [
        ("6,49", 6.49),
        ("6.49", 6.49),
        ("1.234,56", 1234.56),   # milhar com ponto, decimal com virgula
        ("R$ 6,09", 6.09),
        ("  7,00  ", 7.00),
        ("", None),
        (None, None),
        ("abc", None),
    ],
)
def test_para_decimal(entrada, esperado):
    assert para_decimal(entrada) == esperado


def test_sem_acento():
    assert sem_acento("Município") == "Municipio"
    assert sem_acento("SÃO PAULO") == "SAO PAULO"


def test_normalizar_municipio():
    assert normalizar_municipio("  são josé dos campos ") == "SAO JOSE DOS CAMPOS"


def test_chave_coluna():
    assert chave_coluna("Regiao - Sigla") == "regiao sigla"
    assert chave_coluna("REGIAO_SIGLA") == "regiao sigla"
    assert chave_coluna("Data_da_Coleta") == "data da coleta"
    assert chave_coluna("  Valor de Venda  ") == "valor de venda"


def test_mapear_colunas_aceita_variacoes():
    df = pd.DataFrame(
        columns=[
            "Regiao - Sigla", "Estado - Sigla", "Municipio", "Revenda",
            "CNPJ da Revenda", "Produto", "Data da Coleta",
            "Valor de Venda", "Unidade de Medida", "Bandeira",
        ]
    )
    mapa = mapear_colunas(df)
    assert mapa["uf"] == "Estado - Sigla"
    assert mapa["valor_venda"] == "Valor de Venda"
    assert mapa["data_coleta"] == "Data da Coleta"


def test_mapear_colunas_falha_sem_obrigatorias():
    df = pd.DataFrame(columns=["Produto", "Bandeira"])
    with pytest.raises(SystemExit):
        mapear_colunas(df)


def test_localizar_cabecalho_pula_linhas_de_titulo():
    """Planilha da ANP costuma ter titulo antes da tabela."""
    previa = pd.DataFrame([
        ["LEVANTAMENTO DE PRECOS DE COMBUSTIVEIS", None, None, None],
        ["Periodo: 09/08/2026 a 15/08/2026", None, None, None],
        [None, None, None, None],
        ["Município", "Produto", "Data da Coleta", "Valor de Venda"],
        ["MANAUS", "GASOLINA", "10/08/2026", "6,49"],
    ])
    from loader import localizar_cabecalho
    assert localizar_cabecalho(previa) == 3


def test_localizar_cabecalho_quando_ja_e_a_primeira_linha():
    previa = pd.DataFrame([
        ["Municipio", "Produto", "Valor de Venda"],
        ["MANAUS", "GASOLINA", "6,49"],
    ])
    from loader import localizar_cabecalho
    assert localizar_cabecalho(previa) == 0
