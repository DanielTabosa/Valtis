-- Migration de Fase 0: existe apenas para provar que o Flyway está funcionando.
-- Não faz parte do modelo de dados (docs/database.md) e será removida
-- por uma migration futura antes da Fase 1.

CREATE TABLE teste_conexao (
    id          BIGSERIAL PRIMARY KEY,
    mensagem    TEXT        NOT NULL,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO teste_conexao (mensagem) VALUES ('Valtis conectado ao banco.');
