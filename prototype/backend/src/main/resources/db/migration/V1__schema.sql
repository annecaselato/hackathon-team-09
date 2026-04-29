-- SIFAP 2.0 Database Schema
-- Flyway migration V1 — initial schema
-- Traces to: BENEFICIARIO (FNR 151), PAGAMENTO (FNR 152), AUDITORIA (FNR 153), PROGRAMA-SOCIAL (FNR 154)

-- ============================================================
-- BENEFICIARIOS table (maps BENEFICIARIO DDM FNR 151)
-- ============================================================
CREATE TABLE IF NOT EXISTS beneficiarios (
    id                BIGSERIAL PRIMARY KEY,
    cpf               VARCHAR(11)  NOT NULL UNIQUE,
    nome              VARCHAR(100) NOT NULL,
    dt_nascimento     DATE         NOT NULL,
    uf                CHAR(2)      NOT NULL,
    status            CHAR(1)      NOT NULL DEFAULT 'A'
                          CHECK (status IN ('A','S','C','I','D')),
    cod_programa      VARCHAR(3),
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMP,
    usuario_criacao   VARCHAR(8)
);

CREATE INDEX IF NOT EXISTS idx_beneficiarios_cpf    ON beneficiarios (cpf);
CREATE INDEX IF NOT EXISTS idx_beneficiarios_status ON beneficiarios (status);

-- ============================================================
-- PAGAMENTOS table (maps PAGAMENTO DDM FNR 152)
-- Partitioned by ano_mes_ref for 180M+ record performance (REQ-DATA-001)
-- ============================================================
CREATE TABLE IF NOT EXISTS pagamentos (
    id                BIGSERIAL,
    beneficiario_id   BIGINT       NOT NULL REFERENCES beneficiarios(id),
    cpf_benef         VARCHAR(11)  NOT NULL,
    ano_mes_ref       VARCHAR(6)   NOT NULL,  -- AAAAMM format
    cod_programa      VARCHAR(3),
    vlr_bruto         NUMERIC(13,2) NOT NULL,
    vlr_desconto_total NUMERIC(13,2) NOT NULL DEFAULT 0,
    vlr_liquido       NUMERIC(13,2),
    status            CHAR(1)      NOT NULL DEFAULT 'P'
                          CHECK (status IN ('P','G','E','C','D','X','R')),
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMP,
    usuario_geracao   VARCHAR(8),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_pagamentos_cpf_comp ON pagamentos (cpf_benef, ano_mes_ref, cod_programa);
CREATE INDEX IF NOT EXISTS idx_pagamentos_status   ON pagamentos (status);
CREATE INDEX IF NOT EXISTS idx_pagamentos_competencia ON pagamentos (ano_mes_ref);

-- ============================================================
-- DESCONTOS table (maps GRP-DESCONTO periodic group in PAGAMENTO DDM)
-- Max 8 rows per payment (Adabas PE max occurrences)
-- ============================================================
CREATE TABLE IF NOT EXISTS descontos (
    id            BIGSERIAL PRIMARY KEY,
    pagamento_id  BIGINT       NOT NULL REFERENCES pagamentos(id) ON DELETE CASCADE,
    tp_desconto   CHAR(1)      NOT NULL CHECK (tp_desconto IN ('J','C','I','O')),
    vlr_desconto  NUMERIC(9,2) NOT NULL,
    descricao     VARCHAR(60)
);

CREATE INDEX IF NOT EXISTS idx_descontos_pagamento ON descontos (pagamento_id);

-- ============================================================
-- AUDITORIA table (maps AUDITORIA DDM FNR 153)
-- IMMUTABLE: app user cannot UPDATE or DELETE records (REQ-AUD-001)
-- sistema_origem: 'L'=legacy, 'M'=modern SIFAP 2.0 (ADR-007)
-- ============================================================
CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGSERIAL    PRIMARY KEY,
    dt_evento       DATE         NOT NULL,
    hr_evento       TIME         NOT NULL,
    ts_evento       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cod_acao        VARCHAR(2)   NOT NULL
                        CHECK (cod_acao IN ('IN','AL','EX','CO','DV','LG','LO','BT','ER','AU','RE')),
    cod_modulo      VARCHAR(50)  NOT NULL,
    tipo_entidade   VARCHAR(4)   NOT NULL
                        CHECK (tipo_entidade IN ('BENF','PGTO','PROG','ADMN','SIST')),
    id_entidade     VARCHAR(15)  NOT NULL,
    campos_antes    JSONB,
    campos_depois   JSONB,
    usr_evento      VARCHAR(8)   NOT NULL,
    sistema_origem  CHAR(1)      NOT NULL DEFAULT 'M'
                        CHECK (sistema_origem IN ('L','M'))
);

CREATE INDEX IF NOT EXISTS idx_auditoria_entidade   ON auditoria (tipo_entidade, id_entidade);
CREATE INDEX IF NOT EXISTS idx_auditoria_dt_evento  ON auditoria (dt_evento);
CREATE INDEX IF NOT EXISTS idx_auditoria_cod_acao   ON auditoria (cod_acao);

-- Immutability: prevent DELETE and UPDATE via rule (BR-AUD-001)
CREATE RULE auditoria_no_delete AS ON DELETE TO auditoria DO INSTEAD NOTHING;
CREATE RULE auditoria_no_update AS ON UPDATE TO auditoria DO INSTEAD NOTHING;

-- ============================================================
-- PROGRAMAS_SOCIAIS table (maps PROGRAMA-SOCIAL DDM FNR 154)
-- ============================================================
CREATE TABLE IF NOT EXISTS programas_sociais (
    id              BIGSERIAL    PRIMARY KEY,
    cod_programa    VARCHAR(3)   NOT NULL UNIQUE,
    nome_programa   VARCHAR(60)  NOT NULL,
    fator_regional  NUMERIC(4,3) NOT NULL DEFAULT 1.000,
    reajuste        NUMERIC(5,4) NOT NULL DEFAULT 0.0000,
    ativo           BOOLEAN      NOT NULL DEFAULT true,
    criado_em       TIMESTAMP    NOT NULL DEFAULT now()
);

-- Seed base programs
INSERT INTO programas_sociais (cod_programa, nome_programa, fator_regional, reajuste, ativo)
VALUES
    ('A', 'Programa Social Tipo A', 1.100, 0.0500, true),
    ('B', 'Programa Social Tipo B', 1.000, 0.0300, true),
    ('C', 'Programa Social Tipo C', 0.900, 0.0200, true)
ON CONFLICT (cod_programa) DO NOTHING;
