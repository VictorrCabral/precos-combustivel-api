# Deploy

Guia para colocar a API no ar de graça: **Neon** para o banco, **Render** para a
aplicação.

## Por que essas duas

| | Neon (banco) | Render (aplicação) |
|---|---|---|
| Custo | grátis, sem prazo | grátis, sem prazo |
| Limites | 0,5 GB, 100 h de computação/mês | hiberna após 15 min sem uso |
| Hibernação | suspende em 5 min, acorda em milissegundos | acorda em 30 a 60 segundos |

O ponto fraco é um só: **a primeira chamada depois de um período parado demora
até um minuto**. Avise isso no README para ninguém achar que está quebrado.

O Postgres gratuito do próprio Render foi descartado porque expira em 30 dias — o
projeto morreria sozinho no mês seguinte. O do Neon é permanente.

Quando quiser eliminar a espera, migre só a aplicação para o Railway (cerca de
US$ 5/mês) mantendo o banco no Neon. É trocar as mesmas variáveis de lugar.

## 1. Banco no Neon

Crie uma conta em [neon.com](https://neon.com), crie um projeto e copie a
**connection string**. Ela tem este formato:

```
postgresql://usuario:senha@ep-algo-123456.sa-east-1.aws.neon.tech/neondb?sslmode=require
```

O `sslmode=require` no fim não é enfeite: o Neon **só aceita conexão cifrada**.
Sem ele, a conexão é recusada.

### Criar o esquema

```bash
psql "postgresql://usuario:senha@ep-algo.sa-east-1.aws.neon.tech/neondb?sslmode=require" -f db/init.sql
```

### Carregar os dados

```bash
source .venv/bin/activate
python loader/loader.py \
  --arquivo "dados/Preços semestrais - AUTOMOTIVOS_2026.01.csv" \
  --dsn "postgresql://usuario:senha@ep-algo.sa-east-1.aws.neon.tech/neondb?sslmode=require"
```

Confira:

```bash
psql "SUA_URL_DO_NEON" -c "SELECT COUNT(*) FROM resumo_semanal;"
```

## 2. API no Render

Em [render.com](https://render.com): **New → Web Service** e conecte o
repositório `precos-combustivel-api`.

Configurações:

- **Root Directory:** `api`
- **Runtime:** Docker (ele acha o `api/Dockerfile` sozinho)
- **Instance Type:** Free
- **Health Check Path:** `/actuator/health`

Em **Environment**, três variáveis. Cuidado com a primeira: o Spring precisa da
URL no formato **JDBC**, que é diferente do formato que o Neon te deu. Monte
assim, mantendo o `sslmode=require` e **sem** usuário e senha embutidos:

```
DATABASE_URL = jdbc:postgresql://ep-algo.sa-east-1.aws.neon.tech/neondb?sslmode=require
DB_USER      = usuario
DB_PASSWORD  = senha
```

Colar a URL do Neon direto na `DATABASE_URL` é o erro mais comum deste deploy: a
aplicação sobe e cai em seguida.

## 3. Conferir

```bash
curl https://SEU-APP.onrender.com/AM/Manaus
curl "https://SEU-APP.onrender.com/v1/precos?uf=AM&municipio=Manaus&produto=gasolina"
```

A primeira chamada pode demorar até um minuto se o serviço estiver dormindo. A
documentação fica em `https://SEU-APP.onrender.com/docs`.

## 4. Carga semanal automática

Em **Settings → Secrets and variables → Actions** do repositório:

- *secret* `DATABASE_URL` — a URL do Neon no formato `postgresql://` (aqui é o
  formato do Python, **não** o JDBC)
- *variable* `ANP_CSV_URL` — o endereço do arquivo semanal da ANP

## 5. Depois do deploy

- Link no topo do README e na descrição do repositório
- Em `web/index.html`, troque a constante `API` para o domínio do Render
- No README, uma linha honesta: *"Hospedado em plano gratuito; a primeira chamada
  após um período de inatividade pode levar até 1 minuto."*
- Um GIF de 10 segundos do `curl` respondendo, no topo do README

## Problemas comuns

**Sobe e cai logo depois.** `DATABASE_URL` sem o prefixo `jdbc:`, ou sem o
`sslmode=require`. Veja o log de deploy.

**`FATAL: password authentication failed`.** Usuário e senha ficaram embutidos na
URL JDBC além de estarem em `DB_USER` e `DB_PASSWORD`. Deixe só nas variáveis
separadas.

**`relation "resumo_semanal" does not exist`.** O `init.sql` não foi aplicado no
Neon — volte ao passo 1.

**Responde, mas sem dados.** O esquema existe e o carregador não rodou contra o
Neon. Passo 1, segundo comando.

**Primeira chamada muito lenta.** Esperado no plano gratuito do Render. Se
incomodar, migre a aplicação para o Railway mantendo o banco onde está.
