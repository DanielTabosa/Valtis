# HANDOFF — Valtis

**Para:** qualquer agente que vá trabalhar neste repositório
**Atualizado em:** 06/10/2026
**Último commit:** `ae79829 docs: rotina de execucao, URLs de producao e problemas conhecidos`

Este arquivo é o resumo de situação. Ele **não substitui** `AGENTS.md`, `docs/requirements.md`, `docs/architecture.md`, `docs/database.md` e `docs/decisions.md` — leia-os conforme a ordem do `CLAUDE.md`. O que este arquivo faz é te dizer, rápido: onde o projeto está, o que já foi decidido, o que falta e o que você **não** pode fazer sozinho.

---

## 1. O produto em um parágrafo

A **Manutec Válvulas** (Recife/PE) faz manutenção de VRPs — válvulas redutoras de pressão — em prédios. Toda manutenção abre uma janela de reventa cerca de 12 meses depois. Hoje esse rastreamento é manual e falha. O **Valtis** registra as manutenções executadas, calcula o vencimento do próximo ciclo e agendará o retorno no Google Calendar. Três usuários: dois perfis, `tecnico` e `admin`.

**O tamanho da dor, medido na base real (595 NFs, 2018–ago/2026):**

| Métrica | Valor |
|---|---|
| Clientes distintos atendidos | 296 |
| Há 24+ meses sem manutenção (crítico) | 153 |
| Há 12+ meses sem manutenção | 52 |
| Recorrentes sem nenhum contato em 2026 | 94 |
| Volume de atendimentos | ~100/ano |

69% dos clientes já atendidos estão fora do ciclo. **Essa é a receita que o sistema existe para recuperar.** O volume é pequeno: nenhuma decisão técnica deve ser justificada por desempenho sem medição.

---

## 2. Estado atual

**Fase 0 concluída. Fatia vertical do painel em produção.**

| Ambiente | URL |
|---|---|
| Frontend | https://valtis-gray.vercel.app |
| Backend | https://valtis-production.up.railway.app |
| Repositório | https://github.com/DanielTabosa/Valtis (branch `main`) |
| Banco | PostgreSQL no Supabase, região São Paulo |

Deploy contínuo: push em `main` → Railway rebuilda o backend (Docker, root `backend/`), Vercel rebuilda o frontend (root `frontend/`).

### O que funciona hoje

- Backend Spring Boot conectado ao Postgres, Flyway aplicando migrations
- Schema de 6 tabelas: `usuario`, `cliente`, `especificacao`, `estacao`, `valvula`, `servico`
- Cálculo de vencimento (RN-01) e classificação do painel (RN-04) em `PainelService`
- `GET /api/painel` e `GET /api/painel/resumo`
- Frontend React exibindo o painel com cartões de resumo e tabela colorida
- CORS configurável por ambiente

### O que NÃO existe ainda

| Faltando | Consequência |
|---|---|
| **Qualquer teste automatizado** | Viola `AGENTS.md` §5. Regras `RN-xx` sem cobertura |
| **Autenticação** | API e painel abertos na internet. **Bloqueia entrada de dado real** |
| Endpoints/telas de cadastro | Só dá para criar dados via SQL |
| Tabelas `oportunidade`, `anexo`, `auditoria` | Pipeline comercial e fotos impossíveis |
| Integração Google Calendar + Outbox | **O coração da ideia original ainda não existe** |
| Importador dos 296 clientes | Pipeline nasce vazio |
| Geração de PDF | — |

---

## 3. Mapa do código

```
backend/src/main/java/br/com/manutec/valtis/
├── ValtisApplication.java        ponto de entrada; sobe Tomcat embutido
├── comum/CorsConfig.java         libera origens via ${CORS_ORIGENS}
├── usuario/                      Usuario + UsuarioRepository
├── cliente/                      Cliente + ClienteRepository
├── especificacao/                Especificacao + EspecificacaoRepository
├── estacao/                      Estacao + EstacaoRepository
├── valvula/                      Valvula, ValvulaRepository, PainelProjecao
├── servico/                      Servico + ServicoRepository
├── painel/                       StatusPainel, LinhaPainel, PainelService, PainelController
└── fase0/                        ⚠️ DESCARTÁVEL — ver dívidas (§7)
```

**Organização por funcionalidade**, não por camada — coerente com o monolito modular (D-11).

```
backend/src/main/resources/
├── application.yml               config; segredos só por variável de ambiente
└── db/migration/
    ├── V1__teste_conexao.sql     ⚠️ descartável
    ├── V2__schema_inicial.sql    schema real
    └── V3__seed_demonstracao.sql ⚠️ dados fictícios, remover antes de dado real

frontend/src/
├── api.ts                        fetch do backend; URL via VITE_API_URL
├── App.tsx                       tela única do painel
├── App.css / index.css           estilos (CSS puro, sem Tailwind — ver §7)
```

### Onde moram as regras de negócio

**`painel/PainelService.java`** é o único lugar com regra hoje:

- `montarLinha()` → RN-01 (vencimento = última manutenção + periodicidade) e RN-05 (sem registro não gera oportunidade)
- `classificar()` → RN-04 (VENCIDO / PRÓXIMO DO VENCIMENTO / EM DIA)
- `JANELA_ALERTA_DIAS = 30` → D-08

A consulta em `ValvulaRepository.buscarDadosDoPainel()` devolve **apenas dados crus**. Não mova cálculo para o SQL.

---

## 4. Decisões já tomadas

Detalhe e justificativa em `docs/decisions.md`. **Não reabra sem motivo novo.**

| ID | Decisão |
|---|---|
| D-01 | Gmail comum (não Workspace) → agenda compartilhada + OAuth refresh token. App OAuth **precisa estar publicado em produção** no Google Cloud, senão o token expira em dias |
| D-02 | Lançamento **não** é feito no local → offline fora do escopo; fotos aceitam galeria; `data_realizada` é campo explícito |
| D-04 | Fila de oportunidades **sem dono fixo**; qualquer usuário assume |
| D-05 | Ao marcar `recusado`, o sistema **pergunta** se e quando voltar a lembrar |
| D-07 | Estação redutora é entidade própria: conjunto de VRPs, unidade faturada |
| D-08 | **Dois alertas**: 30 dias (comercial) e 7 dias (lembrete final) |
| D-09 | Importar **só** os 296 clientes + data da última manutenção. Sem eventos retroativos |
| D-10 | Pressão de entrada/saída: campos **opcionais** |
| D-11 | Monolito modular. Sem microsserviços, fila dedicada ou cache distribuído |
| D-13 | **Padrão Outbox** para o Calendar. Falha do Google nunca pode perder registro |
| D-14 | Domínio em **português** (tabelas, colunas, entidades, casos de uso), infra em inglês |
| D-15 | Status do painel **calculado em tempo real**, nunca materializado |
| D-18 | Serviço registrado **por válvula individual**, mesmo quando a visita cobre a estação |
| D-19 | Estação comporta **N** válvulas, tipicamente 2 |
| D-20 | Stack **Java 21 + Spring Boot 4.1.1 + Maven + JPA + Flyway + PostgreSQL**, frontend **React + Vite + TS** |

Substituídas: D-03 (→ D-08), D-12 (→ D-20), D-16 (→ D-18).

---

## 5. Backlog, em ordem de execução

### Próximo — Cadastros (Fase 1)

Endpoints REST e telas para `cliente`, `especificacao`, `estacao`, `valvula`, `servico`.

- RF-05 a RF-13 (cadastros), RF-14 a RF-18 (lançamento de serviço)
- RF-09: gerar `codigo_referencia` no padrão `COND-001-VRP-01` (RN-14)
- RF-10: criar cliente/estação/válvula **de dentro** do formulário de lançamento
- RF-12: inativação lógica, nunca `DELETE` (RN-11)
- RF-13: alerta de duplicidade por (`estacao_id`, `numero_valvula`)
- RF-15b: ao lançar, permitir **replicar** o preenchimento para as demais válvulas da estação — mitiga o custo do D-18
- Ao concluir: remover a migration de seed `V3` por uma nova migration

### Depois — Autenticação (bloqueante para dado real)

- Spring Security, perfis `tecnico` e `admin` (RF-01 a RF-03)
- **Nenhum dado real dos 296 clientes pode entrar antes disso** (RNF-22, LGPD)

### Depois — Google Calendar + Outbox

É o coração da ideia original. Exige migration da tabela `oportunidade`.

- RN-07: todo serviço concluído gera oportunidade **sem responsável**
- RN-06: dois eventos, em `data_prevista − 30` e `data_prevista − 7`
- RF-31: falha do Google **não** impede o salvamento; worker `@Scheduled` reprocessa
- Worker idempotente, retentativa espaçada, limite de tentativas, alerta se token revogado
- RF-29: o evento deve trazer condomínio, código, localização, andares, **se exige fechamento geral**, data da última manutenção e link para o registro

### Depois — Importador dos 296 clientes

- RF-51 a RF-59. Fonte: `RELATORIO NF MANUTENCAO VALVULAS - CONSOLIDADO.xlsx` (**não está no repositório** — pedir ao Daniel)
- Oportunidades de **escopo cliente** (sem válvula), `cadastro_incompleto = true`
- `data_legado_aproximada = true` — as datas vêm de metadado de arquivo, não da NF
- **Deduplicação assistida, nunca automática.** Nomes reais vêm sujos: `02 OPERA CLASSIC`, `1976ED CAIS DA AURORA`
- RN-13: nenhum evento retroativo no Calendar

### Depois — Fotos e PDF

- RF-19 (câmera **e galeria**, compressão no dispositivo), tabela `anexo`, storage
- RF-48 PDF com JasperReports. ⚠️ iText 7 é AGPL, restritivo para uso comercial

### Por último — Pipeline comercial

- RF-41 a RF-47. Tabela `oportunidade` já existirá
- RF-43 (destacar oportunidade vencendo sem dono) é praticamente obrigatório dado o D-04

---

## 6. Decisões pendentes — exigem o Daniel

Não invente resposta para nenhuma destas.

| # | Pergunta | Trava |
|---|---|---|
| P-01b | A estação inclui outros componentes relevantes ao serviço (filtro Y, manômetros, registros de bloqueio)? | Modelagem de `estacao` |
| P-01c | A periodicidade de 12 meses é da válvula ou da estação? | RN-02 |
| P-03 | Existe base de clientes sob contrato **fora** das 595 NFs analisadas? | Dimensionamento da importação |
| P-04 | `valor_cobrado` entra no registro de serviço? | RF-47 — sem ele mede conversão, não receita |
| P-05 | As 3 pessoas são todas de campo, ou alguém é só de escritório? | Perfis de acesso |
| P-06 | Como o técnico identifica fisicamente a válvula em campo? Existe plaqueta? Vale QR Code? | RF-11 |
| P-07 | Teto de custo mensal, preferência por dados no Brasil, domínio próprio | Hospedagem |
| P-08 | Painel de válvulas e lista de clientes importados: telas separadas ou visão unificada? | RF-34, RF-53 |
| P-09 | Plano pago do Supabase antes do uso real? O gratuito hiberna — sistema fora do ar | Confiabilidade |
| P-10 | Vercel **Hobby** proíbe uso comercial. Migrar para Cloudflare Pages ou pagar Pro? | Conformidade |

---

## 7. Dívidas técnicas conhecidas

| Dívida | Ação |
|---|---|
| **Zero testes.** `AGENTS.md` §5 exige teste para toda `RN-xx` | Começar por `PainelService`: cálculo de vencimento, classificação, caso `SEM REGISTRO`. JUnit 5 + Mockito; Testcontainers quando houver banco envolvido |
| Pacote `fase0/` e migration `V1` são descartáveis | Remover quando os cadastros existirem |
| Migration `V3` insere dados fictícios | Remover por nova migration antes de dado real |
| `frontend` usa CSS puro; `architecture.md` prevê Tailwind + shadcn/ui | Alinhar ou registrar decisão que altera o documento |
| `backend/HELP.md` é lixo do Spring Initializr | Pode apagar |
| Pasta duplicada e **obsoleta** em `C:\Users\didit\OneDrive\Área de Trabalho\Valtis` | O repositório ativo é `C:\dev\Valtis`. A cópia no OneDrive ficou de uma mudança por `robocopy` e deve ser apagada — ela confunde agente e humano |

---

## 8. Regras rígidas

Resumo. O texto que vale é o `AGENTS.md`.

### 🔴 Exige aprovação humana ANTES de executar (§10)

1. **Schema** — criar, renomear ou remover tabela/coluna; gerar ou rodar migration; alterar enum ou constraint
2. **Regra de negócio `RN-xx`** — periodicidade, cálculo de vencimento, janelas de 30/7 dias, classificação do painel
3. **Google e segredos** — credenciais, escopos OAuth, eventos em agenda real
4. **Dados e produção** — qualquer `DELETE` físico, importador sobre base real, deploy, variável de produção

Também pare e pergunte quando: a tarefa não corresponde a nenhum `RF/RNF/RN`; duas fontes se contradizem; ou você precisou **supor** uma regra de negócio.

### Fronteiras de arquitetura

- Casos de uso **não** importam `googleapis`, `@prisma/client`/JPA diretamente, SDK de storage ou `fetch`
- Nenhuma chamada externa dentro de transação de escrita → Outbox
- Regra de negócio **nunca** em componente React nem em SQL
- Exclusão é sempre lógica (`ativo = false`)

### Código

- Domínio em português, infra em inglês (D-14)
- `any` proibido no TypeScript
- Sem Lombok — decisão pedagógica; usar `record` para DTOs
- Datas em UTC no banco, conversão só na exibição
- Conventional Commits, referenciando o requisito: `feat: calcula vencimento (RF-23, RN-01)`
- Documentação atualizada **no mesmo commit** que muda comportamento

---

## 9. Armadilhas já pagas — não repita

| Sintoma | Causa | Solução |
|---|---|---|
| `UnknownHostException` no host do Supabase | Projeto hibernado no plano gratuito | Restaurar em supabase.com |
| `SocketException: Network is unreachable` no servidor | Conexão direta do Supabase é **IPv6-only**; Railway não tem IPv6 | Usar **Session pooler**. O usuário muda para `postgres.<ref-do-projeto>` |
| 500 em consulta nativa com projeção | Postgres rebaixa apelido sem aspas (`valvulaId` → `valvulaid`) | Apelidos entre aspas: `AS "valvulaId"` |
| CORS bloqueado | Origem não autorizada | `CORS_ORIGENS` **sem barra no final** |
| Variável mudada no Railway sem efeito | Ele **nem sempre** reimplanta ao salvar | Deploy manual em *Deployments* |
| Frontend chamando `localhost` em produção | `VITE_*` é gravada no build, não lida em execução | Alterar variável **e** redeploy na Vercel |
| Migration sumindo do commit | `.gitignore` tinha `*.sql` para proteger dumps | Exceção `!**/src/main/resources/db/migration/*.sql` já existe |

**Ao diagnosticar erro de Spring: a causa raiz está no ÚLTIMO `Caused by:` da pilha, não no primeiro.**

---

## 10. Sobre o Daniel — leia antes de despejar código

Isso muda **como** você trabalha, não só o quê.

O Valtis tem **duplo objetivo**: resolver a dor da empresa **e** ser o portfólio de entrada do Daniel na área de tecnologia. Ele está aprendendo Java e Spring enquanto o projeto é construído, dispõe de ~10h por semana, e o critério de sucesso dele inclui **conseguir defender cada decisão em entrevista**.

Consequências práticas:

- **Explique antes de construir.** Analogia do mundo real primeiro, depois o termo técnico. Frases do tipo *"leia assim: ..."* traduzindo comando, log ou erro para linguagem comum
- **Um conceito por vez.** Não empilhe três ideias novas no mesmo parágrafo
- Aprofundar em: regras de negócio, Outbox, segurança, JPA e transações. Acelerar em: CRUD repetitivo, telas, configuração
- **Avise o erro antes de ele acontecer** quando for previsível — errar sabendo o que vai errar ensina
- Agrupe comandos em blocos, dizendo o que esperar de cada um, em vez de um por vez
- Ele executa os comandos no terminal integrado do VS Code, em duas abas (backend e frontend)

**Pedido explícito pendente:** antes de escrever código novo, ele quer uma sessão de **revisão técnica** do que já existe — entender o código, por que Railway e Vercel, e como apresentar o projeto sem exagerar nem se diminuir. Se ele não mencionar, pergunte se quer fazer isso primeiro.

---

## 11. Como rodar

Detalhe em `README.md`. Resumo:

```powershell
# Terminal 1
cd C:\dev\Valtis\backend
.\mvnw.cmd spring-boot:run      # http://localhost:8080

# Terminal 2
cd C:\dev\Valtis\frontend
npm run dev                      # http://localhost:5173
```

Variáveis de ambiente de usuário necessárias: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
Antes de rodar, confira se o projeto no Supabase não está hibernado.

Para capturar a saída completa num arquivo:

```powershell
.\mvnw.cmd spring-boot:run > erro.txt 2>&1
```

---

## 12. Primeira tarefa sugerida

Se o Daniel não indicar outra coisa, e **depois** da revisão técnica do §10:

**Escrever os testes do `PainelService`.**

Por quê: é a única regra de negócio que existe hoje, está sem cobertura nenhuma, viola o `AGENTS.md` §5, e é o melhor primeiro contato dele com JUnit — testando código que ele já entende.

Critérios de aceite:

- Vencimento calculado a partir de `data_realizada + periodicidade_meses` (RN-01)
- `VENCIDO` quando `diasRestantes < 0`
- `PRÓXIMO DO VENCIMENTO` nas bordas: 0 e 30 dias
- `EM DIA` a partir de 31 dias
- `SEM REGISTRO` quando não há serviço, **sem** calcular vencimento (RN-05)
- Sem banco: o repositório entra como dublê (Mockito)
