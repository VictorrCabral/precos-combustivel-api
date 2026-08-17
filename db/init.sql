-- Esquema do banco. Executado automaticamente pelo Postgres do docker-compose
-- na primeira subida (tudo em /docker-entrypoint-initdb.d roda uma vez).

-- Dado bruto: uma linha por posto pesquisado pela ANP, por semana.
CREATE TABLE IF NOT EXISTS preco_coleta (
    id              BIGSERIAL PRIMARY KEY,
    data_coleta     DATE         NOT NULL,
    regiao          VARCHAR(20),
    uf              CHAR(2)      NOT NULL,
    municipio       VARCHAR(120) NOT NULL,
    produto         VARCHAR(60)  NOT NULL,
    revenda         VARCHAR(200),
    cnpj_revenda    VARCHAR(20),
    bandeira        VARCHAR(80),
    valor_venda     NUMERIC(8,3) NOT NULL,
    unidade_medida  VARCHAR(20),
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Evita inserir duas vezes a mesma coleta ao reprocessar o mesmo arquivo.
CREATE UNIQUE INDEX IF NOT EXISTS ux_preco_coleta_dedup
    ON preco_coleta (data_coleta, cnpj_revenda, produto, valor_venda);

CREATE INDEX IF NOT EXISTS ix_preco_coleta_busca
    ON preco_coleta (uf, municipio, produto, data_coleta DESC);

-- Agregado semanal: é isso que a API lê. Consulta rápida, sem varrer o bruto.
CREATE TABLE IF NOT EXISTS resumo_semanal (
    id                 BIGSERIAL PRIMARY KEY,
    semana_inicio      DATE         NOT NULL,
    semana_fim         DATE         NOT NULL,
    uf                 CHAR(2)      NOT NULL,
    municipio          VARCHAR(120) NOT NULL,
    produto            VARCHAR(60)  NOT NULL,
    preco_medio        NUMERIC(8,3) NOT NULL,
    preco_minimo       NUMERIC(8,3) NOT NULL,
    preco_maximo       NUMERIC(8,3) NOT NULL,
    postos_pesquisados INTEGER      NOT NULL,
    atualizado_em      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_resumo UNIQUE (semana_inicio, uf, municipio, produto)
);

CREATE INDEX IF NOT EXISTS ix_resumo_busca
    ON resumo_semanal (uf, municipio, produto, semana_inicio DESC);
