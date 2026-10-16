# Valtis

Sistema de controle de manutenção de válvulas redutoras de pressão (VRP) da **Manutec Válvulas**.

Registra as manutenções executadas em campo e agenda automaticamente o retorno de 12 meses no Google Calendar, transformando manutenções passadas em oportunidades de reventa rastreáveis.

---

## O problema

A Manutec instala e mantém VRPs em prédios. Toda manutenção abre uma janela de reventa cerca de 12 meses depois. Hoje esse rastreamento é manual — alguém garimpa registros antigos em momentos vagos. O resultado é mensurável:

| Situação da base real (2018 – ago/2026) | Número |
|---|---|
| NFs de manutenção emitidas | 595 |
| Clientes distintos atendidos | 296 |
| Clientes há **24+ meses** sem manutenção (crítico) | **153** |
| Clientes há **12+ meses** sem manutenção (atrasado) | 52 |
| Clientes recorrentes **sem nenhum contato em 2026** | **94** |

**69% dos clientes já atendidos estão fora do ciclo de 12 meses.** Essa é a receita que o sistema existe para recuperar.

## O que o sistema faz

1. O técnico lança a manutenção executada pelo celular ou pelo PC.
2. O sistema calcula o vencimento do próximo ciclo (padrão: 12 meses).
3. Cria dois eventos na agenda da empresa — 30 dias antes (contato comercial) e 7 dias antes (lembrete final) — convidando os 3 usuários.
4. Mantém um painel de status de todas as válvulas: vencidas, próximas do vencimento, em dia e sem registro.
5. Gera o relatório de serviço em PDF para envio ao síndico.

## Estado atual

**Fase 1 implementada na branch `feat/fase-1-mvp`, aguardando teste manual.** Produção ainda roda a Fase 0.

| Ambiente | URL |
|---|---|
| Frontend | https://valtis-gray.vercel.app |
| Backend | https://valtis-production.up.railway.app |
| Banco | PostgreSQL no Supabase (região São Paulo) |

Pronto na branch:

- Login com perfis técnico e administrador, token JWT e bloqueio por senha errada (D-22)
- Cadastros de cliente, especificação, estação e válvula, com código automático e alerta de duplicidade
- Lançamento de serviço por válvula, com replicação para as demais válvulas da estação
- Painel com filtros e resumo, calculado no servidor
- Fila de oportunidades: assumir, desfecho, recusa com nova data de lembrete
- Fila de saída da agenda (Outbox) com worker, retentativa e tela de sincronização
- Adaptador do Google Calendar escrito e testado contra servidor simulado (falta a credencial real)
- 67 testes automatizados, inclusive com Postgres real (Testcontainers), e CI no GitHub Actions

A fazer, em ordem: credencial do Google Calendar · importação dos 296 clientes · fotos e PDF.

> ⚠️ A migration `V3__seed_demonstracao.sql` insere **dados fictícios**. Remover antes de qualquer dado real entrar.

## Documentação

Leia nesta ordem:

| Documento | Para quê |
|---|---|
| **[HANDOFF.md](HANDOFF.md)** | **Comece por aqui.** Estado atual, backlog em ordem, decisões pendentes, dívidas técnicas e armadilhas conhecidas |
| **[AGENTS.md](AGENTS.md)** | **Obrigatório antes de qualquer alteração no repositório.** Regras de arquitetura, código, testes, Git, segurança e o que exige aprovação humana |
| [docs/requirements.md](docs/requirements.md) | O **que** construir. Requisitos funcionais (RF), não funcionais (RNF) e regras de negócio (RN), com IDs estáveis |
| [docs/architecture.md](docs/architecture.md) | **Como** construir. Camadas, fronteiras, padrão Outbox, módulos e stack |
| [docs/database.md](docs/database.md) | O modelo de dados, convenções de nomenclatura, mapeamento da planilha legada e o glossário do domínio |
| [docs/decisions.md](docs/decisions.md) | **Por que** está assim. Registro de decisões (ADR), append-only |
| [docs/plano-fase-0.md](docs/plano-fase-0.md) | Plano de execução da Fase 0, semana a semana, com os conceitos a estudar em paralelo |
| [_arquivo/](_arquivo/) | Versões anteriores da documentação. Histórico, **não** fonte de verdade |

**Regra de ouro:** cada fato mora em um único arquivo. Os demais referenciam por ID (`RF-26`, `RN-04`, `D-08`). Se você encontrar a mesma informação em dois lugares, é bug de documentação — reporte.

## Stack

**Backend:** Java 21 · Spring Boot 4 · Maven · Spring Data JPA · Flyway · PostgreSQL · Spring Security (JWT) · JUnit 5 · Mockito · Testcontainers
**Frontend:** React · Vite · TypeScript · React Router · CSS próprio (D-24)
**Integração:** Google Calendar API via padrão Outbox

Detalhes, alternativas descartadas e o porquê da escolha em [docs/architecture.md](docs/architecture.md) e [docs/decisions.md](docs/decisions.md) · D-20.

## Como rodar

### Pré-requisitos (uma vez só)

- **JDK 21** — `winget install EclipseAdoptium.Temurin.21.JDK`, com `JAVA_HOME` configurado
- **Node 18+** — `winget install OpenJS.NodeJS.LTS`
- **Docker Desktop** — para o banco local e para os testes de integração

Maven não precisa ser instalado — o projeto usa o Maven Wrapper (`mvnw`).

### Rodar localmente, com banco local (recomendado para desenvolver e testar)

O perfil `local` usa um Postgres em Docker na porta 5433 e **nunca toca o Supabase**.
Ele cria um administrador de teste e usa uma agenda simulada, que só escreve no log.
O login está em `backend/src/main/resources/application-local.yml`.

```powershell
# Terminal 1 — banco local (uma vez por sessão; os dados ficam guardados)
cd C:\dev\Valtis
docker compose up -d

# Terminal 1 — backend no perfil local (as aspas são obrigatórias no PowerShell)
cd C:\dev\Valtis\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Terminal 2 — frontend
cd C:\dev\Valtis\frontend
npm run dev
```

Abra `http://localhost:5173`. Para zerar o banco local: `docker compose down -v`.

### Testes

```powershell
cd C:\dev\Valtis\backend
.\mvnw.cmd test      # precisa do Docker Desktop aberto
```

### Rodar contra o banco de produção (evite)

Sem o perfil `local`, o backend usa as variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e também exige
`VALTIS_JWT_SEGREDO`. ⚠️ Ao subir, o Flyway aplica no Supabase as migrations novas.

### Rotina para começar a trabalhar

**1. Abrir o projeto e o Docker Desktop**

```powershell
code C:\dev\Valtis
```

**2. Banco local.** Na raiz do projeto: `docker compose up -d`. Só é preciso conferir o Supabase
(se está *Paused*, clicar em **Restore**) quando for mexer em produção.

**3. Backend** — terminal 1 no VS Code (`Ctrl + '`):

```powershell
cd C:\dev\Valtis\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Sobe em `http://localhost:8080`. Pronto quando aparecer `Started ValtisApplication`.

**4. Frontend** — terminal 2 (botão `+` no painel do terminal):

```powershell
cd C:\dev\Valtis\frontend
npm run dev
```

**5. Abrir** `http://localhost:5173`

Para parar qualquer um dos dois: `Ctrl + C` no terminal correspondente.

### Endpoints disponíveis

Todos exigem o cabeçalho `Authorization: Bearer <token>`, exceto login e health.

| Endpoint | O que faz |
|---|---|
| `POST /api/auth/login` | Devolve o token. Público |
| `GET /api/auth/eu` · `POST /api/auth/senha` | Quem sou eu · trocar a própria senha |
| `GET /api/painel` | Linhas e resumo por situação. Filtros: `busca`, `status`, `bairro`, `tipoAtendimento`, `clienteId` |
| `GET/POST /api/clientes` · `GET/PUT /api/clientes/{id}` | Cadastro de clientes |
| `POST /api/clientes/{id}/estacoes` · `PUT /api/estacoes/{id}` | Estações |
| `POST /api/estacoes/{id}/valvulas` · `GET/PUT/DELETE /api/valvulas/{id}` | Válvulas e ficha com histórico (DELETE só sem histórico) |
| `GET/POST /api/especificacoes` · `PUT /api/especificacoes/{id}` | Catálogo |
| `POST /api/servicos/lancamentos` | Lança serviço em uma ou mais válvulas |
| `GET /api/oportunidades` · `POST .../{id}/assumir`, `/liberar`, `/desfecho` | Fila comercial |
| `GET /api/agenda/situacao?status=` · `POST /api/agenda/sincronizar` | Sincronização com a agenda |
| `GET /api/usuarios/ativos` · `GET/POST/PUT /api/usuarios` | Usuários (gestão só para admin) |
| `POST .../{id}/inativar`, `/reativar` | Inativação lógica. Só admin |
| `GET /api/health` | Sinal de vida, não depende do banco. Público |

### Problemas conhecidos

| Sintoma | Causa | Solução |
|---|---|---|
| `UnknownHostException` no host do Supabase | Projeto hibernado no plano gratuito | Restaurar em `supabase.com` |
| `SocketException: Network is unreachable` em servidor | A conexão direta do Supabase só atende IPv6 | Usar a string do **Session pooler**. O usuário muda para `postgres.<ref-do-projeto>` |
| Erro de CORS no console do navegador | Origem do frontend não autorizada | Ajustar `CORS_ORIGENS` no Railway. Valor **sem barra no final** — o Spring compara a origem caractere por caractere |
| Variável alterada no Railway sem efeito | Ele **nem sempre** reimplanta sozinho ao salvar variável | Disparar **Deploy manual** em *Deployments* e conferir se o novo deploy ficou `ACTIVE` |
| Frontend na Vercel chamando `localhost` | Variáveis `VITE_*` são gravadas no código **durante o build**, não lidas em execução | Adicionar a variável e então **Redeploy** na Vercel |
| Domínio do Railway com sufixo duplicado | O campo já acrescenta `.up.railway.app` | Digitar só o nome curto ao gerar o domínio |
| `index.lock` impedindo `git add` | VS Code segurando o arquivo | `Remove-Item .git\index.lock -Force` |
| Backend não sobe: `Defina VALTIS_JWT_SEGREDO` | Falta a chave que assina os tokens | Rodar com o perfil `local`, ou definir a variável com 32+ caracteres |
| `Connection refused` na porta 5433 | Banco local parado | `docker compose up -d` na raiz do projeto |
| Testes falham com `Could not find a valid Docker environment` | Docker Desktop fechado | Abrir o Docker Desktop e rodar de novo |
| Acento chega quebrado ao testar com `curl` no Git Bash | O terminal manda o texto fora de UTF-8 | Testar pela tela, ou mandar o corpo de um arquivo salvo em UTF-8 |

### Diagnóstico

Para capturar a saída completa num arquivo em vez de rolar o terminal:

```powershell
.\mvnw.cmd spring-boot:run > erro.txt 2>&1
```

A causa raiz costuma estar no **último** `Caused by:` da pilha, não no primeiro.
Apague o arquivo depois — ele não deve ser commitado.

## Roadmap

| Fase | Escopo | Marco |
|---|---|---|
| **0 — Fundação** | Conta Google, agenda compartilhada, app OAuth publicado, repositório e deploy vazio | Um evento criado pelo sistema aparece na agenda dos 3 celulares |
| **1 — MVP + carga da base** | Acesso, cadastros, lançamento, painel, recorrência, Calendar, importação dos 296 clientes | No primeiro dia de uso o painel já mostra 153 clientes críticos como lista de trabalho |
| **2 — Fotos e relatório** | Anexos e PDF do serviço | O técnico envia o relatório ao síndico no fim do atendimento |
| **3 — Pipeline comercial** | Fila de oportunidades, desfecho, indicadores | Responder "quantos clientes recuperamos neste trimestre?" com um número |
| **4 — Refinamentos** | Exportações, QR Code, notificações | Sob demanda |

## Usuários

Três pessoas da Manutec, usando celular em campo e PC no escritório. Perfis: **técnico** e **administrador**.

## Contexto do negócio

**VRP** = Válvula Redutora de Pressão. **Estação redutora** = conjunto que contém várias VRPs, atendendo uma faixa de andares. A estação é a unidade que a empresa fatura, e por isso é a unidade central do modelo de dados. Glossário completo em [docs/database.md](docs/database.md#glossário-do-domínio).
