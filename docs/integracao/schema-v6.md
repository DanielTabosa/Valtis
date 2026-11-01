# Schema `V6`: modo integrado (proposta)

**Situação: proposta, aguardando aprovação do Daniel** (AGENTS.md §10, alteração de schema). Escrita em 01/11/2026; as decisões Q-1 a Q-4 foram respondidas por ele no mesmo dia (fim deste arquivo). Falta o OK no schema completo. **Nenhuma migration foi gerada nem rodada.** Depois de aprovada, vira `V6__modo_integrado.sql` e o `database.md` é atualizado no mesmo commit.

Esta proposta vem de D-29, D-30, D-31, RF-70 a RF-80, RN-21 a RN-25, do [contrato Jiraya → Valtis](jiraya.md) (P-27 a P-30) e do contrato do Manutec Clientes.

## O princípio

**As tabelas são as mesmas nos dois modos; só muda quem escreve nelas (D-29).** Por isso o `V6` não cria uma tabela "espelho" para cada coisa. Ele faz três coisas:

1. **Acrescenta a referência externa** às tabelas que já existem (`cliente`, `valvula`, `bomba`, `servico`).
2. **Afrouxa os campos obrigatórios** que o Jiraya não tem como preencher, por exemplo os andares da estação e a especificação da válvula.
3. **Cria duas tabelas novas** para a entrada de dados: `evento_recebido`, que é o Outbox invertido, e `cursor_integracao`.

O `V6` é **aditivo e não destrutivo**: nenhuma linha é apagada e nenhum valor existente muda. Os serviços já lançados ficam como `origem = 'manual'`. Ele também troca algumas constraints `CHECK` por versões mais largas. Isso não apaga nem altera nenhuma linha, mas é alteração de constraint e por isso entra na aprovação.

## O que muda, tabela por tabela

### `cliente`

| Mudança | Por quê |
|---|---|
| `+ manutec_id BIGINT`, único quando não nulo | Chave do cliente no Manutec Clientes (RF-71, P-18) |
| `+ fonte_atualizado_em TEXT` | `atualizado_em` da fonte, guardado como **texto exato** (convenção dos dois contratos) |
| `+ sincronizado_em TIMESTAMPTZ` | Última aplicação bem-sucedida (RF-78) |
| `origem` aceita também `'integracao'` | Hoje aceita só `'sistema'` e `'importacao'` |

O `codigo_referencia` (`COND-001`) continua sendo gerado pelo Valtis também para o cliente espelhado.

### `estacao` (VRPs)

| Mudança | Por quê |
|---|---|
| `andar_inicial` e `andar_final` passam a **opcionais** | O Jiraya não tem andares. A estação derivada (RF-73) nasce sem eles e fica "a completar" até alguém preencher no Valtis (Q-2). A constraint `ck_estacao_andares` continua valendo quando os dois existem. No cadastro manual (`propria`), a tela continua exigindo os andares |
| `+ chave_integracao TEXT`, única por (`cliente_id`, `chave_integracao`) | A estação derivada é "uma por (cliente, localização interna)". A chave é a localização normalizada pelo Java (sem diferença de caixa, acento ou espaço nas pontas). Sem localização, a chave é `"sem-localizacao"`. É nula em estação cadastrada à mão |

### `valvula`

| Mudança | Por quê |
|---|---|
| `+ jiraya_equipamento_id BIGINT`, único quando não nulo | Referência ao equipamento do Jiraya (RF-72) |
| `+ fonte_atualizado_em TEXT`, `+ sincronizado_em TIMESTAMPTZ` | Como em `cliente` |
| `especificacao_id` passa a **opcional**, com `CHECK`: obrigatória quando `jiraya_equipamento_id` é nulo | O Jiraya tem fabricante e modelo em texto livre, não o catálogo de especificação do Valtis (marca, modelo, diâmetro, registro). Ver Q-1 |
| `+ fabricante TEXT`, `+ modelo TEXT`, `+ identificacao_externa TEXT` | O que o Jiraya manda, para a ficha mostrar. `numero_serie` já existe |

O `numero_valvula` e o `codigo_referencia` (`COND-001-VRP-01`) continuam sendo gerados pelo Valtis.

### `bomba`

| Mudança | Por quê |
|---|---|
| `+ jiraya_equipamento_id BIGINT`, único quando não nulo | RF-72 |
| `+ fonte_atualizado_em TEXT`, `+ sincronizado_em TIMESTAMPTZ` | Como em `cliente` |
| `+ identificacao_externa TEXT` | A identificação da equipe (`BR-01`) que o Jiraya manda. `fabricante` e `modelo` já existem |
| `periodicidade_meses` ganha **padrão 12** | P-22. Hoje é obrigatória e sem padrão |

### `conjunto_bombeamento`

| Mudança | Por quê |
|---|---|
| `+ chave_integracao TEXT`, única por (`cliente_id`, `tipo_bomba_id`, `chave_integracao`) | Conjunto derivado (RF-73). **O tipo entra na chave**, porque o conjunto exige um tipo e a mesma casa de bombas pode ter recalque e incêndio. Ver Q-4 |

### `ocupacao_bomba`

| Mudança | Por quê |
|---|---|
| `registrado_por_id` passa a **opcional**, com `CHECK`: obrigatório quando `origem = 'manual'` | No modo `externa`, quem coloca a bomba no conjunto derivado é o sincronizador, não uma pessoa. Assim a ficha e o painel de bombas continuam lendo a posição pela ocupação aberta (D-27), sem um segundo caminho |
| `+ origem VARCHAR(20)`, `'manual'` ou `'integracao'`, padrão `'manual'` | Distingue as duas origens |

### `servico`

É a mudança maior: o serviço passa a aceitar válvula **ou** bomba (RF-80) e passa a poder vir do Jiraya (RF-74).

| Mudança | Por quê |
|---|---|
| `valvula_id` passa a **opcional**; `+ bomba_id UUID FK → bomba` | RF-80, D-31 |
| `CHECK (num_nonnulls(valvula_id, bomba_id) = 1)` | Exatamente um dos dois |
| `tipo_manutencao` aceita também `'Instalacao'` | A instalação também inicia o ciclo (RN-22) |
| `tecnico_responsavel_id` passa a **opcional**; `+ tecnico_externo TEXT`; `CHECK`: um dos dois preenchido | O técnico vindo do Jiraya não é usuário do Valtis (P-29) |
| `criado_por_id` passa a **opcional**, com `CHECK`: obrigatório quando `origem = 'manual'` | O serviço que vem do Jiraya não tem quem o criou no Valtis |
| `+ origem VARCHAR(20)`, `'manual'` ou `'jiraya'`, padrão `'manual'` | RF-76: o lançamento manual aparece como "lançado no Valtis" |
| `+ jiraya_os_id BIGINT`; índice único em (`jiraya_os_id`, `valvula_id`) e em (`jiraya_os_id`, `bomba_id`) | Uma OS vira N serviços, um por equipamento. Reaplicar a mesma OS não duplica (RN-23) |
| `+ ativo BOOLEAN` com padrão `TRUE`, `+ motivo_inativacao TEXT`, `+ inativado_por_id`, `+ inativado_em` | P-27: o admin pode inativar o serviço de uma OS reaberta ou cancelada. Hoje o serviço não tem inativação. Ver Q-3 |
| Índice `idx_servico_bomba (bomba_id, data_realizada DESC)` | Igual ao da válvula |

### `oportunidade`

| Mudança | Por quê |
|---|---|
| `escopo` aceita também `'bomba'`; `+ bomba_id UUID FK → bomba` | D-31: a bomba gera oportunidade como a VRP (responde P-14) |
| `ck_oportunidade_escopo` reescrita | Escopo `valvula` só com válvula, `bomba` só com bomba, `cliente` sem nenhum dos dois |
| Índice único `ux_oportunidade_aberta_por_bomba` | No máximo um ciclo aberto por bomba, como já existe para a válvula |

### Novas: `evento_recebido` e `cursor_integracao`

**`evento_recebido`** é a fila de entrada, o Outbox invertido (D-30). Cada item lido é gravado **antes** de ser aplicado.

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `origem` | `'manutec_clientes'` \| `'jiraya'` | |
| `tipo` | `'cliente'` \| `'equipamento'` \| `'ordem_servico'` | |
| `id_externo` | bigint | Id na origem |
| `versao` | text | `atualizado_em` da origem, como texto exato |
| `corpo` | jsonb | O item inteiro, como chegou |
| `estado` | `'pendente'` \| `'aplicado'` \| `'pendencia'` \| `'falhou'` \| `'descartado'` \| `'superado'` | Os estados são explicados logo abaixo da tabela |
| `motivo` | text, nulo | Por que virou pendência (RF-75): tipo não mapeado, cliente sem referência, OS sem tipo, OS reaberta… |
| `tentativas`, `proxima_tentativa_em`, `ultimo_erro` | | Retentativa, como no `evento_agenda` |
| `resolvido_por_id`, `resolvido_em`, `justificativa` | | Quem resolveu ou descartou a pendência, e por quê. Descartar exige justificativa (RF-75) |
| `recebido_em`, `aplicado_em` | timestamptz | |

Os estados:

- **`pendente`**: gravado e ainda não aplicado.
- **`aplicado`**: aplicado com sucesso.
- **`pendencia`**: problema de negócio. Precisa de uma pessoa e aparece na tela Pendências (RF-75).
- **`falhou`**: problema técnico. O worker tenta de novo.
- **`descartado`**: o admin descartou, com justificativa.
- **`superado`**: chegou uma versão mais nova do mesmo item antes de esta ser aplicada. Ela não é aplicada por cima da nova.

Restrições e índices:

- **Índice único** em (`origem`, `tipo`, `id_externo`, `versao`). É a garantia, no banco, de que reler o mesmo lote não duplica nada (RN-23).
- **Índice de varredura** em `proxima_tentativa_em`, filtrado para `estado = 'pendente'`, igual ao do `evento_agenda`.

**`cursor_integracao`** tem uma linha por fonte lida.

| Coluna | Tipo | Notas |
|---|---|---|
| `origem` + `recurso` | PK | (`manutec_clientes`, `clientes`), (`jiraya`, `equipamentos`), (`jiraya`, `ordens_servico`) |
| `desde` | text, nulo | `atualizado_em` do último item, como texto exato. Nulo antes da primeira carga |
| `apos_id` | bigint, nulo | Desempate do cursor |
| `ultima_leitura_em`, `ultima_leitura_ok_em` | timestamptz | RF-78 |
| `ultimo_erro` | text, nulo | RF-78 (401 aparece aqui) |

## O que o `V6` não faz

- **Não guarda o modo.** `propria` ou `externa` é configuração de subida (RF-70), não dado.
- **Não cria tabela de mapeamento de tipos.** A regra "contém VRP / contém bomba" é configuração (RF-72).
- **Não apaga o seed `V3`.** A remoção dos dados fictícios continua sendo uma decisão à parte (HANDOFF §4).
- **Não muda `usuario`.** Não precisa de usuário de sistema: a origem fica marcada nas próprias linhas.

## Efeito no código (para dimensionar, não é parte do schema)

- **`RegrasRecorrencia` e `LancamentoService`** passam a aceitar bomba. A regra é a mesma, em uma cópia só (D-30). O "último serviço" ignora o serviço inativo.
- **`Servico`, `Oportunidade` e `Ocupacao`** ganham os campos novos. Todas as consultas de serviço passam a filtrar `ativo`.
- **O painel e a ficha da bomba** passam a mostrar o histórico de serviços.
- **Testes:** cada `CHECK` novo ganha um teste de integração que tenta violá-lo.

## Rascunho do SQL

O rascunho mostra a forma, não é o arquivo final. Os nomes de constraint seguem o padrão das migrations anteriores.

```sql
-- cliente
ALTER TABLE cliente
    ADD COLUMN manutec_id          BIGINT,
    ADD COLUMN fonte_atualizado_em TEXT,
    ADD COLUMN sincronizado_em     TIMESTAMPTZ;
CREATE UNIQUE INDEX ux_cliente_manutec_id ON cliente (manutec_id) WHERE manutec_id IS NOT NULL;
ALTER TABLE cliente DROP CONSTRAINT cliente_origem_check;
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_origem
    CHECK (origem IN ('sistema', 'importacao', 'integracao'));

-- estacao
ALTER TABLE estacao
    ALTER COLUMN andar_inicial DROP NOT NULL,
    ALTER COLUMN andar_final   DROP NOT NULL,
    ADD COLUMN chave_integracao TEXT;
CREATE UNIQUE INDEX ux_estacao_chave_integracao ON estacao (cliente_id, chave_integracao)
    WHERE chave_integracao IS NOT NULL;

-- valvula
ALTER TABLE valvula
    ALTER COLUMN especificacao_id DROP NOT NULL,
    ADD COLUMN jiraya_equipamento_id BIGINT,
    ADD COLUMN fonte_atualizado_em   TEXT,
    ADD COLUMN sincronizado_em       TIMESTAMPTZ,
    ADD COLUMN fabricante            TEXT,
    ADD COLUMN modelo                TEXT,
    ADD COLUMN identificacao_externa TEXT,
    ADD CONSTRAINT ck_valvula_especificacao
        CHECK (especificacao_id IS NOT NULL OR jiraya_equipamento_id IS NOT NULL);
CREATE UNIQUE INDEX ux_valvula_jiraya ON valvula (jiraya_equipamento_id)
    WHERE jiraya_equipamento_id IS NOT NULL;

-- bomba
ALTER TABLE bomba
    ALTER COLUMN periodicidade_meses SET DEFAULT 12,
    ADD COLUMN jiraya_equipamento_id BIGINT,
    ADD COLUMN fonte_atualizado_em   TEXT,
    ADD COLUMN sincronizado_em       TIMESTAMPTZ,
    ADD COLUMN identificacao_externa TEXT;
CREATE UNIQUE INDEX ux_bomba_jiraya ON bomba (jiraya_equipamento_id)
    WHERE jiraya_equipamento_id IS NOT NULL;

-- conjunto e ocupação
ALTER TABLE conjunto_bombeamento ADD COLUMN chave_integracao TEXT;
CREATE UNIQUE INDEX ux_conjunto_chave_integracao
    ON conjunto_bombeamento (cliente_id, tipo_bomba_id, chave_integracao)
    WHERE chave_integracao IS NOT NULL;
ALTER TABLE ocupacao_bomba
    ALTER COLUMN registrado_por_id DROP NOT NULL,
    ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'manual'
        CHECK (origem IN ('manual', 'integracao')),
    ADD CONSTRAINT ck_ocupacao_registrado_por
        CHECK (origem <> 'manual' OR registrado_por_id IS NOT NULL);

-- servico
ALTER TABLE servico
    ALTER COLUMN valvula_id             DROP NOT NULL,
    ALTER COLUMN tecnico_responsavel_id DROP NOT NULL,
    ALTER COLUMN criado_por_id          DROP NOT NULL,
    ADD COLUMN bomba_id          UUID REFERENCES bomba (id),
    ADD COLUMN tecnico_externo   TEXT,
    ADD COLUMN origem            VARCHAR(20) NOT NULL DEFAULT 'manual'
        CHECK (origem IN ('manual', 'jiraya')),
    ADD COLUMN jiraya_os_id      BIGINT,
    ADD COLUMN ativo             BOOLEAN NOT NULL DEFAULT TRUE,
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
ALTER TABLE servico DROP CONSTRAINT servico_tipo_manutencao_check;
ALTER TABLE servico ADD CONSTRAINT ck_servico_tipo_manutencao
    CHECK (tipo_manutencao IN ('Preventiva', 'Corretiva', 'Instalacao'));
CREATE INDEX idx_servico_bomba ON servico (bomba_id, data_realizada DESC);
CREATE UNIQUE INDEX ux_servico_os_valvula ON servico (jiraya_os_id, valvula_id)
    WHERE jiraya_os_id IS NOT NULL AND valvula_id IS NOT NULL;
CREATE UNIQUE INDEX ux_servico_os_bomba ON servico (jiraya_os_id, bomba_id)
    WHERE jiraya_os_id IS NOT NULL AND bomba_id IS NOT NULL;

-- oportunidade
ALTER TABLE oportunidade ADD COLUMN bomba_id UUID REFERENCES bomba (id);
ALTER TABLE oportunidade DROP CONSTRAINT oportunidade_escopo_check;
ALTER TABLE oportunidade DROP CONSTRAINT ck_oportunidade_escopo;
ALTER TABLE oportunidade ADD CONSTRAINT ck_oportunidade_escopo CHECK (
       (escopo = 'valvula' AND valvula_id IS NOT NULL AND bomba_id IS NULL)
    OR (escopo = 'bomba'   AND bomba_id IS NOT NULL AND valvula_id IS NULL)
    OR (escopo = 'cliente' AND valvula_id IS NULL AND bomba_id IS NULL));
CREATE UNIQUE INDEX ux_oportunidade_aberta_por_bomba ON oportunidade (bomba_id)
    WHERE status IN ('agendado', 'contatado', 'aceito');

-- entrada (Outbox invertido) e cursor
CREATE TABLE evento_recebido (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origem               VARCHAR(20) NOT NULL CHECK (origem IN ('manutec_clientes', 'jiraya')),
    tipo                 VARCHAR(20) NOT NULL CHECK (tipo IN ('cliente', 'equipamento', 'ordem_servico')),
    id_externo           BIGINT      NOT NULL,
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
    CONSTRAINT uk_evento_recebido UNIQUE (origem, tipo, id_externo, versao),
    CONSTRAINT ck_evento_descarte CHECK (estado <> 'descartado' OR justificativa IS NOT NULL)
);
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
```

Os nomes `cliente_origem_check`, `servico_tipo_manutencao_check` e `oportunidade_escopo_check` são os que o Postgres dá às constraints sem nome do `V2` e do `V4`. Vou conferi-los no banco local antes de escrever o arquivo final.

## Decisões (Q-1 a Q-4), respondidas pelo Daniel em 01/11/2026

| # | Pergunta | Decisão |
|---|---|---|
| Q-1 | Válvula vinda do Jiraya não tem especificação do catálogo | Especificação **opcional** para a espelhada; a ficha mostra fabricante, modelo e identificação vindos do Jiraya |
| Q-2 | Estação derivada não tem andares | **O que o Jiraya tiver vem do Jiraya; o que ele não tiver fica pendente de preenchimento manual no Valtis.** Andares ficam opcionais, e a estação derivada sem andares aparece como "a completar" até alguém preencher. O "a completar" é calculado (andar nulo), sem coluna nova |
| Q-3 | Serviço pode ser inativado (P-27) | **Sim**, com motivo, quem e quando, pela tela de pendências. Serviço inativo sai do histórico ativo e do cálculo do ciclo, que volta a contar do serviço ativo anterior. Ajusta RN-07 e RN-10 |
| Q-4 | Conjunto derivado | Um por (cliente, localização, **tipo de bomba**). Ajusta o RF-73 |

### Consequência da Q-2: o que é editável no modo `externa`

A Q-2 refina a RN-24 ("cadastro espelhado não é editável"). A regra passa a ser **por campo**:

- **O campo que vem da fonte é só leitura no Valtis:** nome do cliente, tipo, localização, fabricante, modelo, série, identificação. Ele é sobrescrito a cada sincronização, e editá-lo no Valtis seria perder a edição.
- **O campo que a fonte não tem é do Valtis e é editável:** andares e "requer fechamento geral" na estação; especificação na válvula; periodicidade na válvula e na bomba (P-22). A sincronização **nunca** sobrescreve esses campos.

Isso não muda o schema; muda os controllers do modo `externa` (architecture.md §9) e a redação da RN-24.
