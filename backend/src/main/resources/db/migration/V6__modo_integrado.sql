-- Modo integrado (D-29, D-30, D-31; RF-70 a RF-80; RN-21 a RN-26).
-- Proposta em docs/integracao/schema-v6.md, aprovada pelo Daniel em 01/11/2026.
--
-- ADITIVA E NÃO DESTRUTIVA: nenhuma linha é apagada e nenhum valor existente muda.
-- As tabelas são as mesmas nos dois modos; só muda quem escreve nelas (D-29). Por
-- isso aqui só entram: a referência externa nas tabelas que já existem, os campos
-- que o Jiraya não tem passando a opcionais (cada um com CHECK que mantém a
-- exigência no cadastro manual) e as duas tabelas de entrada (Outbox invertido, D-30).
--
-- As constraints sem nome do V2 e do V4 têm o nome que o Postgres dá a CHECK de
-- coluna: <tabela>_<coluna>_check. DROP sem IF EXISTS de propósito: se o nome
-- estiver errado, a migração falha em vez de deixar a regra antiga de pé.

-- ------------------------------------------------- cliente (RF-71)

ALTER TABLE cliente
    ADD COLUMN manutec_id          BIGINT,
    ADD COLUMN fonte_atualizado_em TEXT,
    ADD COLUMN sincronizado_em     TIMESTAMPTZ;

CREATE UNIQUE INDEX ux_cliente_manutec_id ON cliente (manutec_id) WHERE manutec_id IS NOT NULL;

ALTER TABLE cliente DROP CONSTRAINT cliente_origem_check;
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_origem
    CHECK (origem IN ('sistema', 'importacao', 'integracao'));

-- ------------------------------------------------- estacao (RF-73, Q-2)

-- A estação derivada do Jiraya nasce sem andares e fica "a completar" no Valtis.
-- Os dois juntos: ou se sabe o intervalo, ou não se sabe nada.
ALTER TABLE estacao
    ALTER COLUMN andar_inicial DROP NOT NULL,
    ALTER COLUMN andar_final   DROP NOT NULL,
    ADD COLUMN chave_integracao TEXT,
    ADD CONSTRAINT ck_estacao_andares_juntos CHECK ((andar_inicial IS NULL) = (andar_final IS NULL));

-- Uma estação por (cliente, localização interna normalizada). Nula na estação cadastrada à mão.
CREATE UNIQUE INDEX ux_estacao_chave_integracao ON estacao (cliente_id, chave_integracao)
    WHERE chave_integracao IS NOT NULL;

-- ------------------------------------------------- valvula (RF-72, Q-1)

ALTER TABLE valvula
    ALTER COLUMN especificacao_id DROP NOT NULL,
    ADD COLUMN jiraya_equipamento_id BIGINT,
    ADD COLUMN fonte_atualizado_em   TEXT,
    ADD COLUMN sincronizado_em       TIMESTAMPTZ,
    ADD COLUMN fabricante            TEXT,
    ADD COLUMN modelo                TEXT,
    ADD COLUMN identificacao_externa TEXT,
    -- Especificação só é opcional para a válvula espelhada do Jiraya.
    ADD CONSTRAINT ck_valvula_especificacao
        CHECK (especificacao_id IS NOT NULL OR jiraya_equipamento_id IS NOT NULL);

CREATE UNIQUE INDEX ux_valvula_jiraya ON valvula (jiraya_equipamento_id)
    WHERE jiraya_equipamento_id IS NOT NULL;

-- ------------------------------------------------- bomba (RF-72, P-22)

ALTER TABLE bomba
    ALTER COLUMN periodicidade_meses SET DEFAULT 12,
    ADD COLUMN jiraya_equipamento_id BIGINT,
    ADD COLUMN fonte_atualizado_em   TEXT,
    ADD COLUMN sincronizado_em       TIMESTAMPTZ,
    ADD COLUMN identificacao_externa TEXT;

CREATE UNIQUE INDEX ux_bomba_jiraya ON bomba (jiraya_equipamento_id)
    WHERE jiraya_equipamento_id IS NOT NULL;

-- ------------------------------------------------- conjunto e ocupação (RF-73, Q-4, D-27)

ALTER TABLE conjunto_bombeamento ADD COLUMN chave_integracao TEXT;

-- O tipo entra na chave: a mesma casa de bombas pode ter recalque e incêndio (Q-4).
CREATE UNIQUE INDEX ux_conjunto_chave_integracao
    ON conjunto_bombeamento (cliente_id, tipo_bomba_id, chave_integracao)
    WHERE chave_integracao IS NOT NULL;

-- No modo externa quem põe a bomba no conjunto derivado é o sincronizador, não uma
-- pessoa. A posição continua sendo a ocupação aberta (D-27), sem segundo caminho.
ALTER TABLE ocupacao_bomba
    ALTER COLUMN registrado_por_id DROP NOT NULL,
    ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'manual'
        CHECK (origem IN ('manual', 'integracao')),
    ADD CONSTRAINT ck_ocupacao_registrado_por
        CHECK (origem <> 'manual' OR registrado_por_id IS NOT NULL);

-- ------------------------------------------------- servico (RF-74, RF-76, RF-80, RN-26)

ALTER TABLE servico
    ALTER COLUMN valvula_id             DROP NOT NULL,
    ALTER COLUMN tecnico_responsavel_id DROP NOT NULL,
    ALTER COLUMN criado_por_id          DROP NOT NULL,
    ADD COLUMN bomba_id          UUID REFERENCES bomba (id),
    -- P-29: o técnico vindo do Jiraya não é usuário do Valtis.
    ADD COLUMN tecnico_externo   TEXT,
    ADD COLUMN origem            VARCHAR(20) NOT NULL DEFAULT 'manual'
        CHECK (origem IN ('manual', 'jiraya')),
    ADD COLUMN jiraya_os_id      BIGINT,
    ADD COLUMN ativo             BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN motivo_inativacao TEXT,
    ADD COLUMN inativado_por_id  UUID REFERENCES usuario (id),
    ADD COLUMN inativado_em      TIMESTAMPTZ,
    ADD CONSTRAINT ck_servico_equipamento CHECK (num_nonnulls(valvula_id, bomba_id) = 1),
    ADD CONSTRAINT ck_servico_tecnico
        CHECK (tecnico_responsavel_id IS NOT NULL OR tecnico_externo IS NOT NULL),
    ADD CONSTRAINT ck_servico_criado_por
        CHECK (origem <> 'manual' OR criado_por_id IS NOT NULL),
    ADD CONSTRAINT ck_servico_os
        CHECK ((origem = 'jiraya') = (jiraya_os_id IS NOT NULL)),
    ADD CONSTRAINT ck_servico_inativacao
        CHECK (ativo OR (motivo_inativacao IS NOT NULL AND inativado_em IS NOT NULL));

-- RN-22: a instalação também inicia o ciclo.
ALTER TABLE servico DROP CONSTRAINT servico_tipo_manutencao_check;
ALTER TABLE servico ADD CONSTRAINT ck_servico_tipo_manutencao
    CHECK (tipo_manutencao IN ('Preventiva', 'Corretiva', 'Instalacao'));

CREATE INDEX idx_servico_bomba ON servico (bomba_id, data_realizada DESC);

-- RN-23: uma OS vira N serviços, um por equipamento; reaplicar a mesma OS não duplica.
CREATE UNIQUE INDEX ux_servico_os_valvula ON servico (jiraya_os_id, valvula_id)
    WHERE jiraya_os_id IS NOT NULL AND valvula_id IS NOT NULL;
CREATE UNIQUE INDEX ux_servico_os_bomba ON servico (jiraya_os_id, bomba_id)
    WHERE jiraya_os_id IS NOT NULL AND bomba_id IS NOT NULL;

-- ------------------------------------------------- oportunidade (D-31)

ALTER TABLE oportunidade ADD COLUMN bomba_id UUID REFERENCES bomba (id);

ALTER TABLE oportunidade DROP CONSTRAINT oportunidade_escopo_check;
ALTER TABLE oportunidade DROP CONSTRAINT ck_oportunidade_escopo;
ALTER TABLE oportunidade ADD CONSTRAINT ck_oportunidade_escopo CHECK (
       (escopo = 'valvula' AND valvula_id IS NOT NULL AND bomba_id IS NULL)
    OR (escopo = 'bomba'   AND bomba_id IS NOT NULL AND valvula_id IS NULL)
    OR (escopo = 'cliente' AND valvula_id IS NULL AND bomba_id IS NULL));

-- No máximo UM ciclo aberto por bomba, como já existe para a válvula.
CREATE UNIQUE INDEX ux_oportunidade_aberta_por_bomba ON oportunidade (bomba_id)
    WHERE status IN ('agendado', 'contatado', 'aceito');

-- ------------------------------------------------- entrada: Outbox invertido (D-30, RN-23)

CREATE TABLE evento_recebido (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origem               VARCHAR(20) NOT NULL CHECK (origem IN ('manutec_clientes', 'jiraya')),
    tipo                 VARCHAR(20) NOT NULL CHECK (tipo IN ('cliente', 'equipamento', 'ordem_servico')),
    id_externo           BIGINT      NOT NULL,
    -- atualizado_em da origem, texto exato como recebido (convenção dos dois contratos).
    versao               TEXT        NOT NULL,
    corpo                JSONB       NOT NULL,
    estado               VARCHAR(20) NOT NULL DEFAULT 'pendente'
        CHECK (estado IN ('pendente', 'aplicado', 'pendencia', 'falhou', 'descartado', 'superado')),
    motivo               TEXT,
    tentativas           INTEGER     NOT NULL DEFAULT 0 CHECK (tentativas >= 0),
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ultimo_erro          TEXT,
    resolvido_por_id     UUID REFERENCES usuario (id),
    resolvido_em         TIMESTAMPTZ,
    justificativa        TEXT,
    recebido_em          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    aplicado_em          TIMESTAMPTZ,
    -- Reler o mesmo lote não duplica nada: a garantia mora no banco (RN-23).
    CONSTRAINT uk_evento_recebido UNIQUE (origem, tipo, id_externo, versao),
    -- RF-75: nada é descartado em silêncio.
    CONSTRAINT ck_evento_descarte CHECK (estado <> 'descartado' OR justificativa IS NOT NULL)
);

-- Varredura do worker, como a do evento_agenda.
CREATE INDEX idx_evento_recebido_pendente ON evento_recebido (proxima_tentativa_em)
    WHERE estado = 'pendente';
CREATE INDEX idx_evento_recebido_item ON evento_recebido (origem, tipo, id_externo);

CREATE TABLE cursor_integracao (
    origem               VARCHAR(20) NOT NULL,
    recurso              VARCHAR(20) NOT NULL,
    desde                TEXT,
    apos_id              BIGINT,
    ultima_leitura_em    TIMESTAMPTZ,
    ultima_leitura_ok_em TIMESTAMPTZ,
    ultimo_erro          TEXT,
    PRIMARY KEY (origem, recurso)
);

COMMENT ON TABLE evento_recebido IS
    'Fila de entrada do modo integrado (D-30): cada item lido é gravado antes de ser aplicado';
COMMENT ON TABLE cursor_integracao IS
    'Cursor de leitura por fonte (RF-78): atualizado_em e id do último item gravado';
