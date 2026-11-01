-- V7: oportunidade pode ser encerrada como 'cancelado' (P-32, RN-26).
-- Aprovada pelo Daniel em 01/11/2026 (docs/integracao/schema-v7.md).
--
-- 'cancelado' encerra o ciclo que nasceu de um serviço inativado quando não há
-- outra data-base para recontar. Não conta como receita perdida (isso é 'perdido').
-- Só alarga o CHECK: nenhuma linha muda, nenhuma é apagada. Os índices de
-- "um ciclo aberto por equipamento" não mudam: 'cancelado' é estado de encerramento.

ALTER TABLE oportunidade DROP CONSTRAINT oportunidade_status_check;
ALTER TABLE oportunidade ADD CONSTRAINT ck_oportunidade_status
    CHECK (status IN ('agendado', 'contatado', 'aceito',
                      'recusado', 'executado', 'perdido', 'cancelado'));
