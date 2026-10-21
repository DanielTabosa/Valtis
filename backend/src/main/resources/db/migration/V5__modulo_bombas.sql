-- Módulo de bombas, MVP 1 (RF-60 a RF-68, D-26, D-27). Ver docs/database.md §8.
-- Aprovado pelo Daniel em 20/10/2026. Nenhuma tabela existente é alterada.
--
-- A bomba não aponta para o conjunto: quem diz onde ela está é ocupacao_bomba,
-- com período (D-27). Enums como VARCHAR + CHECK, como nas demais migrations.

CREATE TABLE tipo_bomba (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome      TEXT        NOT NULL,
    ativo     BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX ux_tipo_bomba_nome ON tipo_bomba (lower(nome));

-- Catálogo inicial (RF-60). Não é dado fictício: é configuração do negócio.
INSERT INTO tipo_bomba (nome) VALUES
    ('Recalque'), ('Incêndio'), ('Drenagem'), ('Pressurização'),
    ('Pluvial'), ('Piscina'), ('Jacuzzi'), ('Esgoto');

CREATE TABLE conjunto_bombeamento (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id    UUID        NOT NULL REFERENCES cliente (id),
    tipo_bomba_id UUID        NOT NULL REFERENCES tipo_bomba (id),
    localizacao   TEXT        NOT NULL,
    observacoes   TEXT,
    ativo         BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_conjunto_cliente ON conjunto_bombeamento (cliente_id);

CREATE TABLE bomba (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_referencia   TEXT        NOT NULL UNIQUE,
    patrimonio_manutec  BOOLEAN     NOT NULL DEFAULT FALSE,
    cliente_id          UUID        REFERENCES cliente (id),
    fabricante          TEXT,
    modelo              TEXT,
    potencia_cv         NUMERIC(6, 2),
    tensao_volts        INTEGER,
    corrente_amperes    NUMERIC(6, 2),
    funcao              TEXT,
    data_instalacao     DATE,
    periodicidade_meses INTEGER     NOT NULL,
    ultima_manutencao   DATE,
    ativo               BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_bomba_periodicidade CHECK (periodicidade_meses > 0),
    -- Bomba patrimonial não tem dono; bomba de condomínio sempre tem.
    CONSTRAINT ck_bomba_dono CHECK (patrimonio_manutec = (cliente_id IS NULL))
);
CREATE INDEX idx_bomba_cliente ON bomba (cliente_id);

CREATE TABLE ocupacao_bomba (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    bomba_id             UUID        NOT NULL REFERENCES bomba (id),
    conjunto_id          UUID        NOT NULL REFERENCES conjunto_bombeamento (id),
    numero_posicao       TEXT        NOT NULL,
    natureza             VARCHAR(20) NOT NULL CHECK (natureza IN ('instalacao', 'emprestimo')),
    bomba_substituida_id UUID        REFERENCES bomba (id),
    inicio               DATE        NOT NULL,
    fim                  DATE,
    motivo_encerramento  VARCHAR(20) CHECK (motivo_encerramento IN ('retirada', 'devolucao')),
    observacoes          TEXT,
    registrado_por_id    UUID        NOT NULL REFERENCES usuario (id),
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ocupacao_periodo      CHECK (fim IS NULL OR fim >= inicio),
    CONSTRAINT ck_ocupacao_encerramento CHECK ((fim IS NULL) = (motivo_encerramento IS NULL))
);
-- Sem unicidade de ocupação aberta por posição: RN-19 permite duas ao mesmo tempo.
CREATE INDEX idx_ocupacao_bomba_aberta   ON ocupacao_bomba (bomba_id) WHERE fim IS NULL;
CREATE INDEX idx_ocupacao_posicao_aberta ON ocupacao_bomba (conjunto_id, numero_posicao) WHERE fim IS NULL;
