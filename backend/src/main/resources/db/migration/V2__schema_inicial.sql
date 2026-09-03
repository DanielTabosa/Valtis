-- Schema inicial do domínio. Ver docs/database.md.
-- Entidades desta migration: usuario, cliente, especificacao, estacao, valvula, servico.
-- Ficam para depois: anexo, oportunidade, auditoria.

-- Convenção: enums como VARCHAR + CHECK, não como tipo ENUM nativo do Postgres.
-- Enum nativo é doloroso de alterar e de mapear no Hibernate; CHECK dá a mesma
-- garantia de integridade e evolui com um ALTER simples.

CREATE TABLE usuario (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        TEXT        NOT NULL,
    email       TEXT        NOT NULL UNIQUE,
    senha_hash  TEXT        NOT NULL,
    perfil      VARCHAR(20) NOT NULL CHECK (perfil IN ('tecnico', 'admin')),
    ativo       BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cliente (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_referencia        TEXT        NOT NULL UNIQUE,
    nome_condominio          TEXT        NOT NULL,
    endereco                 TEXT,
    bairro                   TEXT,
    cidade                   TEXT,
    sindico_responsavel      TEXT,
    telefone_contato         TEXT,
    email_contato            TEXT,
    tipo_atendimento         VARCHAR(20) NOT NULL DEFAULT 'Avulso'
                                 CHECK (tipo_atendimento IN ('Contrato', 'Avulso')),
    status_contrato          VARCHAR(20) NOT NULL DEFAULT 'Ativo'
                                 CHECK (status_contrato IN ('Ativo', 'Suspenso', 'Inativo')),
    ativo                    BOOLEAN     NOT NULL DEFAULT TRUE,
    origem                   VARCHAR(20) NOT NULL DEFAULT 'sistema'
                                 CHECK (origem IN ('sistema', 'importacao')),
    ultima_manutencao_legado DATE,
    data_legado_aproximada   BOOLEAN     NOT NULL DEFAULT FALSE,
    cadastro_incompleto      BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE especificacao (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marca              TEXT        NOT NULL,
    modelo             TEXT        NOT NULL,
    diametro_polegadas TEXT        NOT NULL,  -- texto: aceita 1.1/2, 3/4
    tipo_registro      VARCHAR(20) NOT NULL CHECK (tipo_registro IN ('Esfera', 'Gaveta')),
    descricao_completa TEXT        NOT NULL,
    ativo              BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_especificacao UNIQUE (marca, modelo, diametro_polegadas, tipo_registro)
);

CREATE TABLE estacao (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id              UUID        NOT NULL REFERENCES cliente (id),
    localizacao_instalacao  TEXT        NOT NULL,
    andar_inicial           INTEGER     NOT NULL,  -- térreo = 0, subsolo negativo
    andar_final             INTEGER     NOT NULL,
    requer_fechamento_geral BOOLEAN     NOT NULL DEFAULT FALSE,
    ativo                   BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_estacao_andares CHECK (andar_final >= andar_inicial)
);

CREATE TABLE valvula (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estacao_id          UUID        NOT NULL REFERENCES estacao (id),
    especificacao_id    UUID        NOT NULL REFERENCES especificacao (id),
    codigo_referencia   TEXT        NOT NULL UNIQUE,
    numero_valvula      TEXT        NOT NULL,  -- 01, 02, 03... sem limite (D-19)
    numero_serie        TEXT,
    data_instalacao     DATE,
    periodicidade_meses INTEGER     NOT NULL DEFAULT 12,
    ativo               BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_valvula_na_estacao UNIQUE (estacao_id, numero_valvula),
    CONSTRAINT ck_periodicidade CHECK (periodicidade_meses > 0)
);

CREATE TABLE servico (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    valvula_id             UUID        NOT NULL REFERENCES valvula (id),
    data_realizada         DATE        NOT NULL,
    tipo_manutencao        VARCHAR(20) NOT NULL
                               CHECK (tipo_manutencao IN ('Preventiva', 'Corretiva')),
    tecnico_responsavel_id UUID        NOT NULL REFERENCES usuario (id),
    pecas_substituidas     TEXT,
    pressao_entrada        NUMERIC(6, 2),
    pressao_saida          NUMERIC(6, 2),
    observacoes            TEXT,
    status                 VARCHAR(20) NOT NULL DEFAULT 'concluido'
                               CHECK (status IN ('rascunho', 'concluido')),
    criado_por_id          UUID        NOT NULL REFERENCES usuario (id),
    criado_em              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices: só onde há consulta real (docs/database.md §7).
CREATE INDEX idx_estacao_cliente   ON estacao (cliente_id);
CREATE INDEX idx_valvula_estacao   ON valvula (estacao_id);
CREATE INDEX idx_servico_valvula   ON servico (valvula_id, data_realizada DESC);
CREATE INDEX idx_cliente_nome      ON cliente (nome_condominio);
