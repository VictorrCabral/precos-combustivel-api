# API de Preços de Combustíveis

API pública e gratuita com o preço médio dos combustíveis por município brasileiro,
construída sobre o levantamento semanal da ANP.

> **Status:** funcionando localmente. Deploy público em andamento — veja [DEPLOY.md](DEPLOY.md).
>
> Quando publicado em plano gratuito, a primeira chamada após um período de
> inatividade pode levar até 1 minuto para responder.

## O problema

A ANP publica toda semana uma pesquisa de preços posto a posto, em todo o país.
O dado é público, oficial e de boa qualidade — mas sai como **arquivo CSV pesado**,
separado por ponto e vírgula, em latin-1 e com vírgula decimal, e a série histórica
fica em outro lugar.

Quem quer só responder *"quanto está a gasolina em Manaus esta semana?"* precisa
baixar planilha, tratar encoding, normalizar nome de município e cruzar arquivos.

Esta API faz esse trabalho uma vez por semana e devolve o resultado pronto.

## Como usar

```bash
curl "https://SEU-DOMINIO/v1/precos?uf=AM&municipio=Manaus&produto=gasolina"
```

```json
{
  "uf": "AM",
  "municipio": "MANAUS",
  "produto": "GASOLINA",
  "semanaInicio": "2026-08-09",
  "semanaFim": "2026-08-15",
  "precoMedio": 6.421,
  "precoMinimo": 6.090,
  "precoMaximo": 6.890,
  "postosPesquisados": 37
}
```

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/v1/precos` | Preço mais recente por UF, município e produto |
| `GET` | `/v1/precos/historico` | Série semanal, para montar gráfico (`de` e `ate` em ISO) |
| `GET` | `/v1/municipios?uf=AM` | Municípios com dado disponível |
| `GET` | `/v1/produtos` | Produtos disponíveis |

### No terminal

Ninguém quer abrir navegador para ver um número:

```bash
curl https://SEU-DOMINIO/AM/Manaus
curl https://SEU-DOMINIO/AM/Manaus/etanol
```

```
  Gasolina em Manaus (AM)
  semana de 29/06 a 05/07

  Média    R$ 6,99
  Mínimo   R$ 6,89
  Máximo   R$ 7,09
  Postos   39

  ▁▁▂▃▄▅▅▄▂▂▁▁▂▃▅▆█▇▆▅▅▄▃▂▂▂
  05/01 a 05/07   R$ 5,88 a R$ 6,55

  fonte: ANP  ·  json: /v1/precos?uf=AM&municipio=MANAUS
```

Documentação interativa em `/docs` (Swagger UI).

Acentos e caixa não importam: `São Paulo`, `sao paulo` e `SAO PAULO` chegam
no mesmo município.

## Arquitetura

```
CSV semanal da ANP
        │
        ▼
  loader (Python)          normaliza encoding, decimal e município
        │                  grava o bruto e recalcula o agregado
        ▼
   PostgreSQL              preco_coleta (bruto)  +  resumo_semanal (agregado)
        │
        ▼
  API (Spring Boot)        lê só o agregado, com cache em memória
```

Duas decisões que valem explicação:

**A API não lê o dado bruto.** O carregador já deixa o agregado semanal pronto
em `resumo_semanal`. Consulta de API não deve varrer milhões de linhas para
calcular média a cada requisição.

**O Hibernate não cria nem altera tabela** (`ddl-auto: none`). O esquema vive em
`db/init.sql`, versionado no repositório. Deixar o ORM alterar banco de produção
é como deixar a porta destrancada.

## Rodando localmente

Pré-requisitos: Docker, Java 21 e Python 3.12.

```bash
# 1. banco
docker compose up -d

# 2. baixe um arquivo semanal em
#    https://www.gov.br/anp/pt-br/assuntos/precos-e-defesa-da-concorrencia/precos/
#    levantamento-de-precos-de-combustiveis-ultimas-semanas-pesquisadas
pip install -r loader/requirements.txt
python loader/loader.py --arquivo dados/ca-2026-08.csv

# 3. API
cd api && ./mvnw spring-boot:run
```

Abra http://localhost:8080/docs

### Testes do carregador

```bash
python -m pytest loader/tests -q
```

## Carga automática

O workflow `.github/workflows/carga-semanal.yml` roda toda quarta-feira, executa
os testes e carrega os dados novos. Precisa de dois valores configurados no
repositório: o *secret* `DATABASE_URL` e a *variable* `ANP_CSV_URL`.

## Roadmap

- [x] Esquema do banco e carregador com testes
- [x] Endpoints de preço atual, histórico, municípios e produtos
- [ ] Deploy público com banco gerenciado (guia pronto em DEPLOY.md)
- [x] Saída em texto puro para terminal, com mini gráfico
- [ ] Página com gráfico de evolução do preço
- [ ] Limite de requisições por IP
- [ ] Testes de integração da API

## Fonte dos dados

[Levantamento de Preços de Combustíveis — ANP](https://www.gov.br/anp/pt-br/assuntos/precos-e-defesa-da-concorrencia/precos/levantamento-de-precos-de-combustiveis-ultimas-semanas-pesquisadas).
Dados públicos. Este projeto não tem vínculo com a ANP.

## Licença

MIT
