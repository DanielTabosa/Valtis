-- Fase 1: oportunidades (M5, M10) e a fila de saída da agenda (Outbox, D-13 e D-21).
-- Ver docs/database.md.

-- A tabela de teste da Fase 0 deixa de existir junto com o código que a lia.
DROP TABLE IF EXISTS teste_conexao;

CREATE TABLE oportunidade (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    escopo                   VARCHAR(20) NOT NULL CHECK (escopo IN ('valvula', 'cliente')),
    cliente_id               UUID        NOT NULL REFERENCES cliente (id),
    valvula_id               UUID        REFERENCES valvula (id),
    servico_origem_id        UUID        REFERENCES servico (id),
    data_prevista            DATE        NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'agendado'
                                 CHECK (status IN ('agendado', 'contatado', 'aceito',
                                                   'recusado', 'executado', 'perdido')),
    responsavel_id           UUID        REFERENCES usuario (id),
    assumida_em              TIMESTAMPTZ,
    motivo_recusa            TEXT,
    origem                   VARCHAR(20) NOT NULL DEFAULT 'sistema'
                                 CHECK (origem IN ('sistema', 'importacao')),
    notas                    TEXT,
    servico_resultante_id    UUID        REFERENCES servico (id),
    oportunidade_anterior_id UUID        REFERENCES oportunidade (id),
    encerrada_em             TIMESTAMPTZ,
    criado_em                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Escopo válvula sempre tem válvula; escopo cliente (importados, D-09) nunca tem.
    CONSTRAINT ck_oportunidade_escopo CHECK (
        (escopo = 'valvula' AND valvula_id IS NOT NULL)
        OR (escopo = 'cliente' AND valvula_id IS NULL))
);

-- No máximo UM ciclo aberto por válvula. Se um bug tentar abrir dois, o banco recusa.
CREATE UNIQUE INDEX ux_oportunidade_aberta_por_valvula
    ON oportunidade (valvula_id)
    WHERE status IN ('agendado', 'contatado', 'aceito');

CREATE INDEX idx_oportunidade_status_data ON oportunidade (status, data_prevista);
CREATE INDEX idx_oportunidade_cliente     ON oportunidade (cliente_id);

-- Fila de saída para o Google Calendar. Uma linha por alerta (RN-06).
CREATE TABLE evento_agenda (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oportunidade_id      UUID        NOT NULL REFERENCES oportunidade (id),
    tipo                 VARCHAR(20) NOT NULL CHECK (tipo IN ('alerta_comercial', 'lembrete_final')),
    data_evento          DATE        NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'pendente'
                             CHECK (status IN ('pendente', 'criado', 'falhou', 'descartado')),
    google_event_id      TEXT,
    tentativas           INTEGER     NOT NULL DEFAULT 0 CHECK (tentativas >= 0),
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ultimo_erro          TEXT,
    sincronizado_em      TIMESTAMPTZ,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Idempotência no banco: nunca dois alertas do mesmo tipo para a mesma oportunidade.
    CONSTRAINT uk_evento_por_oportunidade UNIQUE (oportunidade_id, tipo)
);

-- Varredura do worker: só os pendentes, pela hora da próxima tentativa.
CREATE INDEX idx_evento_agenda_pendente
    ON evento_agenda (proxima_tentativa_em)
    WHERE status = 'pendente';

-- Serviços lançados antes desta migration (o seed de demonstração) ganham o
-- ciclo que teriam ganho se a Fase 1 já existisse: uma oportunidade a partir
-- do último serviço concluído de cada válvula.
INSERT INTO oportunidade (escopo, cliente_id, valvula_id, servico_origem_id, data_prevista)
SELECT DISTINCT ON (s.valvula_id)
       'valvula',
       e.cliente_id,
       s.valvula_id,
       s.id,
       (s.data_realizada + make_interval(months => v.periodicidade_meses))::DATE
FROM servico s
         JOIN valvula v ON v.id = s.valvula_id
         JOIN estacao e ON e.id = v.estacao_id
WHERE s.status = 'concluido'
ORDER BY s.valvula_id, s.data_realizada DESC, s.criado_em DESC;

-- E os alertas ainda no futuro. 30 e 7 são os valores padrão de D-08;
-- daqui em diante quem calcula é a aplicação, a partir da configuração.
INSERT INTO evento_agenda (oportunidade_id, tipo, data_evento)
SELECT id, 'alerta_comercial', data_prevista - 30
FROM oportunidade
WHERE data_prevista - 30 >= CURRENT_DATE;

INSERT INTO evento_agenda (oportunidade_id, tipo, data_evento)
SELECT id, 'lembrete_final', data_prevista - 7
FROM oportunidade
WHERE data_prevista - 7 >= CURRENT_DATE;
