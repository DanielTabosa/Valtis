# Registro de decisões — Valtis

**Por que** o sistema é do jeito que é. Formato ADR: contexto, decisão, consequências, status.

> ## 📌 Este arquivo é append-only
>
> **Decisão registrada nunca é editada nem apagada.** Se uma decisão se mostrar errada, crie uma **nova entrada** que a substitua e marque a antiga como `Substituída por D-xx`.
>
> O histórico do raciocínio é o que impede o projeto de repetir o mesmo erro — inclusive erros que já foram considerados e descartados com bom motivo.
>
> Alterar uma decisão exige **aprovação humana** (AGENTS.md §10).

**Status possíveis:** `Aceita` · `Proposta` (aguarda validação) · `Substituída por D-xx`

---

## D-01 — Autenticação no Google Calendar via OAuth de conta Gmail

**Status:** Aceita · 13/08/2026

**Contexto.** O sistema precisa criar eventos numa agenda compartilhada e convidar os 3 usuários. A abordagem usual seria uma conta de serviço do Google Cloud. Porém a Manutec usa **Gmail comum**, não Google Workspace com domínio próprio.

**Decisão.** Uma conta Gmail **institucional** é dona da agenda "Manutenções VRP", compartilhada com os 3 usuários com permissão de alteração. O sistema autentica como essa conta via OAuth, guardando um refresh token.

**Alternativa descartada.** Conta de serviço com delegação: exige Google Workspace. Sem ele, a conta de serviço **não consegue enviar convites** para contas Gmail externas.

**Consequências.**
- O app OAuth **precisa ser publicado como "Em produção"** no Google Cloud Console. Em modo *Testing*, o refresh token expira em poucos dias.
- Quem autorizar verá tela de aviso de app não verificado. Esperado, uma vez só.
- A conta Gmail vira **ponto único de falha** — deve ser institucional, com recuperação configurada e credenciais em cofre compartilhado.
- O refresh token passa a ser o segredo mais crítico do sistema (RNF-21).

---

## D-02 — Lançamento não precisa ser feito no local do atendimento

**Status:** Aceita · 13/08/2026

**Contexto.** VRPs ficam em casas de máquinas, subsolos e shafts, onde raramente há sinal de celular. A dúvida era se o preenchimento offline seria requisito de MVP.

**Decisão.** O técnico **não** preenche dentro da casa de máquinas. Lança depois, em momento propício e com internet estável.

**Consequências.**
- **Operação offline sai inteiramente do escopo** (RNF-10). Elimina sincronização local, fila de envio e resolução de conflito — economia significativa de complexidade.
- Fotos são tiradas no local e anexadas depois → o anexo **precisa aceitar galeria**, não só câmera ao vivo (RF-19).
- `data_realizada` vira campo **obrigatório e explícito** (RF-17). Se o sistema assumisse "hoje", um atendimento lançado com 10 dias de atraso empurraria o retorno em 10 dias — acumulando desvio a cada ciclo.
- **Novo risco:** o lançamento pode simplesmente não acontecer. "Depois eu lanço" é onde a informação morre. Mitigado por D-16, pelo PDF como contrapartida (RF-48) e por RF-22.

---

## D-03 — Antecedência do alerta: 7 dias

**Status:** ⚠️ **Substituída por D-08** · 13/08/2026

Decisão original: criar o evento 7 dias antes do vencimento. Substituída após a leitura da planilha de controle da Manutec, que já definia uma janela de 30 dias.

---

## D-04 — Fila de oportunidades sem dono fixo

**Status:** Aceita · 13/08/2026

**Contexto.** Alguém precisa fazer o contato de reventa. As opções eram: pessoa fixa, o técnico que atendeu, ou fila aberta.

**Decisão.** A oportunidade nasce **sem responsável**. Qualquer um dos 3 usuários pode assumir; o nome de quem assumiu fica visível.

**Alternativas descartadas.** Dono fixo (mais simples, mas concentra em uma pessoa) e técnico que atendeu (usa a relação com o síndico, mas amarra o comercial à escala de campo).

**Consequências.**
- `oportunidade.responsavel_id` é **nulo** até alguém assumir.
- **Risco:** oportunidade de todos vira oportunidade de ninguém. Por isso RF-43 (destacar o que está vencendo sem dono) passa a ser praticamente obrigatório, não desejável.

---

## D-05 — Recusa do cliente: o sistema pergunta o que fazer

**Status:** Aceita · 13/08/2026

**Contexto.** Se o condomínio recusa a manutenção, é preciso decidir se o ciclo continua.

**Decisão.** Ao marcar `recusado`, o sistema **pergunta ao usuário** se deve voltar a lembrar e em qual data. Se sim, cria nova oportunidade; se não, encerra até reativação manual (RN-09).

**Alternativas descartadas.** Reagendar automaticamente para +12 ou +6 meses — regra fixa não cobre a variedade de motivos de recusa.

**Consequências.** Um passo a mais na interface, em troca de não perder cliente por regra automática inadequada. RF-45 (motivo da recusa) vira informação comercial útil depois de um ano.

---

## D-06 — A base histórica existe e será usada

**Status:** Aceita · 13/08/2026

**Contexto.** Sem base histórica, o pipeline nasceria vazio e só geraria receita em 12 meses.

**Decisão.** Usar as duas planilhas fornecidas pelo Daniel:
- `Controle_Manutencao_VRP_Final.xlsx` — modelo de dados desejado, dados fictícios. **Serve de especificação.**
- `RELATORIO NF MANUTENCAO VALVULAS - CONSOLIDADO.xlsx` — 595 NFs reais, 296 clientes, 2018–2026. **Serve de carga inicial.**

**Consequências.** O retorno financeiro do projeto deixa de ser "daqui a 12 meses" e passa a ser imediato: 153 clientes críticos e 94 recorrentes sem contato entram no painel no primeiro dia. Ver D-09 para o escopo da importação.

---

## D-07 — Estação redutora é um conjunto de VRPs, e vira entidade própria

**Status:** Aceita · 13/08/2026

**Contexto.** A análise dos dados reais mostrou que **343 das 595 NFs são de "Estação redutora"** e apenas 252 de "Válvula redutora". A estação é a maior parte do faturamento, e o modelo inicial não a contemplava.

**Decisão.** Estação redutora é um conjunto que contém várias VRPs. Vira entidade `estacao`, entre `cliente` e `valvula`.

**Evidência independente na planilha.** As válvulas do mesmo ponto repetem `localizacao_instalacao`, `andar_inicial`, `andar_final` e `requer_fechamento_geral`, diferindo apenas em `numero_valvula` — e foram atendidas na mesma data pelo mesmo técnico. A planilha já modelava estações sem nomeá-las.

**Consequências.** Elimina repetição de colunas, reflete a unidade de faturamento e viabiliza D-16.

**Pendente (P-01).** Quantas válvulas cabem numa estação, quais outros componentes ela inclui, e se a periodicidade é da válvula ou da estação.

---

## D-08 — Dois alertas: 30 dias e 7 dias

**Status:** Aceita · 13/08/2026 · *substitui D-03*

**Contexto.** D-03 definia 7 dias de antecedência. A planilha de controle da Manutec, escrita depois, define 30 dias como parâmetro de alerta. Conflito explícito, levado ao Daniel.

**Decisão.** **Dois eventos por ciclo:** 30 dias antes (contato comercial) e 7 dias antes (lembrete final).

**Racional.** 30 dias é o tempo realista para orçar, apresentar e aprovar — especialmente em condomínio, onde a decisão pode depender de administradora ou assembleia. 7 dias é a rede de segurança.

**Consequências.**
- `oportunidade` guarda `data_alerta_30`, `data_alerta_7`, `google_event_id_30` e `google_event_id_7`.
- O worker do Outbox cria **dois** eventos por oportunidade.
- Ambas as janelas configuráveis (RF-28) — a planilha já tratava isso como parâmetro editável.

---

## D-09 — Importar apenas clientes e data da última manutenção

**Status:** Aceita · 13/08/2026

**Contexto.** As 595 NFs reais contêm apenas cliente, data e tipo de serviço. **Não há informação de qual válvula foi atendida** — essa dimensão simplesmente não existe na base.

**Decisão.** Importar os **296 clientes** com a data da última manutenção. **Não** importar as 595 intervenções individuais.

**Alternativas descartadas.**
- Importar tudo no nível do cliente: traria histórico intermediário sem uso prático.
- Criar uma válvula genérica "a identificar" por cliente: manteria o modelo uniforme, mas poluiria o cadastro com 296 registros falsos que alguém teria que limpar depois.

**Consequências.**
- `oportunidade` ganha o campo `escopo` (`valvula` | `cliente`) para acomodar retornos sem válvula vinculada.
- Clientes importados ficam com `cadastro_incompleto = true`. O cadastro se completa na próxima visita — quando o técnico está na frente do equipamento, e não preenchendo de memória.
- Nenhum evento retroativo é criado no Calendar (RN-13). 205 eventos no passado inutilizariam a agenda; esses clientes viram lista de ação imediata no painel.
- As datas importadas são **aproximadas** (metadado de arquivo, não emissão da NF) → `data_legado_aproximada`.

---

## D-10 — Pressão de entrada e saída são campos opcionais

**Status:** Aceita · 13/08/2026

**Contexto.** Pressão de entrada/saída foi sugerida como dado técnico central de VRP, mas a planilha de histórico da Manutec **não a registra** — só técnico, tipo, peças e observações.

**Decisão.** Incluir como campos **opcionais** no registro de serviço.

> *Atualização:* à época a decisão apontava para a tabela `servico_valvula`. Com a D-18 essa tabela deixou de existir e os campos passaram para `servico`. A decisão em si — pressões opcionais — permanece válida.

**Consequências.** Disponível para quem quiser registrar, sem alongar o preenchimento obrigatório (RNF-03). Se a prática se consolidar, pode virar obrigatório numa decisão futura.

---

## D-11 — Monolito modular

**Status:** Aceita · 13/08/2026

**Contexto.** Um desenvolvedor, três usuários, ~100 registros por ano.

**Decisão.** Um único projeto, organizado em módulos com fronteiras explícitas. Sem microsserviços, filas dedicadas, cache distribuído ou orquestração de containers.

**Racional.** Nesse porte, arquitetura distribuída não resolve problema nenhum e adiciona operação que ninguém tem tempo de manter. As fronteiras modulares preservam a opção de extrair partes no futuro, se algum dia isso se justificar.

**Consequências.** Ver os anti-padrões em [architecture.md](architecture.md) §8. Agente que propuser separação em serviços precisa apresentar o problema medido que a motiva.

---

## D-12 — Stack: TypeScript, Next.js, PostgreSQL, Prisma, Supabase

**Status:** ⚠️ **Substituída por D-20** · 13/08/2026

A recomendação original otimizava para *manutenção por uma pessoa*. Surgiu depois um objetivo que não estava na mesa: o projeto é também o portfólio de entrada do Daniel na área de tecnologia. Isso muda a função-objetivo e, com ela, a stack. Raciocínio original preservado abaixo.

---

### *(conteúdo original da decisão substituída)*

**Contexto.** O Daniel constrói o sistema com apoio de IA. Manutenção por uma pessoa, orçamento enxuto, uso em celular e PC.

**Decisão.** TypeScript estrito · Next.js (App Router) · PostgreSQL via Supabase · Prisma · Tailwind + shadcn/ui · @react-pdf/renderer · googleapis.

**Racional.** Uma só linguagem no front e no back (RNF-24). Supabase entrega banco, storage, autenticação e backup num pacote, eliminando três decisões de infraestrutura — e como é Postgres puro, não há lock-in (RNF-27).

**Alternativas descartadas.** Detalhadas em [architecture.md](architecture.md) §7 — incluindo **manter tudo na planilha**, que é o concorrente mais sério e merece resposta honesta.

---

## D-13 — Padrão Outbox para o Google Calendar

**Status:** Aceita · 13/08/2026

**Contexto.** Se o sistema chamasse a API do Google dentro da requisição de salvamento, uma indisponibilidade do Google faria o técnico ver erro em campo — com risco de perder o registro, que é o dado mais valioso do sistema.

**Decisão.** Gravar `servico` e `oportunidade` numa transação, com os IDs de evento nulos. Um worker em segundo plano cria os eventos depois e preenche os IDs.

**Consequências.**
- **Falha do Google nunca causa perda de registro** (RNF-14).
- O sistema continua funcionando se o Calendar sair do ar ou for descontinuado — a fonte da verdade é o banco.
- Exige worker idempotente, retentativa espaçada, limite de tentativas e alerta em caso de token revogado.
- Exige visibilidade das pendências de sincronização (RF-33).

---

## D-14 — Idioma: domínio em português, infraestrutura em inglês

**Status:** Aceita · 13/08/2026

**Contexto.** Definir o idioma de tabelas, colunas e identificadores de código. Regra permanente — mudar depois é retrabalho generalizado.

**Decisão.**

| Camada | Idioma |
|---|---|
| Tabelas, colunas, enums | Português |
| Entidades e casos de uso | Português |
| Infra, utilitários, componentes genéricos | Inglês |
| Commits e branches | Inglês |

**Racional.** É o princípio de *ubiquitous language* do DDD: os termos do negócio permanecem na língua em que o negócio é falado. Decisivo aqui é que os termos **não sobrevivem à tradução** — `sindico` não é *manager* nem *trustee*, é um papel jurídico brasileiro específico; `barrilete` e `shaft hidráulico` não têm equivalente útil. Traduzir criaria uma camada permanente de tradução mental entre o que a equipe fala e o que o código diz, que é justamente onde nascem erros de regra de negócio.

**Consequências.**
- RNF-28 foi ajustado de "nomes idênticos à planilha" para "mesmo **vocabulário** da planilha". Fidelidade de conceito, não de string: a planilha usa `id_equipamento` como chave; o banco usa `valvula.id`, seguindo convenção.
- O mapeamento planilha → banco fica documentado em [database.md](database.md#importação-da-base-legada).

---

## D-15 — Status do painel é calculado, não materializado

**Status:** Aceita · 13/08/2026

**Contexto.** `dias_restantes` e `status` mudam sozinhos com a passagem do tempo, sem nenhum evento no sistema.

**Decisão.** Calcular na consulta, em tempo real. Não persistir.

**Racional.** Com ~1.200 válvulas o custo é irrelevante, e materializar criaria uma classe inteira de bug — painel desatualizado, que é falha silenciosa e de difícil detecção.

**Consequências.** Materializar só mediante problema de desempenho **medido**. Ver [architecture.md](architecture.md) §8.

---

## D-16 — Serviço é lançado por estação, não por válvula

**Status:** ⚠️ **Rejeitada — substituída por D-18** · 13/08/2026

Proposta apresentada e recusada pelo Daniel: o registro será mantido no nível da válvula individual, mesmo quando o serviço for executado na estação inteira. Registro do raciocínio preservado abaixo.

---

### *(conteúdo original da proposta rejeitada)*

**Contexto.** A planilha registra uma linha de histórico por válvula. Mas os dados mostram que as válvulas de uma mesma estação são sempre atendidas juntas, na mesma data e pelo mesmo técnico. Com lançamento por válvula, um prédio com 8 válvulas em 4 estações exigiria **8 preenchimentos para 4 visitas**.

**Decisão proposta.** Um `servico` por estação/visita, com marcação de quais válvulas foram atendidas (`servico_valvula`) e detalhe opcional por válvula.

**Racional.** Corta o esforço de lançamento pela metade ou mais — e o esforço de lançamento é o **maior risco de adoção** do projeto, agravado por D-02. Também espelha o negócio: a empresa fatura a estação, não a válvula, o que explica o predomínio de "Estação redutora" nas NFs.

**Condição de reversão.** Se na prática as válvulas de uma estação forem atendidas em datas diferentes com frequência, esta decisão cai e volta-se ao lançamento por válvula.

---

## D-18 — O serviço é registrado por válvula individual

**Status:** Aceita · 13/08/2026 · *substitui D-16*

**Contexto.** A D-16 propunha um registro por estação/visita, para reduzir o esforço de lançamento. O Daniel recusou: **as válvulas são tratadas individualmente, mesmo quando o serviço é executado na estação inteira.**

**Decisão.** Cada válvula atendida gera **um registro de `servico` próprio**, com sua data, tipo, peças substituídas, pressões e observações. A tabela `servico_valvula` deixa de existir; seus campos passam para `servico`.

**Racional.** O histórico técnico pertence ao equipamento, não ao ponto de instalação. Duas válvulas do mesmo barrilete podem ter idades, marcas, números de série e desgastes diferentes — e é o histórico individual que sustenta a decisão de reparar ou substituir. Agregar por estação perderia essa granularidade de forma irrecuperável.

**Consequências.**
- `servico.valvula_id` substitui `servico.estacao_id`.
- `oportunidade` de escopo válvula continua sendo a regra para registros novos; o escopo cliente permanece apenas para os importados (D-09).
- **Custo assumido:** mais preenchimentos por visita. Uma estação com 3 válvulas gera 3 lançamentos. Isso agrava o risco de adoção já criado pela D-02.
- **Mitigação necessária, não opcional:** a interface deve permitir preencher uma vez e **replicar para as demais válvulas da estação**, ajustando apenas o que difere. Isso é conveniência de tela — o modelo de dados permanece individual, e nenhum registro é derivado ou compartilhado.
- `estacao` continua existindo (D-07): agrupa as válvulas, carrega `requer_fechamento_geral` e a faixa de andares, e é a unidade faturada.

---

## D-19 — Uma estação comporta N válvulas, não apenas duas

**Status:** Aceita · 13/08/2026 · *resolve parte de P-01*

**Contexto.** A planilha da Manutec restringe `numero_valvula` aos valores `01` e `02`. Perguntado, o Daniel confirmou: **geralmente são 2, mas pode ser mais.**

**Decisão.** `numero_valvula` deixa de ser enum de dois valores e passa a ser um **sequencial dentro da estação** (`01`, `02`, `03`, …), sem limite fixo.

**Consequências.**
- A regra de ordenação da planilha continua válida e vira convenção de cadastro: **01** é a da esquerda quando em paralelo, ou a de cima quando sobrepostas; as demais seguem na mesma direção de leitura.
- A unicidade (`estacao_id`, `numero_valvula`) permanece e continua sustentando o alerta de duplicidade (RF-13).
- Nenhuma tela deve assumir "no máximo duas válvulas" — listas e formulários precisam ser genéricos.

---

## D-17 — Modelo Cliente → Estação → Válvula; remoção de "Local"

**Status:** Aceita · 13/08/2026

**Contexto.** O modelo inicial propunha `Cliente → Local → Válvula`, supondo que um cliente pudesse ter vários prédios.

**Decisão.** Remover `Local`. Adicionar `Estacao` (D-07) e `Especificacao` como catálogo. Hierarquia final: `cliente → estacao → valvula → servico`.

**Racional.**
- A planilha mostra que **cliente e condomínio são a mesma coisa**: `nome_condominio`, `endereco`, `bairro` e `cidade` estão todos na aba `clientes`, uma linha por prédio. `Local` seria uma camada vazia.
- `Especificacao` como catálogo reutilizável (marca + modelo + diâmetro + tipo de registro) evita que "Bermad 720" e "bermad 720" virem equipamentos diferentes.

**Consequências.** Modelo detalhado em [database.md](database.md). Se um dia um cliente tiver de fato dois prédios, a solução é cadastrá-lo duas vezes ou reintroduzir a camada — decisão a ser tomada quando o caso aparecer, não antes.

---

## D-20 — Stack: Java + Spring Boot no backend, React no frontend

**Status:** Aceita · 13/08/2026 · *substitui D-12*

**Contexto.** A D-12 escolheu TypeScript/Next.js otimizando para **manutenção por uma pessoa**: uma linguagem só, menos superfície. Faltava uma informação decisiva, que só apareceu depois: **o Valtis é também o projeto de portfólio do Daniel para entrar na área de tecnologia**, e ele já está estudando Java e Spring.

Isso troca a função-objetivo. Não se trata mais de "qual stack dá menos trabalho para manter", e sim de "qual stack entrega o sistema **e** constrói a carreira". Mantido o TypeScript, ele precisaria de um segundo projeto paralelo em Java — e já existe um terceiro projeto em Salesforce Apex/LWC concorrendo pelo mesmo tempo.

**Decisão.**

| Camada | Escolha |
|---|---|
| Backend | **Java 21 (LTS) + Spring Boot 3** |
| Build | **Maven** — mais comum em vagas e em código legado que em Gradle |
| Persistência | **Spring Data JPA / Hibernate** |
| Migrações | **Flyway** — versionadas, alinhadas ao AGENTS.md §10 |
| Banco | **PostgreSQL** (inalterado) |
| Segurança | **Spring Security** |
| Agendador do Outbox | **`@Scheduled` do Spring** — sem infraestrutura extra |
| Google Calendar | SDK oficial `google-api-client` para Java |
| PDF | **JasperReports** — padrão de mercado em Java corporativo, e habilidade valorizada em vaga. *Atenção à licença de alternativas: o iText 7 é AGPL, restritivo para uso comercial* |
| Testes | **JUnit 5 + Mockito + Testcontainers** |
| Frontend | **React + Vite + TypeScript** |
| Deploy backend | Railway ou Render (container). **Vercel não hospeda Java** |
| Deploy frontend | Vercel ou Netlify (estático) |

**Racional da escolha de React sobre Angular.** Angular é a dupla clássica do Java corporativo brasileiro, mas tem curva mais íngreme — e o orçamento é de 10h por semana, divididas entre aprender e construir. React tem mais vagas no agregado e curva mais suave. Thymeleaf foi descartado: entregaria mais rápido, mas não sustentaria a alegação de full stack, que é parte do objetivo.

**Consequências.**
- **RNF-24 fica revogado.** Passam a ser duas linguagens e dois deploys. É um custo real, assumido conscientemente em troca do objetivo de carreira.
- O backend deixa de servir telas: vira **API REST** consumida pelo React.
- O padrão Outbox (D-13) **permanece** e fica até mais natural — `@Transactional` e `@Scheduled` são nativos do Spring.
- O monolito modular (D-11) permanece. Vale considerar **Spring Modulith** para tornar as fronteiras entre módulos verificáveis por teste.
- Aumenta o prazo de entrega e a curva de aprendizado. Aceito.

---

## Índice

| ID | Decisão | Status |
|---|---|---|
| D-01 | OAuth de conta Gmail para o Calendar | Aceita |
| D-02 | Lançamento não é feito no local | Aceita |
| D-03 | Antecedência de 7 dias | Substituída por D-08 |
| D-04 | Fila de oportunidades sem dono | Aceita |
| D-05 | Recusa: o sistema pergunta | Aceita |
| D-06 | Usar a base histórica | Aceita |
| D-07 | Estação redutora como entidade | Aceita |
| D-08 | Dois alertas: 30 e 7 dias | Aceita |
| D-09 | Importar só clientes + última manutenção | Aceita |
| D-10 | Pressões opcionais | Aceita |
| D-11 | Monolito modular | Aceita |
| D-12 | Stack TypeScript / Next / Postgres | **Substituída por D-20** |
| D-13 | Padrão Outbox | Aceita |
| D-14 | Domínio em PT, infra em EN | Aceita |
| D-15 | Painel calculado | Aceita |
| D-16 | Lançamento por estação | **Rejeitada — ver D-18** |
| D-17 | Cliente → Estação → Válvula | Aceita |
| D-18 | Serviço registrado por válvula individual | Aceita |
| D-19 | Estação comporta N válvulas | Aceita |
| D-20 | Stack Java + Spring Boot + React | Aceita |
