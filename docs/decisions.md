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
- **Revisto em 16/10/2026 (Daniel, após o teste manual):** a tela passa a **sugerir** hoje, porque digitar a data toda vez pesa no lançamento mais comum (o do mesmo dia). O risco acima continua; a mitigação é o aviso "Confira: é hoje?" ao lado do campo. O servidor segue exigindo a data no pedido.
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

## D-21 — O Outbox é uma tabela própria: `evento_agenda`

**Status:** Aceita · 13/10/2026 · validada pelo Daniel em 16/10/2026 · *refina D-13*

**Contexto.** O modelo original (D-08, database.md) guardava o estado da sincronização em colunas da própria `oportunidade`: `google_event_id_30`, `google_event_id_7`, `tentativas_sync`, `sincronizado_em`. Na implementação, três problemas apareceram: os dois alertas falham e são retentados de forma independente, mas dividiriam um único contador de tentativas; as janelas são configuráveis (RF-28), e colunas com "30" e "7" no nome mentiriam assim que a configuração mudasse; e não havia onde guardar a última mensagem de erro, que o RF-33 precisa mostrar.

**Decisão.** Uma tabela `evento_agenda`, com uma linha por alerta: tipo, data, status (`pendente`, `criado`, `falhou`, `descartado`), tentativas, próxima tentativa, último erro e id do Google. A linha é gravada na mesma transação do serviço e da oportunidade. As datas de alerta deixam de ser colunas da oportunidade e passam a ser derivadas de `data_prevista` (como o status do painel, D-15).

**Idempotência.** O id do evento no Google é **decidido pelo Valtis antes da chamada**, a partir do id da linha. Se o Google criar o evento e a aplicação cair antes de gravar a resposta, a retentativa manda o mesmo id, o Google responde 409 e nada é duplicado.

**Alternativa descartada.** Manter as colunas na oportunidade: menos uma tabela, mas sem estado por alerta nem registro de erro.

**Consequências.**
- Alerta cuja data já passou não entra na fila (RN-13): evento no passado só polui a agenda. Quem fica sem lembrete é avisado pela fila de Oportunidades (`semLembreteNaAgenda`), decidido pelo Daniel em 16/10/2026.
- O worker reconfere, imediatamente antes de enviar, se o ciclo ainda está aberto e a válvula ativa; se não, descarta.
- `database.md` atualizado. Índice parcial `WHERE status = 'pendente'` para a varredura do worker.

---

## D-22 — Autenticação por token JWT, sem sessão no servidor

**Status:** Aceita, com gatilho de revisão · 13/10/2026 · validada pelo Daniel em 16/10/2026

**Contexto.** Frontend (Vercel) e backend (Railway) estão em domínios diferentes. Sessão por cookie entre domínios exige `SameSite=None` e esbarra no bloqueio de cookies de terceiros do Safari, comum em iPhone.

**Decisão.** O login devolve um JWT assinado com HS256 por uma chave em variável de ambiente (`VALTIS_JWT_SEGREDO`), válido por 14 dias (RF-02). O frontend o envia no cabeçalho `Authorization`. A biblioteca é a Nimbus, a mesma que o Spring Security já usa para validar tokens (starter `oauth2-resource-server`).

**Ponto de projeto.** A assinatura prova que o token é nosso, mas **perfil e status ativo são lidos do banco a cada requisição**. Inativar alguém corta o acesso na hora, sem esperar o token expirar. O custo é uma consulta por requisição, irrelevante para 3 usuários.

**Proteções incluídas.** Senha com BCrypt (RNF-20); bloqueio de 15 minutos após 5 senhas erradas para o mesmo e-mail; mesma mensagem e mesmo tempo de resposta para e-mail inexistente e senha errada.

**Alternativas descartadas.** Sessão com cookie (problema entre domínios, acima). Token opaco em tabela (exigiria mais uma tabela e consulta, sem ganho com o perfil já lido do banco).

**Consequências.**
- O token fica no `localStorage` do navegador. Um XSS conseguiria lê-lo; a defesa é não renderizar HTML vindo de usuário (o React escapa por padrão).
- Não há "sair de todos os dispositivos" além de inativar o usuário ou trocar a chave.
- Produção passa a exigir `VALTIS_JWT_SEGREDO`; sem ela a aplicação **não sobe**.

**Gatilho de revisão (Daniel, 16/10/2026).** O motivo central (domínios diferentes) deve cair: a tendência é servir frontend e backend num **domínio único** (P-07). Quando isso for decidido, reavaliar a troca para sessão em cookie `HttpOnly` + `SameSite=Strict`, que tira o token do alcance de um XSS. A troca é localizada: `SegurancaConfig` e o emissor de token no backend, `http.ts` e `sessao/` no frontend. Até lá, fica JWT.

---

## D-23 — Google Calendar pela API REST, sem o SDK

**Status:** Aceita · 13/10/2026 · validada pelo Daniel em 16/10/2026 · *ajusta a linha "Calendar" de D-20*

**Contexto.** D-20 previa o `google-api-client`. O Valtis usa duas chamadas: trocar o refresh token por um token de acesso e criar um evento.

**Decisão.** Chamar as duas URLs com o `RestClient` do Spring, atrás da interface `AgendaGateway`. Sem dependência nova (AGENTS.md §3: a resposta padrão para "instalar isso?" é não).

**Consequências.**
- Menos dependências transitivas, e o adaptador é testável com `MockRestServiceServer`.
- Se o uso da API crescer (editar ou remover eventos, RF-32), reavaliar o SDK.
- **Ainda não exercitado contra o Google real**: depende do app OAuth publicado em produção (D-01), que só o Daniel pode criar.

---

## D-24 — Frontend com CSS próprio, sem Tailwind e shadcn/ui por enquanto

**Status:** Aceita · 13/10/2026 · validada pelo Daniel em 16/10/2026 · *diverge de architecture.md §7*

**Contexto.** architecture.md previa Tailwind + shadcn/ui; a Fase 0 já saiu com CSS puro, registrado como dívida no HANDOFF.

**Decisão.** Manter CSS próprio, com variáveis de tema e componentes simples, e a única dependência nova de frontend sendo o roteador (`react-router`).

**Racional.** São poucas telas, e cada biblioteca de estilo é mais uma coisa para aprender ao mesmo tempo que Java e Spring (10h por semana). CSS puro é a base que Tailwind abstrai; trocar depois é viável tela a tela.

**Consequências.** Mais CSS escrito à mão. Se a interface crescer ou ganhar componentes complexos (tabelas com ordenação, seletores de data), reabrir com Tailwind.

---

## D-25 — Importador reconhece as colunas pelo cabeçalho, em duas etapas

**Status:** Aceita · 16/10/2026 · pedido do Daniel

**Contexto.** A planilha dos 296 clientes (D-09) ainda não chegou, e o formato dela é desconhecido. O Daniel quer um botão de importar dentro do sistema que aproveite o que servir e ignore o resto.

**Decisão.**
- O admin envia `.xlsx`, `.xls` ou `.csv`. O sistema **reconhece cada coluna pelo nome do cabeçalho**, sem acento e sem diferença de maiúsculas, a partir de uma lista de nomes aceitos por campo (`CampoImportavel`): "Cliente", "Condomínio", "Nome" → nome do condomínio; "Data NF", "Última manutenção", "Data" → última manutenção; e assim por diante. **Coluna que não corresponde a nenhum campo do cadastro é descartada**, e a tela mostra quais foram.
- **Duas etapas (RF-56).** A análise lê, valida e devolve a prévia sem gravar nada; a tela deixa corrigir nome e data e desmarcar linhas. A gravação valida tudo de novo no servidor e é **tudo ou nada**: uma linha com erro barra o lote.
- Erro (barra a linha): sem nome, data inválida ou no futuro. Aviso (não barra): sem data (entra no cadastro, fora da fila), já existe cliente com o mesmo nome ou nome repetido no arquivo (RF-57, vêm desmarcadas), e-mail inválido (descartado), atendimento não reconhecido (vira Avulso).
- Cliente importado: `origem = importacao`, `cadastro_incompleto`, data marcada como aproximada (RF-58). Com data, ganha oportunidade de escopo cliente com vencimento em data + 12 meses (RF-54); alertas no passado não vão para a agenda (RN-13).
- **Apache POI** para ler Excel. Dependência nova, justificada: obrigar a converter para CSV antes é um passo a mais em que se erra o encoding ou o separador. O CSV é lido sem biblioteca (UTF-8 ou Windows-1252, `;` ou `,`).

**Alternativas descartadas.** Esperar a planilha e moldar o importador ao formato dela: trava o projeto e quebra na próxima planilha. Mapear colunas manualmente na tela: mais flexível, mas é mais um passo para quem importa; fica como evolução se o reconhecimento automático errar com frequência.

**Consequências.**
- Rodar o importador sobre a **base real em produção** continua exigindo aprovação explícita (AGENTS.md §10).
- Cabeçalho fora da lista é ignorado em silêncio, salvo pela lista de descartadas na prévia. Ao receber a planilha real, conferir a prévia antes de confirmar e, se preciso, acrescentar o nome à lista de `CampoImportavel`.

---

## D-26 — Bombas são um módulo próprio, espelhando o modelo das válvulas

**Status:** Aceita · 20/10/2026 · aprovada pelo Daniel no mesmo dia

**Contexto.** O Daniel pediu um módulo de bombas (recalque, incêndio, drenagem…) com a mesma lógica de cadastro técnico, histórico e recorrência do módulo de válvulas, mais o controle de bombas patrimoniais da Manutec emprestadas a condomínios. O módulo de válvulas já está validado e não pode ser perturbado.

**Decisão.** Um módulo `bomba` novo, com a hierarquia `cliente → conjunto_bombeamento → bomba`, paralela a `cliente → estacao → valvula`. O cálculo de vencimento e a classificação do painel reutilizam `RegrasRecorrencia` (RN-16 chama o mesmo método de RN-01). Nada em `estacao`, `valvula`, `servico` ou `oportunidade` muda.

**Alternativas descartadas.**
- **Generalizar válvula e bomba num supertipo `equipamento`** (tabela única com `tipo`, ou herança JPA). É a "solução elegante", mas é abstração antes da terceira repetição (architecture.md §8): são dois equipamentos com campos, numeração e regras diferentes (a bomba se move; a válvula não). Além disso, obrigaria a migrar `valvula`, `servico` e `oportunidade`, que estão validados em uso. Se um terceiro tipo de equipamento aparecer, reavaliar.
- **Tratar bomba como válvula de outro tipo** (`especificacao.tipo = bomba`). Esconde o domínio: potência, tensão, corrente e empréstimo não cabem numa VRP.

**Consequências.**
- Código duplicado de forma consciente entre `estacao`/`valvula` e `conjunto`/`bomba` (serviço, controller, tela). É o custo da D-11 e do §8; aceito.
- Painel de bombas separado do painel de válvulas, no MVP 1. Unificar fica para quando a tela existir e o Daniel vir as duas.
- Serviço por bomba e oportunidade por bomba **não** entram no MVP 1 (P-14, P-16): a bomba guarda `ultima_manutencao` no cadastro.
- Modelo em [database.md](database.md#módulo-de-bombas); requisitos RF-60 a RF-69; regras RN-15 a RN-20.

---

## D-27 — A posição de uma bomba é derivada da ocupação aberta, não gravada na bomba

**Status:** Aceita · 20/10/2026 · aprovada pelo Daniel no mesmo dia

**Contexto.** A bomba se move: a patrimonial da Manutec é emprestada e devolvida; a do condomínio sai para conserto e volta. Precisamos saber onde cada bomba está, quando esteve em cada lugar e quem ela substituiu — e o briefing exige que a bomba original possa ser reinstalada **antes** de a emprestada ser retirada.

**Decisão.** A bomba não tem coluna "conjunto atual". Existe a tabela `ocupacao_bomba`: uma linha por período em que uma bomba ocupou uma posição (`numero_posicao`) de um conjunto, com início, fim (nulo enquanto aberta), natureza (`instalacao` | `emprestimo`), a bomba substituída e quem registrou. **Onde a bomba está** e **se a patrimonial está disponível** (RN-17) são calculados a partir das ocupações abertas — como o status do painel (D-15). Uma posição **pode** ter duas ocupações abertas ao mesmo tempo (RN-19); o sistema avisa, não bloqueia.

**Alternativas descartadas.**
- `bomba.conjunto_id` mutável mais uma tabela de log de movimentações. Duas verdades: o log fica inconsistente no primeiro erro de registro, e "reinstalar a original antes de retirar a emprestada" não cabe num ponteiro único.
- Restrição de unicidade "uma ocupação aberta por posição". Simples, mas proíbe exatamente o caso que o briefing exige.

**Consequências.**
- "Número da bomba" é o número da posição que ela ocupa agora; a identidade permanente é o `codigo_referencia` (RN-15).
- A ocupação é histórico (RN-20): nunca se edita a data retroativamente; corrige-se encerrando e abrindo outra.
- Consulta de "onde está" é uma leitura de ocupações abertas; com ~centenas de bombas, custo irrelevante (RNF-12).

---

## D-28 — Sem conectores nem interfaces para Jiraya e Manutec_clientes por enquanto

**Status:** **Substituída por D-29 e D-30** · 30/10/2026. Valeu de 20/10 a 30/10/2026; o padrão de entrada que ela antecipou (recebimento idempotente processado por worker) foi mantido na D-30

**Contexto.** O briefing cita duas integrações futuras: Manutec_clientes como fonte de condomínios e contatos, e Jiraya como sistema de OS que, após validação do gestor, atualizaria o histórico do Valtis. Pede para "preparar interfaces desacopladas quando fizer sentido", sem implementar conectores.

**Decisão.** Não criar interface, adaptador vazio nem campo "id externo" agora. Quando os contratos existirem (P-18), o padrão já está decidido: entrada pelo mesmo mecanismo do Outbox (D-13, D-21) invertido — uma tabela de recebimento com o id do evento externo como chave de idempotência, processada por worker, com retentativa e erro visível.

**Alternativa descartada.** Interfaces "preparatórias" com um único implementador fictício. É abstração sem segundo caso de uso (architecture.md §8) e costuma errar o formato do contrato real.

**Consequências.** `cliente` continua sendo cadastrado no Valtis. Quando a Manutec_clientes virar fonte, a decisão de quem é dono do cadastro vira uma D-xx própria, com aprovação.

---

## D-29 — O Valtis é o monitor de prazos; cliente, equipamento e serviço podem vir de fora (modo de operação)

**Status:** Aceita · 30/10/2026 · aprovada pelo Daniel no mesmo dia. O schema (`V6`) ainda será apresentado à parte antes de rodar (AGENTS.md §10)

**Contexto.** A Manutec passou a ter três sistemas em construção, e o Daniel definiu o papel de cada um:

| Sistema | Papel | O que já tem (conferido em 30/10/2026) |
|---|---|---|
| **Manutec Clientes** (Django) | Fonte única de clientes, administradoras e contatos | API de leitura `api/integracao/v1/clientes/` com busca, detalhe e `alteracoes/` com cursor; token Bearer de serviço |
| **Jiraya** (Spring) | Mestre operacional: OS, equipes, agenda, preventivas, orçamento, estoque | Clientes espelhados do Manutec Clientes (V19); **equipamentos por cliente** (E4, V16, tipos "Bomba de recalque" e "VRP", substituição com histórico); OS ligada a equipamento com `completed_at` (E5, V21); planos preventivos (E6, V24). **Não expõe API** para terceiros |
| **Valtis** | Monitor de prazos e apontador de oportunidades de revenda da manutenção de 12 meses, para VRPs e bombas | Recorrência, oportunidade, fila, agenda Google, painel; e cadastros próprios, hoje redundantes com os dois acima |

Cadastrar cliente e equipamento no Valtis e lançar serviço nele seria retrabalho: tudo isso já acontece, ou vai acontecer, no Jiraya. Ao mesmo tempo o Daniel quer **manter tudo o que o Valtis já tem**, porque pretende comercializá-lo depois como produto independente (SaaS), onde não haverá Jiraya nem Manutec Clientes.

**Decisão.**

1. O Valtis ganha uma configuração de **origem dos cadastros**, `valtis.cadastros.origem`, com dois valores:
   - `propria` — como hoje. Cliente, estação, válvula, conjunto e bomba são cadastrados no Valtis; importador e movimentações de bombas ativos. É o modo do produto independente.
   - `externa` — o modo da Manutec. Cliente é espelho do Manutec Clientes; equipamento (VRP e bomba) é espelho do Jiraya; serviço nasce de OS concluída no Jiraya. As telas de cadastro viram consulta e dizem onde o dado é gerido. Importador, movimentações e patrimônio de bombas ficam desligados: vivem no Jiraya.
2. **As tabelas são as mesmas nos dois modos.** O que muda é **quem escreve** nelas: a tela (modo `propria`) ou o sincronizador (modo `externa`). Recorrência, oportunidade, agenda, painel e fila leem `cliente`, `valvula`, `bomba` e `servico` do mesmo jeito e **não sabem em que modo estão**.
3. As entidades espelhadas guardam a referência externa: `cliente.manutec_id`, `valvula.jiraya_equipamento_id`, `bomba.jiraya_equipamento_id`, `servico.jiraya_os_id`, mais `sincronizado_em`. Como o Jiraya é plano (cliente → endereço → equipamento com `internal_location`), **estação e conjunto são derivados**: uma estação por (cliente, localização interna) para VRPs, um conjunto por (cliente, localização interna) para bombas.
4. O **lançamento manual continua** no modo `externa`, como correção para quando a OS não chegou ou chegou errada. É marcado "lançado no Valtis" e **não volta** ao Jiraya: a integração é de mão única (RN-24).
5. O que faltar num evento recebido (equipamento de tipo não mapeado, cliente sem referência, data ausente) vai para uma tela de **pendências de integração**; nada é descartado em silêncio.

**Alternativas descartadas.**
- **Clonar o Valtis com outro nome para a Manutec** e manter o original para o SaaS. Dois códigos divergindo desde o primeiro dia; cada correção na recorrência, fila ou agenda feita duas vezes, com ~10h semanais. O motor é o mesmo nos dois usos; só muda a origem de cliente e equipamento. Quando o SaaS virar prioridade, um fork a partir de uma base madura leva um minuto e vai divergir de qualquer forma (multi-cliente, cobrança, marca).
- **Interface `FonteDeCadastro` com dois implementadores** (banco próprio × APIs externas), lida em tempo real pelos casos de uso. Abstração sem necessidade: as tabelas são as mesmas, só o escritor muda. Além disso faria o painel depender da disponibilidade do Jiraya a cada consulta.
- **Ler o banco do Jiraya diretamente.** Acopla o Valtis ao schema interno de outro sistema e viola a fronteira da architecture.md §2. A leitura é por API, com contrato.
- **Mover a recorrência e a fila para dentro do Jiraya.** O Jiraya já tem preventivas, mas elas são agenda operacional da equipe elétrica. A fila comercial de bombas e VRPs é outra coisa, e o Daniel quer o Valtis como produto separado.

**Consequências.**
- D-28 substituída. RF-69 substituído pela seção 1.10 dos requisitos (RF-70 a RF-79); regras novas RN-21 a RN-24.
- **Alteração de schema** (colunas de referência externa, tabelas de recebimento e cursor): exige aprovação e migração `V6` própria.
- No modo `externa` o módulo de bombas se reduz a monitorar: RN-16 e, agora, oportunidade e agenda (D-31). Conjunto, ocupação e patrimônio emprestado continuam existindo no código, ativos só em `propria`. O **patrimônio emprestado precisa nascer no Jiraya** (trabalho naquele repositório, fora do Valtis).
- A importação dos 296 clientes (D-09) deixa de ser caminho da Manutec: a base histórica entra pelo Manutec Clientes (clientes) e pelo Jiraya (equipamentos e OS). O importador continua para o modo `propria`. Ver P-25.
- A Manutec sobe em `externa` desde o primeiro deploy. Até o Jiraya expor a API (D-30), o Valtis não tem dado em produção: a ordem de implantação passa a depender do Jiraya.

---

## D-30 — Entrada de dados externos por leitura com cursor, idempotente

**Status:** Aceita · 30/10/2026 · escolhida e aprovada pelo Daniel no mesmo dia. Intervalo de leitura: 5 minutos, configurável (P-23)

**Contexto.** O Valtis precisa saber de três coisas que acontecem fora dele: cliente criado ou alterado (Manutec Clientes), equipamento criado, alterado ou substituído (Jiraya) e OS de manutenção concluída (Jiraya). O Manutec Clientes já oferece `alteracoes/` com cursor, e o Jiraya já consome esse padrão (V19, `manutec_sync_cursor`). O Jiraya não expõe nada ainda.

**Decisão.**
- O Valtis **puxa**: um job por origem, a cada intervalo configurável (proposta: 5 minutos), chama `alteracoes?desde=<cursor>` e avança o cursor só depois de gravar o lote. Autenticação por token Bearer de serviço, um por origem, em variável de ambiente (`VALTIS_MANUTEC_CLIENTES_URL/TOKEN`, `VALTIS_JIRAYA_URL/TOKEN`).
- Cada item recebido é gravado **antes de ser aplicado** numa tabela de recebimento (`evento_recebido`: origem, tipo, id externo, versão ou `atualizado_em`, corpo, estado, tentativas, erro), com índice único em (origem, tipo, id externo, versão). É o Outbox (D-13, D-21) invertido: o mesmo worker, a mesma retentativa, o mesmo erro visível. Reprocessar não duplica serviço nem oportunidade (RN-23).
- O Jiraya precisa ganhar uma API de leitura para o Valtis, no molde do `INTEGRACAO_JIRAYA.md` do Manutec Clientes: equipamentos (com tipo, cliente `manutec_id`, localização interna, substituição) e OS concluídas (com equipamentos, data de conclusão, quem concluiu, **tipo de serviço**). O contrato é escrito no Valtis, em `docs/integracao/jiraya.md`, e implementado no repositório do Jiraya.

**Alternativas descartadas.**
- **Webhook** (Jiraya chama o Valtis ao concluir a OS). Imediato, mas exige fila e retentativa dos dois lados, um endpoint público no Valtis e tratamento de evento perdido. Para um ciclo de 12 meses, latência de 5 minutos é irrelevante.
- **Os dois.** Fica como evolução se um dia a latência importar.

**Consequências.**
- Latência igual ao intervalo de leitura. Sem Jiraya no ar, o Valtis continua funcionando com o que já espelhou: o painel não depende da outra API em tempo real.
- Tela de estado da integração (RF-78): cursor, última leitura, erros, "ler agora".
- Dependência externa nova para a Manutec: três itens no Jiraya (API de leitura, tipo de serviço na OS, patrimônio emprestado) e um token no Manutec Clientes. Ver P-24.

---

## D-31 — Toda manutenção concluída é oportunidade, inclusive sob contrato; só OS de manutenção reinicia o ciclo

**Status:** Aceita · 30/10/2026 · aprovada pelo Daniel no mesmo dia. Reiniciam o ciclo: manutenção preventiva, manutenção corretiva e instalação (P-20)

**Contexto.** O Jiraya tem planos preventivos que geram OS periódicas para clientes sob contrato. Parecia duplicidade com o Valtis. O Daniel esclareceu: **toda manutenção é oportunidade de receita**, porque as peças não são cobertas pelo contrato e têm margem; as preventivas do Jiraya são da equipe elétrica, e o Valtis é do pessoal de bombas e VRP. Uma bomba de condomínio com contrato e manutenção vencida precisa aparecer no Valtis do mesmo jeito. Quando o Jiraya registrar a manutenção, o Valtis recebe e o alerta some.

Por outro lado, a OS do Jiraya hoje **não tem tipo**: uma visita para orçar e uma troca de peças são a mesma coisa para o banco. Se qualquer OS concluída reiniciasse o ciclo, uma visita adiaria o alerta em 12 meses.

**Decisão.**
- O Valtis **não filtra por contrato nem por plano preventivo**. Coerente com RN-03: contrato é informação comercial, não altera prazo (RN-21).
- Só reinicia o ciclo a OS **concluída** cujo **tipo de serviço** seja manutenção (preventiva ou corretiva) e cujo equipamento seja de tipo mapeado (VRP → válvula; bomba → bomba). Instalação de equipamento novo também inicia o ciclo. Visita técnica, orçamento e outros tipos não reiniciam (RN-22). O campo "tipo de serviço" **não existe no Jiraya** e precisa ser criado lá.
- Bomba passa a gerar oportunidade e eventos de agenda como a VRP: responde P-14 (era "não gera no MVP 1").

**Alternativas descartadas.**
- **Ignorar equipamento em plano preventivo ativo.** Esconderia justamente a receita de peças que o Daniel quer ver.
- **Qualquer OS concluída reinicia o ciclo.** Zero mudança no Jiraya, mas visita de orçamento adiaria o alerta.
- **Toda OS chega como pendência para alguém confirmar.** Zero mudança no Jiraya, mas transforma a integração em trabalho manual diário.

**Consequências.**
- RN-21 e RN-22 novas; RN-07 e RN-10 passam a valer também para bomba.
- Serviço por bomba (`servico.bomba_id`, hoje `valvula_id` é obrigatório) precisa existir: responde P-16. Em `externa` vem do Jiraya; o lançamento manual por bomba entra junto.
- Um `tipo de serviço` novo na OS do Jiraya: trabalho naquele repositório.

---

## D-32 — RN-24 por campo pela rota de complemento, não pela edição comum

**Status:** Aceita · 01/11/2026 · decisão técnica tomada na implementação, com a autonomia dada pelo Daniel; reversível sem mudança de schema

**Contexto.** A RN-24, refinada pela Q-2 do V6, diz que no registro espelhado o campo da fonte é só leitura e o campo que só o Valtis tem é editável. As telas de edição de cliente, estação, válvula e bomba mandam o registro inteiro (`PUT`), misturando os dois tipos de campo.

**Decisão.**
- A edição comum (`PUT /api/{recurso}/{id}`), a inativação e a exclusão do registro espelhado são **recusadas com 409**, dizendo onde o dado é gerido. Vale em qualquer modo: o que define é o registro ser espelhado, não o modo.
- Cada recurso ganha uma rota de **complemento** (`PUT /api/{recurso}/{id}/complemento`) que só aceita os campos do Valtis: tipo de atendimento e contrato no cliente; andares e fechamento geral na estação; especificação, data de instalação e periodicidade na válvula; dados elétricos, periodicidade e última manutenção informada na bomba.
- Em `externa`, criar cliente, estação, válvula, conjunto, bomba e patrimônio, mexer no catálogo de tipos de bomba, movimentar bombas e importar planilha são recusados com 409 (RF-71, RF-77).

**Alternativas descartadas.**
- **Aceitar o `PUT` inteiro e ignorar os campos da fonte em silêncio.** A tela mostraria "salvo" e o valor voltaria na próxima leitura.
- **Aceitar o `PUT` e recusar só se um campo da fonte mudou.** Funciona, mas exige comparar campo a campo em cada serviço, e a regra de qual campo é de quem fica espalhada. A rota de complemento deixa a lista de campos do Valtis explícita, num lugar só por recurso.

**Consequências.**
- O frontend do modo `externa` usa a rota de complemento nas fichas espelhadas.
- Registros criados à mão antes de ligar o modo `externa` (sem referência externa) continuam editáveis pela tela comum.


---

## D-33 — Oportunidade que perde o serviço de origem é `cancelado`, não `perdido`

**Status:** Aceita · 01/11/2026 · escolhida pelo Daniel ao responder a P-32; schema `V7` aprovado por ele no mesmo dia ([proposta](integracao/schema-v7.md))

**Contexto.** A RN-26 permite ao admin inativar um serviço (OS reaberta ou cancelada no Jiraya, ou lançamento por engano). Quando o serviço é o único que dá data-base ao equipamento, o ciclo aberto perde a origem e não há data para recontar.

**Decisão.**
- Status novo **`cancelado`**, de encerramento: a oportunidade que nasceu do serviço é cancelada, o motivo fica nas notas e os lembretes pendentes saem da agenda. O equipamento volta a SEM REGISTRO; o próximo serviço abre um ciclo novo (RN-10).
- `cancelado` **não** é desfecho que o usuário escolhe na fila (RF-41 segue com os cinco de antes).
- Na bomba, a data de cadastro também é data-base (RF-80): havendo, o ciclo é recontado dela em vez de cancelado.
- O retorno pedido numa recusa (RN-09) não nasce do serviço e fica como está.

**Alternativas descartadas.**
- **Encerrar como `perdido`.** Contaria como receita perdida o que foi só um erro de registro.
- **Continuar recusando a inativação.** Obrigaria a inventar um serviço para corrigir um erro.

**Consequências.**
- Migração `V7__oportunidade_cancelada.sql` (só alarga o CHECK).
- Relatórios de receita perdida, quando existirem, contam só `perdido` (e, se for o caso, `recusado`), nunca `cancelado`.
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
| D-21 | Outbox em tabela própria (`evento_agenda`) | Aceita |
| D-22 | Autenticação por JWT, perfil lido do banco | Aceita (revisar com domínio único) |
| D-23 | Google Calendar pela API REST, sem SDK | Aceita |
| D-24 | CSS próprio, sem Tailwind por enquanto | Aceita |
| D-25 | Importador reconhece colunas pelo cabeçalho, em duas etapas | Aceita |
| D-26 | Bombas são módulo próprio, espelhando as válvulas | Aceita |
| D-27 | Posição da bomba derivada da ocupação aberta | Aceita |
| D-28 | Sem conectores nem interfaces de integração por enquanto | **Substituída por D-29 e D-30** |
| D-29 | Valtis como monitor de prazos; cadastros e serviços podem vir de fora (modo `propria` / `externa`) | Aceita |
| D-30 | Entrada externa por leitura com cursor, idempotente (`evento_recebido`) | Aceita |
| D-31 | Toda manutenção é oportunidade, inclusive sob contrato; só OS de manutenção reinicia o ciclo | Aceita |
| D-32 | RN-24 por campo pela rota de complemento; edição comum do espelhado recusada | Aceita |
| D-33 | Oportunidade sem serviço de origem é `cancelado`, não `perdido` | Aceita |
