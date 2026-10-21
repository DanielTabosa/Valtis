# Requisitos — Valtis

Fonte de verdade sobre **o que** o sistema deve fazer. Arquitetura em [architecture.md](architecture.md), modelo de dados em [database.md](database.md), justificativas em [decisions.md](decisions.md).

**IDs são estáveis e nunca reaproveitados.** Requisito removido vira `~~riscado~~` com a nota do porquê; o número não volta a ser usado.

## Legenda

| Marca | Significado |
|---|---|
| `[INFORMADO]` | Veio da descrição do cliente |
| `[PLANILHA]` | Extraído das planilhas reais da Manutec — a fonte mais confiável |
| `[DECIDIDO]` | Fechado em [decisions.md](decisions.md) |
| `[SUGESTÃO]` | Proposta técnica ainda **não validada pelo Daniel**. Pode ser cortada |
| `[A DEFINIR]` | Pendente |

**Prioridade:** `P0` = MVP obrigatório · `P1` = próxima entrega · `P2` = desejável

---

## 1. Requisitos Funcionais

### 1.1 Acesso

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-01 | Login individual antes de qualquer acesso a dados | P0 | `[SUGESTÃO]` |
| RF-02 | Sessão longa no celular, sem relogin constante | P0 | `[SUGESTÃO]` |
| RF-03 | Administrador gerencia os usuários | P1 | `[SUGESTÃO]` |
| RF-04 | Todo registro guarda `criado_em`, `atualizado_em` e autor | P0 | `[PLANILHA]` |

Perfis: **técnico** (lança e edita serviços, anexa fotos, consulta histórico, assume oportunidades) e **administrador** (tudo do técnico + cadastros, inativação, gestão de usuários, importação, painel gerencial).

### 1.2 Cadastros

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-05 | Cadastrar **cliente** (condomínio) com código de referência, endereço, bairro, cidade, síndico, telefone, e-mail, tipo de atendimento e status de contrato | P0 | `[PLANILHA]` |
| RF-06 | Cadastrar **especificação técnica** no catálogo: marca + modelo + diâmetro + tipo de registro, reutilizável por várias válvulas | P0 | `[PLANILHA]` |
| RF-07 | Cadastrar **estação** com localização, andar inicial, andar final e se exige fechamento geral | P0 | `[PLANILHA]` · D-07 |
| RF-08 | Cadastrar **válvula** vinculada a uma estação, com número (01/02), especificação, número de série, data de instalação e periodicidade | P0 | `[PLANILHA]` |
| RF-09 | Gerar automaticamente o código de referência no padrão `COND-001-VRP-01` | P0 | `[PLANILHA]` |
| RF-10 | Criar cliente / estação / válvula **de dentro do formulário de lançamento**, sem trocar de tela | P0 | `[SUGESTÃO]` |
| RF-11 | Buscar por nome do condomínio, código de referência, bairro ou número de série | P0 | `[SUGESTÃO]` |
| RF-12 | **Inativar** cliente, estação ou válvula. Inativos saem do painel e permanecem no histórico. Técnico e admin inativam válvula; cliente e estação, só admin | P0 | `[PLANILHA]` |
| RF-12a | **Excluir** válvula só quando ela não tem nenhum serviço nem oportunidade (cadastro feito por engano). Com histórico, o sistema recusa e orienta a inativar | P1 | `[DECIDIDO]` 16/10/2026 |
| RF-13 | Alertar duplicidade ao cadastrar válvula com mesma estação e mesmo número | P1 | `[PLANILHA]` |

### 1.3 Registro de serviço

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-14 | Lançar manutenção com data realizada, tipo (Preventiva/Corretiva), técnico responsável, peças substituídas e observações | P0 | `[PLANILHA]` |
| RF-15 | O registro é feito **por válvula individual**, mesmo quando o serviço é executado na estação inteira. Uma visita a 3 válvulas gera 3 registros | P0 | `[DECIDIDO]` · D-18 |
| RF-15b | Ao lançar, permitir **replicar o preenchimento** para as demais válvulas da mesma estação, ajustando apenas o que difere. Conveniência de tela; os registros permanecem independentes | P0 | `[SUGESTÃO]` · mitiga o custo assumido em D-18 |
| RF-16 | Pressão de entrada e saída como campos **opcionais** | P1 | `[DECIDIDO]` · D-10 |
| RF-17 | A data realizada é **informada pelo técnico**. A tela sugere a data de hoje e pede para conferir; o campo é obrigatório e é o técnico quem confirma | P0 | D-02 · revisto 16/10/2026 |
| RF-18 | Nunca sobrescrever registro antigo — toda intervenção é uma linha nova | P0 | `[PLANILHA]` |
| RF-19 | Anexar fotos pela **câmera ou pela galeria**, com compressão no dispositivo | P0 | `[DECIDIDO]` · D-02 |
| RF-20 | Salvar como rascunho e concluir depois | P1 | `[SUGESTÃO]` |
| RF-21 | Ver histórico completo de uma válvula e de uma estação, em ordem cronológica | P1 | `[SUGESTÃO]` |
| RF-22 | Apontar atendimentos executados e ainda não lançados após X dias | P2 | `[SUGESTÃO]` |

### 1.4 Recorrência e Google Calendar

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-23 | `data_proxima_manutencao = data_realizada + periodicidade_meses` (padrão 12) | P0 | `[INFORMADO]` `[PLANILHA]` |
| RF-24 | Periodicidade editável **por válvula**, apenas para exceções pontuais | P0 | `[PLANILHA]` |
| RF-25 | Tipo de atendimento (Contrato/Avulso) **não altera** o cálculo de prazo | P0 | `[PLANILHA]` |
| RF-26 | Criar evento na agenda da empresa **30 dias antes** do vencimento — alerta comercial | P0 | `[DECIDIDO]` · D-08 |
| RF-27 | Criar segundo evento **7 dias antes** — lembrete final | P0 | `[DECIDIDO]` · D-08 |
| RF-28 | Ambas as antecedências configuráveis num único lugar | P1 | `[PLANILHA]` |
| RF-29 | Evento traz condomínio, código de referência, localização da estação, andares atendidos, **se exige fechamento geral**, data da última manutenção e link para o registro | P0 | `[SUGESTÃO]` |
| RF-30 | Convidar os 3 usuários no evento | P0 | `[DECIDIDO]` · D-01 |
| RF-31 | Falha na API do Google **não impede** o salvamento; o agendamento entra em fila de reprocessamento | P0 | `[SUGESTÃO]` · D-13 |
| RF-32 | Editar data ou inativar equipamento atualiza/remove os eventos correspondentes | P1 | `[SUGESTÃO]` |
| RF-33 | Painel mostra o que não conseguiu sincronizar com o Calendar | P1 | `[SUGESTÃO]` |

### 1.5 Painel de status

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-34 | Painel calculado, **não editável**, com uma linha por válvula ativa | P0 | `[PLANILHA]` |
| RF-35 | Classificar em **VENCIDO · PRÓXIMO DO VENCIMENTO · EM DIA · SEM REGISTRO** | P0 | `[PLANILHA]` |
| RF-36 | Exibir dias restantes (negativo quando vencido) | P0 | `[PLANILHA]` |
| RF-37 | Destacar **SEM REGISTRO**: válvula cadastrada sem nenhuma manutenção lançada, sem data-base para calcular vencimento | P0 | `[PLANILHA]` |
| RF-38 | Filtrar por cliente, bairro, status, técnico, período e tipo de atendimento | P0 | `[DECIDIDO]` |
| RF-39 | Resumo com a contagem por situação | P0 | `[PLANILHA]` |
| RF-40 | Exportar a listagem filtrada para Excel/CSV | P2 | `[SUGESTÃO]` |

### 1.6 Pipeline de oportunidades

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-41 | Marcar desfecho: contatado / aceito / recusado / executado / perdido | P1 | `[SUGESTÃO]` |
| RF-42 | Oportunidade nasce **sem dono**; qualquer usuário assume com um toque | P1 | `[DECIDIDO]` · D-04 |
| RF-43 | Destacar oportunidade vencendo **sem ninguém ter assumido** | P1 | `[SUGESTÃO]` |
| RF-44 | Ao marcar `recusado`, **perguntar** se e quando voltar a lembrar | P1 | `[DECIDIDO]` · D-05 |
| RF-45 | Registrar motivo da recusa em lista curta | P2 | `[SUGESTÃO]` |
| RF-46 | Ao lançar a manutenção da reventa, fechar a oportunidade e abrir novo ciclo | P1 | `[SUGESTÃO]` |
| RF-47 | Dashboard: oportunidades no período, taxa de conversão, clientes recuperados | P2 | `[SUGESTÃO]` |

### 1.7 Relatório em PDF

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-48 | PDF do serviço com logo, dados do condomínio, estação, válvulas atendidas, peças, observações e data do próximo retorno | P1 | `[DECIDIDO]` |
| RF-49 | Compartilhar direto do celular (WhatsApp/e-mail) | P1 | `[SUGESTÃO]` |
| RF-50 | Enviar por e-mail ao cliente de dentro do sistema | P2 | `[SUGESTÃO]` |

### 1.8 Importação da base real

Ver o mapeamento completo em [database.md](database.md#importação-da-base-legada).

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-51 | Importar os **296 clientes** com a **data da última manutenção** | P0 | `[DECIDIDO]` · D-09 |
| RF-52 | **Não** importar as 595 intervenções individuais | P0 | `[DECIDIDO]` · D-09 |
| RF-53 | Clientes importados entram sem estação e sem válvula, com marcação de "cadastro incompleto" | P0 | `[SUGESTÃO]` |
| RF-54 | Gerar oportunidade **de escopo cliente** para importados, já classificada como vencida quando aplicável | P0 | `[SUGESTÃO]` |
| RF-55 | **Não criar eventos retroativos** no Calendar para vencimentos passados | P0 | `[SUGESTÃO]` |
| RF-56 | Pré-visualizar, apontar linhas problemáticas e permitir correção antes de gravar. As colunas são reconhecidas pelo cabeçalho; as que não existem no cadastro são descartadas | P0 | `[DECIDIDO]` · D-25 |
| RF-57 | Deduplicação **assistida** de nomes de cliente — o sistema agrupa candidatos, o humano decide | P0 | `[PLANILHA]` |
| RF-58 | Marcar a data importada como **aproximada** | P0 | `[PLANILHA]` |
| RF-59 | Ao lançar a primeira manutenção real de um cliente importado, completar o cadastro e retirar o estado "incompleto" | P1 | `[SUGESTÃO]` |

### 1.9 Módulo de bombas — MVP 1 `[PROPOSTA · 20/10/2026]`

> **Status:** briefing do Daniel de 20/10/2026; schema (`V5`) e regras RN-15 a RN-20 **aprovados por ele no mesmo dia** e implementados na branch `feat/modulo-bombas`. Modelo em [database.md](database.md#módulo-de-bombas). O módulo de válvulas não mudou.

Estrutura: `cliente (condomínio) → conjunto de bombeamento → bomba`. Uma bomba é um equipamento físico com identidade própria; a **posição** que ela ocupa num conjunto pode mudar ao longo do tempo (empréstimo, substituição), e o histórico pertence à bomba, não à posição — o mesmo princípio de D-18.

| ID | Requisito | Prio | Origem |
|---|---|---|---|
| RF-60 | Catálogo **configurável** de tipos de bombeamento, com os iniciais: recalque, incêndio, drenagem, pressurização, pluvial, piscina, jacuzzi e esgoto. Admin inclui e inativa tipos; a tela usa lista suspensa (RNF-06) | P0 | `[INFORMADO]` |
| RF-61 | Cadastrar **conjunto de bombeamento** vinculado ao cliente, com tipo (RF-60), localização e observações | P0 | `[INFORMADO]` |
| RF-62 | Cadastrar **bomba individual** com código, fabricante, modelo, potência (CV), tensão (V), corrente (A), função, data de instalação, periodicidade e última manutenção. A **próxima manutenção é calculada**, nunca digitada (RN-16) | P0 | `[INFORMADO]` |
| RF-63 | Dados técnicos desconhecidos (fabricante, modelo, potência, tensão, corrente) podem ficar em branco e ser **completados depois**. A bomba aparece marcada "dados técnicos incompletos" enquanto faltar algum | P0 | `[INFORMADO]` |
| RF-64 | Gerar o código da bomba automaticamente: `COND-001-BMB-01` para bomba do condomínio, `MAN-BMB-001` para bomba patrimonial da Manutec (RN-15) | P0 | `[SUGESTÃO]` · espelha RF-09 |
| RF-65 | Cadastrar **bomba patrimonial da Manutec** (sem condomínio dono) e consultar sua situação: disponível, ou emprestada a qual condomínio desde quando (RN-17) | P0 | `[INFORMADO]` |
| RF-66 | Registrar **movimentações**: instalação de bomba em posição de um conjunto, empréstimo de bomba patrimonial, devolução e retirada, sempre com data e, quando houver, qual bomba foi substituída | P0 | `[INFORMADO]` |
| RF-67 | Ver o **histórico individual** de cada bomba: posições ocupadas, períodos, substituições e quem registrou. Trocar uma bomba de posição não apaga nada (RN-20) | P0 | `[INFORMADO]` |
| RF-68 | Listagem e consulta **responsivas**: bombas por cliente e por conjunto, com próxima manutenção e situação; patrimônio da Manutec com disponibilidade | P0 | `[INFORMADO]` |
| RF-69 | Integrações futuras: Manutec_clientes como fonte de condomínios e contatos; Jiraya como sistema de OS que, após validação do gestor, atualiza o histórico no Valtis. **Não implementar agora** — sem contratos nem autorização (D-28) | P2 | `[A DEFINIR]` · P-18 |

**Fora do MVP 1 de bombas:** orçamento, financeiro, estoque de peças, ordem de serviço completa, sincronização real com outros sistemas, fotos e PDF de bomba.

---

## 2. Requisitos Não Funcionais

### 2.1 Usabilidade
Se lançar for chato, o sistema morre e a empresa volta para a planilha. Este é o bloco mais crítico.

| ID | Requisito | Origem |
|---|---|---|
| RNF-01 | Responsiva: mesma aplicação em celular e PC | `[INFORMADO]` |
| RNF-02 | Preenchível com uma mão; alvos de toque ≥ 44px | `[SUGESTÃO]` |
| RNF-03 | Lançamento completo em ≤ 3 minutos | `[SUGESTÃO]` |
| RNF-04 | Instalável como PWA na tela inicial | `[SUGESTÃO]` |
| RNF-05 | Teclado numérico automático em campos numéricos; seletor nativo de data | `[SUGESTÃO]` |
| RNF-06 | Listas suspensas com os valores fixos definidos em [database.md](database.md#enums), nunca texto livre | `[PLANILHA]` |
| RNF-07 | Interface em português do Brasil | `[SUGESTÃO]` |

### 2.2 Conectividade
D-02 removeu a necessidade de operação offline.

| ID | Requisito | Origem |
|---|---|---|
| RNF-08 | Perda momentânea de conexão não apaga o que já foi digitado | `[SUGESTÃO]` |
| RNF-09 | Upload de fotos com progresso e nova tentativa | `[SUGESTÃO]` |
| RNF-10 | ~~Sincronização offline~~ — **fora do escopo** por D-02 | `[DECIDIDO]` |

### 2.3 Desempenho e escala

| ID | Requisito | Origem |
|---|---|---|
| RNF-11 | Suportar 3 usuários simultâneos; até 20 sem mudança de arquitetura | `[SUGESTÃO]` |
| RNF-12 | Volume esperado: ~300 clientes, ~1.200 válvulas, ~100 serviços/ano. Em 10 anos, poucos milhares de linhas | `[PLANILHA]` |
| RNF-13 | Carregamento inicial em 3G ≤ 5s; navegação ≤ 1s | `[SUGESTÃO]` |

> **Nota para agentes:** o volume é pequeno. **Nenhuma decisão técnica deve ser justificada por desempenho** sem medição que a sustente. Cache, desnormalização e índice especulativo são complexidade sem contrapartida aqui.

### 2.4 Confiabilidade

| ID | Requisito | Origem |
|---|---|---|
| RNF-14 | Falha do Google **nunca** causa perda de registro | `[SUGESTÃO]` · D-13 |
| RNF-15 | Backup diário automático, retenção ≥ 30 dias, restauração testada | `[SUGESTÃO]` |
| RNF-16 | Exclusão sempre lógica (`ativo = false`) | `[PLANILHA]` |
| RNF-17 | Trilha de auditoria de criação, alteração e inativação | `[SUGESTÃO]` |
| RNF-18 | Disponibilidade alvo 99% em horário comercial | `[SUGESTÃO]` |

### 2.5 Segurança e conformidade

| ID | Requisito | Origem |
|---|---|---|
| RNF-19 | HTTPS em todo o tráfego | `[SUGESTÃO]` |
| RNF-20 | Senhas com hash forte; nenhum segredo no repositório | `[SUGESTÃO]` |
| RNF-21 | Refresh token do Google cifrado, fora do código, com alerta se for revogado | `[SUGESTÃO]` |
| RNF-22 | LGPD: contatos de ~296 síndicos e administradoras exigem base legal, finalidade declarada e política de retenção | `[SUGESTÃO]` |
| RNF-23 | Nenhuma tela pública com dado de cliente | `[SUGESTÃO]` |

### 2.6 Manutenibilidade e custo

| ID | Requisito | Origem |
|---|---|---|
| ~~RNF-24~~ | ~~Uma linguagem no front e no back~~ — **revogado por D-20**. Serão Java no backend e TypeScript no frontend. Custo assumido em troca do objetivo de aprendizado | `[DECIDIDO]` |
| RNF-25 | Deploy automatizado a partir do Git | `[SUGESTÃO]` |
| RNF-26 | Custo de infraestrutura ≤ R$ 150/mês na fase inicial | `[SUGESTÃO]` |
| RNF-27 | Dados integralmente exportáveis, sem lock-in | `[SUGESTÃO]` |
| RNF-28 | O banco usa o **mesmo vocabulário** da planilha da Manutec, não necessariamente os mesmos identificadores. O mapeamento fica em [database.md](database.md#importação-da-base-legada) | `[SUGESTÃO]` · D-14 |

---

## 3. Regras de Negócio

**Toda regra desta seção exige teste automatizado** (AGENTS.md §5) e **aprovação humana para ser alterada** (AGENTS.md §10).

| ID | Regra | Origem |
|---|---|---|
| RN-01 | `data_proxima_manutencao = data_realizada + periodicidade_meses` (padrão 12) | `[INFORMADO]` `[PLANILHA]` |
| RN-02 | Periodicidade é 12 meses para toda VRP, independentemente de marca, modelo ou de o cliente ser contrato ou avulso. Editável por válvula só em exceções | `[PLANILHA]` |
| RN-03 | `tipo_atendimento` é informação comercial e **não** altera o cálculo de prazo | `[PLANILHA]` |
| RN-04 | Status do painel: **VENCIDO** (dias restantes < 0) · **PRÓXIMO DO VENCIMENTO** (dentro da janela de 30 dias) · **EM DIA** · **SEM REGISTRO** (nenhuma manutenção lançada) | `[PLANILHA]` |
| RN-05 | Válvula **SEM REGISTRO** não tem data-base e por isso **não gera oportunidade automática** — entra em lista de pendência de cadastro | `[PLANILHA]` `[SUGESTÃO]` |
| RN-06 | Dois eventos por ciclo: 30 dias antes (comercial) e 7 dias antes (lembrete final) | `[DECIDIDO]` · D-08 |
| RN-07 | Todo serviço concluído gera uma oportunidade de escopo válvula, **sem responsável** | `[SUGESTÃO]` · D-04 |
| RN-08 | Qualquer usuário assume uma oportunidade da fila; o nome de quem assumiu fica visível | `[DECIDIDO]` · D-04 |
| RN-09 | Ao marcar `recusado`, perguntar se e quando voltar a lembrar. Se sim, nova oportunidade; se não, encerra até reativação manual | `[DECIDIDO]` · D-05 |
| RN-10 | Novo serviço fecha as oportunidades abertas da estação e inicia novo ciclo | `[SUGESTÃO]` |
| RN-11 | Nada é excluído fisicamente. Inativação lógica em todas as entidades | `[PLANILHA]` |
| RN-12 | Válvula ou cliente inativo não gera novas oportunidades | `[PLANILHA]` `[SUGESTÃO]` |
| RN-13 | **Nenhum** alerta com data passada vai para o Calendar, importado ou não. A oportunidade aberta que já está na janela de contato e não tem lembrete na agenda aparece na fila marcada "sem lembrete na agenda"; os alertas descartados ficam listados na tela Agenda, com o motivo | `[DECIDIDO]` 16/10/2026 |
| RN-14 | Código do equipamento = código do cliente + `-VRP-` + sequencial | `[PLANILHA]` |

### 3.1 Regras do módulo de bombas `[DECIDIDO]` · 20/10/2026

Formalizadas a partir do briefing de 20/10/2026 e aprovadas pelo Daniel no mesmo dia. Implementadas em `bomba/SituacaoBomba` (RN-16, RN-17), `bomba/OcupacaoBomba` (RN-18, RN-20) e `bomba/MovimentacaoService` (RN-17, RN-19), com testes. Onde o briefing não dizia, a lacuna está em **P-13 a P-19**, com a proposta que vale enquanto ele não responde.

| ID | Regra | Origem |
|---|---|---|
| RN-15 | Código da bomba de condomínio = código do cliente + `-BMB-` + sequencial do cliente (`COND-001-BMB-01`). Bomba patrimonial da Manutec = `MAN-BMB-` + sequencial global (`MAN-BMB-001`). Gerado pelo sistema; nunca muda, mesmo quando a bomba troca de posição | `[SUGESTÃO]` · espelha RN-14 |
| RN-16 | `proxima_manutencao = ultima_manutencao + periodicidade_meses` — o mesmo cálculo de RN-01, pela mesma classe (`RegrasRecorrencia`). Bomba sem última manutenção é **SEM REGISTRO** e não tem próxima data. A classificação do painel segue RN-04 | `[INFORMADO]` · periodicidade padrão em aberto (P-13) |
| RN-17 | Bomba patrimonial da Manutec está **disponível** quando não tem nenhuma ocupação aberta. Só bomba disponível pode ser emprestada. Disponibilidade é **calculada** a partir das ocupações, nunca gravada (D-27) | `[INFORMADO]` |
| RN-18 | A **devolução registrada** encerra a ocupação de empréstimo; só a partir dela a bomba volta a ficar disponível. Não existe devolução implícita: instalar outra bomba na posição não devolve a emprestada | `[INFORMADO]` |
| RN-19 | A bomba original pode ser **reinstalada antes** da retirada da emprestada. Por isso uma posição pode ter mais de uma ocupação aberta ao mesmo tempo; o sistema avisa, mas não impede | `[INFORMADO]` |
| RN-20 | Ocupação é histórico: nunca é apagada nem reescrita. Erro de registro se corrige com encerramento e nova ocupação, com observação do motivo. Mesma lógica de RF-18 e RN-11 | `[SUGESTÃO]` |

---

## 4. Fora de escopo

Declarado explicitamente. Um agente que "adicionar" qualquer item desta lista está fora do escopo acordado (AGENTS.md §9).

- Peças de reposição padrão e faixa de complexidade de orçamento `[PLANILHA]`
- Emissão de nota fiscal, financeiro, contas a receber
- Controle de estoque
- Roteirização de rotas de campo
- Portal de acesso para o cliente final
- Aplicativo nativo em loja (iOS/Android) — a solução é PWA
- Operação offline `[DECIDIDO]` · D-02

---

## 5. Pendências

| # | Pendência | Bloqueia |
|---|---|---|
| ~~P-01a~~ | ~~Quantas válvulas por estação?~~ **Resolvido:** N válvulas, tipicamente 2 (D-19) | — |
| P-01b | A estação inclui outros componentes relevantes ao serviço — filtro Y, manômetros, registros de bloqueio? Se sim, o escopo do serviço precisa listá-los | Modelagem final de `estacao` |
| P-01c | A periodicidade de 12 meses é da válvula ou da estação? | RN-02 |
| ~~P-02~~ | ~~Confirmar lançamento por estação~~ **Resolvido:** registro por válvula individual (D-18) | — |
| P-03 | Existe base de clientes sob contrato **fora** das 595 NFs analisadas? | Dimensionamento da importação |
| P-04 | Valor cobrado entra no registro? Sem ele, RF-47 mede conversão mas não receita | RF-47 |
| P-05 | As 3 pessoas são todas de campo, ou alguém é só de escritório? | Perfis de acesso |
| P-06 | Identificação física da válvula em campo: existe plaqueta? Vale QR Code? | RF-11 |
| P-07 | Teto de custo mensal, preferência por dados no Brasil, domínio disponível. Tendência (16/10/2026): **domínio único** para frontend e backend, o que reabre D-22 | Escolha de hospedagem |
| P-08 | ~~Painel de válvulas e lista de clientes importados: telas separadas ou visão unificada?~~ **Respondida em 16/10/2026: telas separadas.** O painel segue uma linha por válvula; os importados terão lista própria ("base antiga, cadastro incompleto") e entram na fila normalmente | RF-34, RF-53 |

### Pendências do módulo de bombas (20/10/2026)

| # | Pendência | Bloqueia | Proposta enquanto não há resposta |
|---|---|---|---|
| P-13 | Qual a **periodicidade padrão** da manutenção de bombas? É única (como os 12 meses da VRP) ou varia por tipo (incêndio ≠ piscina)? | RN-16 | Campo obrigatório no cadastro, **sem padrão**; a tela não sugere valor |
| P-14 | Manutenção de bomba **gera oportunidade e eventos na agenda** como a de VRP (RN-07, RN-06)? | M5/M6 para bombas | MVP 1 **não gera**. Painel de bombas só classifica (RN-16) |
| P-15 | O tipo (recalque, incêndio…) é do **conjunto** ou de cada **bomba**? | RF-61, RF-62 | Do conjunto. Na bomba, `funcao` é texto livre curto (ex.: "principal", "reserva") |
| P-16 | **Lançamento de serviço** para bomba segue o mesmo fluxo da válvula? Hoje `servico.valvula_id` é obrigatório | M3 para bombas | MVP 1 registra só `ultima_manutencao` no cadastro da bomba. Serviço por bomba fica para o MVP 2 |
| P-17 | Empréstimo de bomba patrimonial tem **cobrança** (locação)? | Escopo | Fora do escopo; não entra nem como campo |
| P-18 | **Contratos** de integração com Jiraya e Manutec_clientes: quem chama quem, formato, identificador comum do condomínio | RF-69 | Nada implementado (D-28) |
| P-19 | **Técnico** cadastra conjunto e bomba e registra movimentação, como faz com válvula (S-01)? | `@PreAuthorize` | Sim: mesmo perfil de permissão da válvula. Catálogo de tipos (RF-60), só admin |
