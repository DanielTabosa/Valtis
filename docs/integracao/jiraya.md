# Integração Jiraya → Valtis: contrato da API de leitura

**Situação: contrato vigente (v1).** Aprovado pelo Daniel em 31/10/2026, junto com os pontos P-27 a P-30 (seção [Pontos decididos](#pontos-decididos)). Escrito no Valtis conforme D-30 e P-24; a implementação é no repositório do Jiraya, como etapa antes da E8, e ainda não começou. Mudanças posteriores seguem a seção [Versionamento](#versionamento).

O molde é o contrato que o Jiraya já consome: `manutec-clientes/docs/INTEGRACAO_JIRAYA.md`. As convenções são as mesmas, para que quem fez um lado reconheça o outro.

## Contexto e decisão

Decisões do Valtis aprovadas em 30/10/2026:

- **D-29:** na Manutec, o Valtis roda no modo `externa`. O cliente vem do Manutec Clientes. O equipamento (VRP e bomba) e o serviço vêm do Jiraya. O Valtis fica só com o que nenhum dos dois faz: ciclo de 12 meses, oportunidade, fila, agenda e painel.
- **D-30:** o Valtis **puxa** os dados. A cada 5 minutos ele chama `alteracoes` com cursor, grava cada item recebido e só depois o aplica. Não há webhook.
- **D-31 e RN-22:** só reinicia o ciclo a OS **concluída** com tipo de serviço **manutenção preventiva**, **manutenção corretiva** ou **instalação**.
- **RN-24:** a integração é de mão única. O Valtis **nunca** escreve no Jiraya.

Fluxo esperado no Valtis:

1. A cada rodada, o Valtis lê os **tipos de equipamento** (lista curta) para saber quais são VRP e quais são bomba (RF-72).
2. Lê **equipamentos/alterações** a partir do cursor. Cria ou atualiza a válvula ou a bomba espelhada (RF-72, RF-73, RF-79).
3. Lê **ordens-servico/alterações** a partir do cursor. A OS concluída e de tipo que reinicia o ciclo vira um serviço por equipamento mapeado (RF-74). O que não puder ser aplicado vai para as pendências (RF-75).
4. Avança cada cursor só depois de gravar o lote (D-30).

## O que o Jiraya precisa ter antes desta API

São três itens novos no Jiraya. Hoje eles **não existem**.

| # | Item | Por quê |
|---|---|---|
| J-1 | **Tipo de serviço na OS** (`tipo_servico`). Valores: `manutencao_preventiva`, `manutencao_corretiva`, `instalacao`, `visita_tecnica`, `orcamento` e `outro`. Obrigatório para concluir a OS. As OS antigas ficam sem valor | RN-22. Sem esse campo, uma visita para orçar adiaria o alerta em 12 meses (D-31) |
| J-2 | **Data de execução na OS** (`data_execucao`, uma data sem hora). É o dia em que o serviço foi feito no cliente. Obrigatória para concluir e preenchida com o dia de hoje, mas editável (P-28) | `completed_at` é o instante do clique em "concluir". Numa OS lançada depois, ou retroativa (base histórica, P-25), o ciclo começaria na data errada. É o mesmo cuidado do D-02 no Valtis: a data é informada, nunca assumida |
| J-3 | **`atualizado_em` confiável** em equipamento e em OS. Ele muda sempre que mudar qualquer coisa que esta API devolve | Sem isso, o cursor perde alterações. Ver [Como o Valtis detecta mudanças](#como-o-valtis-detecta-mudancas) |

Os tipos de equipamento por função (P-21) continuam como um cadastro comum do Jiraya, sem migração. O patrimônio de bombas emprestadas (P-24) **não** entra nesta versão: no modo `externa` o Valtis não usa patrimônio (RF-77).

## Conexão e autenticação

- **URL base:** `https://<servidor-do-jiraya>/api/integracao/v1/`. Em desenvolvimento, `http://localhost:8080/api/integracao/v1/`. Em produção, somente HTTPS.
- **Cabeçalho obrigatório em toda chamada:** `Authorization: Bearer <token>`.
- **O token é um segredo de serviço** (de sistema para sistema), sem usuário nem sessão.
  - No Jiraya fica na variável `JIRAYA_INTEGRACAO_VALTIS_TOKEN`, com no mínimo 32 caracteres.
  - No Valtis fica em `VALTIS_JIRAYA_TOKEN`, ao lado de `VALTIS_JIRAYA_URL` (D-30).
  - Nunca vai no repositório, em URL ou em log.
- **Comparação em tempo constante.** Variável vazia ou ausente desliga a integração: toda chamada recebe 401.
- **Alcance do token:** ele **só** vale para as rotas deste documento e **só** para leitura (`GET` e `HEAD`). Não dá acesso a `/api/**` nem a nenhuma escrita. No sentido inverso, o JWT de usuário do Jiraya não acessa estas rotas.
  - **Atenção para quem implementar:** no Jiraya, todo endpoint sob `/api/**` exige uma Permission. Estas rotas precisam de uma cadeia de segurança própria, só com o token, sem perfil nem Permission.
- **Troca do token:** gerar um novo, atualizar a variável dos dois lados e reiniciar ambos. Durante a troca, as chamadas com o token antigo recebem 401.

## Convenções

As mesmas do contrato do Manutec Clientes:

- **Formato:** JSON UTF-8. Nomes de campo em português, `snake_case`, sem acento. A API interna do Jiraya é em inglês, mas esta segue a família dos contratos de integração da Manutec.
- **Datas e horas:** ISO 8601 no fuso da empresa (`America/Sao_Paulo`, D-48 do Jiraya), por exemplo `2026-10-24T10:15:30-03:00`. Datas puras vêm como `2026-10-24`.
  - Quando houver fração de segundo, ela vem junto. O Valtis guarda o valor exatamente como recebido e o devolve igual no cursor.
  - Em parâmetro de URL, o valor vai codificado.
- **Campo opcional sem valor** vem como `null`, nunca omitido.
- **Ids** são inteiros permanentes e nunca são reaproveitados. Equipamento excluído continua sendo devolvido, com `excluido: true` (exclusão lógica, RN-E4-16).
- **Campos desconhecidos:** o Valtis deve ignorá-los. Assim dá para acrescentar campos sem nova versão.
- **Itens completos:** os itens de `alteracoes` vêm **completos**, no mesmo formato do detalhe. O Valtis grava cada item como corpo do evento recebido (`evento_recebido`, D-30) e não precisa de uma segunda chamada por item. O volume é pequeno (cerca de 100 OS por ano).

## Rotas

### 1. Tipos de equipamento

`GET /api/integracao/v1/tipos-equipamento/`

Devolve todos os tipos, inclusive os inativos, por `id` crescente, sem paginação.

```json
{
  "resultados": [
    {"id": 1, "nome": "Bomba de recalque", "ativo": true, "atualizado_em": "2026-10-23T09:00:00-03:00"},
    {"id": 2, "nome": "Válvula redutora de pressão (VRP)", "ativo": true, "atualizado_em": "2026-10-23T09:00:00-03:00"},
    {"id": 3, "nome": "Registro", "ativo": true, "atualizado_em": "2026-10-23T09:00:00-03:00"}
  ]
}
```

O Valtis decide pelo nome se o tipo é VRP ou bomba (RF-72). Como renomear um tipo **não** altera o `atualizado_em` dos equipamentos daquele tipo, o Valtis relê esta lista a cada rodada. Assim, um tipo renomeado ou criado passa a valer na rodada seguinte.

### 2. Equipamento

`GET /api/integracao/v1/equipamentos/{id}/`

Devolve o equipamento, inclusive quando excluído ou substituído.

```json
{
  "id": 12,
  "tipo": {"id": 1, "nome": "Bomba de recalque"},
  "cliente": {"id": 7, "manutec_id": 412, "nome": "CONDOMINIO EDIFICIO SOLAR DAS PALMEIRAS"},
  "endereco": {"id": 3, "manutec_endereco_id": 980},
  "localizacao_interna": "Bloco B — casa de bombas",
  "identificacao": "BR-01",
  "fabricante": "KSB",
  "modelo": "Meganorm",
  "numero_serie": "SN-123",
  "caracteristicas": "5 cv, trifásica",
  "substitui": null,
  "substituido_por": {"equipamento_id": 31, "em": "2026-10-28T14:00:00-03:00", "motivo": "Motor queimado", "ordem_servico_id": 1234},
  "excluido": false,
  "excluido_em": null,
  "criado_em": "2026-10-23T09:10:00-03:00",
  "atualizado_em": "2026-10-28T14:00:00-03:00"
}
```

- **`cliente.manutec_id`** é o id permanente do cliente no Manutec Clientes. É a chave que o Valtis usa para achar o cliente (P-18). Vem `null` quando o cliente é antigo e ainda não foi vinculado (RN-E3V2-25 do Jiraya). Nesse caso o Valtis manda o equipamento para as pendências ("cliente sem referência", RF-75) e não o descarta.
- **`endereco`** vem `null` quando o equipamento não tem endereço. O `manutec_endereco_id` também pode vir `null`, quando o endereço é antigo e não foi vinculado.
- **`localizacao_interna`** é o que o Valtis usa para derivar a estação ou o conjunto (RF-73). O texto vai exatamente como gravado; o Valtis compara sem diferenciar maiúsculas, acentos ou espaços nas pontas.
- **`substitui` e `substituido_por`** formam a cadeia da substituição física (RN-E4-12 a RN-E4-15). Têm o mesmo formato, e `ordem_servico_id` pode ser `null`. Em cada substituição, os **dois** equipamentos mudam de `atualizado_em`, o antigo e o novo (J-3).
- **Exclusão lógica:** `excluido: true` com `excluido_em` preenchido. O Valtis inativa o espelho. O motivo da exclusão não é exposto.

### 3. Equipamentos alterados desde um momento

`GET /api/integracao/v1/equipamentos/alteracoes/?desde=2026-10-24T10:00:00-03:00&limite=100`

Lista os equipamentos (de todos os tipos, inclusive excluídos e substituídos) cujo `atualizado_em` é posterior a `desde`, em ordem crescente de `atualizado_em` e `id`. Cada item tem o formato do detalhe.

| Parâmetro | Regra |
| --- | --- |
| `desde` | Opcional. Ausente = desde o início, usado na primeira carga |
| `apos_id` | Opcional. Desempate do cursor: com `desde`, devolve também os itens de `atualizado_em` **igual** a `desde` e `id` maior que `apos_id` |
| `limite` | Opcional, padrão 100, máximo 200 |

A paginação é por cursor e continua estável mesmo com alterações acontecendo durante a leitura:

- A resposta traz `proxima`, com o `desde` e o `apos_id` do último item.
- O Valtis segue `proxima` até vir `null`.
- Ao final, guarda o `atualizado_em` e o `id` do último item para a rodada seguinte.

```json
{
  "resultados": [{"id": 12, "tipo": {"id": 1, "nome": "Bomba de recalque"}, "...": "..."}],
  "limite": 100,
  "proxima": "https://.../api/integracao/v1/equipamentos/alteracoes/?desde=2026-10-28T14%3A00%3A00-03%3A00&apos_id=12&limite=100"
}
```

Esta rota não filtra por tipo, de propósito: quem sabe o que é VRP ou bomba é o Valtis (RF-72). O Valtis ignora os tipos não mapeados e os lista nas pendências.

### 4. Ordem de serviço

`GET /api/integracao/v1/ordens-servico/{id}/`

Devolve a OS em qualquer situação.

```json
{
  "id": 1234,
  "situacao": "concluida",
  "tipo_servico": "manutencao_corretiva",
  "data_execucao": "2026-10-27",
  "cliente": {"id": 7, "manutec_id": 412, "nome": "CONDOMINIO EDIFICIO SOLAR DAS PALMEIRAS"},
  "endereco": {"id": 3, "manutec_endereco_id": 980},
  "localizacao_interna": "Bloco B — casa de bombas",
  "descricao": "Bomba 2 sem pressão. Trocado selo mecânico e rolamentos.",
  "equipamentos": [
    {"item_id": 55, "equipamento_id": 12, "descricao": null},
    {"item_id": 56, "equipamento_id": null, "descricao": "Pressostato do quadro"}
  ],
  "conclusao": {
    "concluida_em": "2026-10-28T09:40:00-03:00",
    "concluida_por": {"id": 5, "nome": "Carlos Lima", "email": "carlos@manutec.com.br"}
  },
  "executores": [
    {"id": 9, "nome": "João Pereira", "email": "joao@manutec.com.br"}
  ],
  "criado_em": "2026-10-26T08:00:00-03:00",
  "atualizado_em": "2026-10-28T09:40:00-03:00"
}
```

- **`situacao`** é `aberta`, `pausada`, `concluida` ou `cancelada` (RN-E5-26 do Jiraya). Uma OS concluída pode ser **reaberta** (volta a `aberta`, RN-OS-21) e depois cancelada (RN-E5-27b). Por isso o Valtis recebe **todas** as situações, e não só as concluídas.
- **`tipo_servico`** usa os valores de J-1. Vem `null` em OS anteriores ao campo. O Valtis manda para as pendências a OS concluída sem tipo.
- **`data_execucao`** (J-2) vem `null` enquanto a OS não for concluída. Sem esse campo, o Valtis não tem a data para começar o ciclo (RF-74).
- **`equipamentos`** são os itens da OS (`service_order_equipment`). O item pode ser só uma descrição, sem equipamento cadastrado (`equipamento_id: null`). O Valtis ignora esses itens; se a OS inteira não tiver nenhum equipamento, ela vai para as pendências ("OS sem equipamento", RF-75).
- **`conclusao`** só vem preenchida com `situacao: "concluida"`; em qualquer outra situação vem `null`, inclusive na OS reaberta.
- **`concluida_por`** é quem concluiu a OS no sistema. Pelas regras do Jiraya, costuma ser o **gestor principal** (RN-E5-24), não o técnico que foi ao cliente.
- **`executores`** são as pessoas que participaram das etapas e não foram removidas (`stage_participants`), sem repetição, em ordem de nome. Pode vir vazia. O Valtis usa o primeiro executor como técnico do serviço e, se a lista vier vazia, quem concluiu (P-29).

### 5. Ordens de serviço alteradas desde um momento

`GET /api/integracao/v1/ordens-servico/alteracoes/?desde=2026-10-24T10:00:00-03:00&limite=100`

Os parâmetros, a ordenação e a paginação são os mesmos da rota 3. Devolve as OS **em qualquer situação** cujo `atualizado_em` é posterior a `desde`. Cada item tem o formato do detalhe.

### Como o Valtis detecta mudanças

O cursor só funciona se o `atualizado_em` mudar **sempre** que mudar algo que o detalhe devolve (J-3):

- **No equipamento:** tipo, cliente, endereço, localização, identificação, fabricante, modelo, série, características, substituição (nos dois lados) e exclusão.
- **Na OS:** situação (inclusive pausa, retomada, reabertura e cancelamento), tipo de serviço, data de execução, descrição, cliente, endereço, localização, inclusão ou remoção de equipamento, conclusão e executores (entrada ou saída de participante de etapa).

Se o Jiraya alterar algum desses dados sem renovar `atualizado_em`, o Valtis não vai ver a mudança. O mesmo limite já vale no Manutec Clientes para alterações feitas por SQL direto no banco.

## Como o Valtis aplica (resumo, não é parte do contrato)

Este resumo serve para quem implementa o Jiraya entender o uso. O que vale está nos requisitos do Valtis.

| Recebido | O Valtis faz |
|---|---|
| Tipo cujo nome contém "VRP" | Equipamento vira **válvula**, numa estação por (cliente, localização interna) (RF-72, RF-73) |
| Tipo cujo nome contém "bomba" | Equipamento vira **bomba**, num conjunto por (cliente, localização interna) |
| Outro tipo | Ignorado e listado nas pendências (RF-75) |
| Substituição | O antigo é inativado e o novo é criado, com o histórico do antigo preservado; o ciclo do novo começa sem registro (RF-79) |
| OS `concluida` com tipo `manutencao_preventiva`, `manutencao_corretiva` ou `instalacao` | Um **serviço** por equipamento mapeado, com `data_realizada = data_execucao`. Passa pelo **mesmo** `LancamentoService` do lançamento manual (RN-07, RN-10) |
| OS `concluida` com outro tipo | Nada: não reinicia o ciclo (RN-22) |
| OS com data anterior ao último serviço do equipamento | Entra no histórico e não muda o ciclo (RN-25) |
| Mesmo item relido | Nada é duplicado. Chave idempotente: (`jiraya`, tipo, id, `atualizado_em`) (RN-23) |
| OS já aplicada que volta como `aberta` ou `cancelada` | Vai para as pendências, e o admin decide se mantém ou inativa o serviço (P-27) |

## Erros

Formato único, o mesmo do Manutec Clientes:

```json
{"erro": {"codigo": "parametro_invalido", "mensagem": "desde não é uma data e hora ISO 8601 válida.", "campo": "desde"}}
```

| HTTP | `codigo` | Quando |
| --- | --- | --- |
| 400 | `parametro_invalido` | Parâmetro fora do limite ou data inválida. `campo` indica qual |
| 401 | `nao_autenticado` | Cabeçalho ausente, token errado ou integração desligada. A resposta vem com `WWW-Authenticate: Bearer` |
| 404 | `nao_encontrado` | Equipamento ou OS inexistente, ou rota desconhecida dentro do prefixo |
| 405 | `metodo_nao_permitido` | Qualquer método além de `GET` e `HEAD` |
| 500 | `erro_interno` | Falha inesperada; o Valtis tenta de novo na rodada seguinte |

O Valtis trata **401** como configuração errada: não repete em laço e mostra o erro na tela Estado da integração (RF-78). Trata **5xx** e falha de rede como temporários: mantém o cursor e os dados que já tem. O painel nunca depende de o Jiraya estar no ar (D-30).

## Versionamento

- A versão vai no caminho: `/api/integracao/v1/`.
- Dentro da v1 só entram mudanças compatíveis: campos novos, valores novos em `situacao` ou `tipo_servico`, e rotas novas. O Valtis manda para as pendências o valor que não conhece; não falha por isso.
- Remover ou renomear campo, ou mudar tipo ou significado, exige nova versão (`/v2/`). A v1 fica em paralelo até o Valtis migrar, em data combinada entre os dois lados.
- Este documento é a referência. O histórico de mudanças fica no fim dele. A cópia no repositório do Jiraya aponta para cá.

## Fora desta versão

- Escrita de qualquer tipo (RN-24).
- Patrimônio de bombas emprestadas: fica no Jiraya, e o Valtis não o usa no modo `externa` (RF-77).
- Registros de execução, evidências e fotos, atividades e etapas (fora os executores), orçamento e estoque.
- Planos preventivos: o Valtis não filtra por plano (RN-21).
- Webhooks ou avisos ativos para o Valtis (D-30).
- Tipo de serviço por **equipamento** da OS: o tipo é da OS inteira, e a equipe abre OS separadas quando os serviços forem diferentes (P-30).

## Pontos decididos

Respondidos pelo Daniel em 31/10/2026; a escolha foi a recomendada em todos os quatro.

| # | Pergunta | Decisão | Descartado |
|---|---|---|---|
| P-27 | O que acontece com o serviço já criado quando a OS é **reaberta** ou **cancelada** no Jiraya? | Vai para as **pendências**: o admin vê o motivo e decide se mantém ou inativa o serviço. Nenhuma regra automática nova | Suspender o serviço na reabertura e inativá-lo no cancelamento, recalculando o ciclo. Mexeria em RN-07 e RN-10 por um caso raro |
| P-28 | De onde vem a data do serviço? | Campo novo **`data_execucao`** na OS do Jiraya (J-2), informado na conclusão | A data de `completed_at`. Erra na OS concluída depois e na base histórica |
| P-29 | Quem é o **técnico** do serviço no Valtis? | O primeiro dos **`executores`**; com a lista vazia, quem concluiu. Gravado como texto, porque essa pessoa não é usuário do Valtis. Ajusta o RF-74 | Sempre quem concluiu. Costuma ser o gestor, não o técnico |
| P-30 | OS com equipamentos de serviços diferentes (manutenção numa bomba, só visita na outra) | Tipo **por OS**. A equipe abre OS separadas quando os serviços forem diferentes | Tipo por item da OS. Mais trabalho no Jiraya |

## Consequências para o schema `V6` do Valtis

Estas consequências ainda não estão aprovadas e vão na apresentação do `V6` (HANDOFF §13, item 0c):

- `servico.tecnico_responsavel_id` hoje é obrigatório e aponta para `usuario`. O técnico que vem do Jiraya não é usuário do Valtis, então o serviço precisa de um campo de texto para o técnico externo, e a FK passa a ser opcional (P-29).
- `servico.tipo_manutencao` hoje é `Preventiva` ou `Corretiva`. A **instalação** também inicia o ciclo (RN-22), então o enum ganha um valor.
- Cursores separados para equipamentos e para OS, cada um com `atualizado_em` (texto exato) e o último `id`.

## Histórico do contrato

- 31/10/2026: proposta inicial (v1), escrita no Valtis; P-27 a P-30 decididos e contrato aprovado pelo Daniel no mesmo dia.
