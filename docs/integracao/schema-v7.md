# Schema `V7`: oportunidade cancelada (proposta)

**Situação: proposta, aguardando aprovação do Daniel** (AGENTS.md §10, alteração de constraint e de regra RN-26). Escrita em 01/11/2026 a partir da resposta dele à P-32. **Nenhuma migration foi gerada nem rodada.** Depois do OK, vira `V7__oportunidade_cancelada.sql`, e `database.md`, `requirements.md` (RN-26, RN-08) e o código mudam no mesmo commit.

## O problema (P-32)

O admin inativa (RN-26) um serviço porque a OS foi reaberta ou cancelada no Jiraya, ou porque foi lançado por engano. Se esse serviço é o **único** ativo do equipamento e é dele que nasce o ciclo aberto, a oportunidade perde a origem. Hoje a inativação é **recusada** com uma mensagem citando a P-32 (`InativacaoDeServicoService.inativar`).

Encerrar como `perdido` não serve: `perdido` quer dizer "o cliente não fechou", e contaria como receita perdida.

## A resposta do Daniel (01/11/2026)

Encerrar a oportunidade com um **status novo, `cancelado`**, que não conta como receita perdida. O equipamento volta a SEM REGISTRO.

## O SQL

```sql
-- V7: oportunidade pode ser encerrada como 'cancelado' (P-32, RN-26).
-- Só alarga o CHECK: nenhuma linha muda, nenhuma é apagada.
ALTER TABLE oportunidade DROP CONSTRAINT oportunidade_status_check;
ALTER TABLE oportunidade ADD CONSTRAINT ck_oportunidade_status
    CHECK (status IN ('agendado', 'contatado', 'aceito',
                      'recusado', 'executado', 'perdido', 'cancelado'));
```

O nome `oportunidade_status_check` é o que o Postgres deu ao `CHECK` sem nome da `V4`. Conferir no banco local antes de gerar (`\d oportunidade`). O nome novo segue o padrão `ck_` da `V6`.

Os índices parciais de "um ciclo aberto por equipamento" (`WHERE status IN ('agendado', 'contatado', 'aceito')`) **não mudam**: `cancelado` é um estado de encerramento, como `perdido`.

## O que muda no código (depois do OK)

| Onde | Mudança |
|---|---|
| `StatusOportunidade` | `CANCELADO("cancelado")`, encerra a oportunidade, **fora** de `ABERTOS` |
| `Oportunidade` | Método `cancelar(motivo)`, só a partir de estado aberto. **Não** é desfecho que o usuário escolhe na fila (RF-41 continua com os cinco de hoje) |
| `InativacaoDeServicoService` | No lugar do erro da P-32: cancela as oportunidades abertas que nasceram do serviço e descarta os eventos pendentes da agenda (`FilaDaAgenda`, como já acontece num desfecho) |
| `OportunidadeService` | O equipamento fica sem ciclo aberto; o próximo serviço lançado abre um novo normalmente (RN-10) |
| Frontend | `tipos.ts` ganha `"cancelado"`; a fila e o histórico comercial mostram "Cancelada (serviço inativado)" |
| Testes | Inativar o único serviço: oportunidade `cancelado`, evento da agenda descartado, painel SEM REGISTRO, novo lançamento abre ciclo |

## Um caso a decidir junto: a bomba

A data-base da bomba é a mais recente entre o **cadastro** ("última manutenção" informada) e o último serviço ativo (RF-80). Então uma bomba cujo único serviço é inativado **não** fica necessariamente sem data-base: pode voltar a contar do cadastro.

**Proposta:** se a bomba tem data de cadastro, recontar o ciclo a partir dela (como já se faz quando há serviço anterior) em vez de cancelar; só cancelar quando também não houver data de cadastro. A válvula não tem esse campo, então para ela é sempre cancelar.

## O que quebra se der errado

- Nome da constraint diferente do esperado: a migração falha na subida e o Flyway para; nada é gravado (a migração roda numa transação). Corrige-se o nome e sobe de novo.
- Não há dado a migrar: nenhuma oportunidade existente vira `cancelado`.

## Alternativa descartada

Manter a recusa (como hoje) e pedir que o admin lance outro serviço antes de inativar. Descartada pelo Daniel em 01/11: obriga a inventar um serviço para corrigir um erro.
