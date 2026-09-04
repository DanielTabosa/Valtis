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

**Fase 0 concluída. Fatia vertical do painel no ar, em produção.**

| Ambiente | URL |
|---|---|
| Frontend | https://valtis-gray.vercel.app |
| Backend | https://valtis-production.up.railway.app |
| Banco | PostgreSQL no Supabase (região São Paulo) |

Pronto:

- Backend Spring Boot conectado ao PostgreSQL, com Flyway aplicando migrations
- Schema do domínio: `usuario`, `cliente`, `especificacao`, `estacao`, `valvula`, `servico`
- Cálculo de vencimento (RN-01) e classificação do painel (RN-04)
- Frontend React exibindo o painel com resumo por situação
- Deploy contínuo: backend no Railway via Docker, frontend na Vercel

A fazer, em ordem: endpoints e telas de cadastro · autenticação · integração com o Google Calendar · importação dos 296 clientes.

> ⚠️ A migration `V3__seed_demonstracao.sql` insere **dados fictícios** para o painel ter o que exibir. Remover antes de qualquer dado real entrar.
>
> ⚠️ **Ainda não há autenticação.** Nenhum dado real de cliente pode entrar no sistema antes disso (RNF-22, LGPD).

## Documentação

Leia nesta ordem:

| Documento | Para quê |
|---|---|
| **[AGENTS.md](AGENTS.md)** | **Obrigatório antes de qualquer alteração no repositório.** Regras de arquitetura, código, testes, Git, segurança e o que exige aprovação humana |
| [docs/requirements.md](docs/requirements.md) | O **que** construir. Requisitos funcionais (RF), não funcionais (RNF) e regras de negócio (RN), com IDs estáveis |
| [docs/architecture.md](docs/architecture.md) | **Como** construir. Camadas, fronteiras, padrão Outbox, módulos e stack |
| [docs/database.md](docs/database.md) | O modelo de dados, convenções de nomenclatura, mapeamento da planilha legada e o glossário do domínio |
| [docs/decisions.md](docs/decisions.md) | **Por que** está assim. Registro de decisões (ADR), append-only |
| [docs/plano-fase-0.md](docs/plano-fase-0.md) | Plano de execução da Fase 0, semana a semana, com os conceitos a estudar em paralelo |
| [_arquivo/](_arquivo/) | Versões anteriores da documentação. Histórico, **não** fonte de verdade |

**Regra de ouro:** cada fato mora em um único arquivo. Os demais referenciam por ID (`RF-26`, `RN-04`, `D-08`). Se você encontrar a mesma informação em dois lugares, é bug de documentação — reporte.

## Stack

**Backend:** Java 21 · Spring Boot 3 · Maven · Spring Data JPA · Flyway · PostgreSQL · Spring Security · JasperReports
**Frontend:** React · Vite · TypeScript · Tailwind + shadcn/ui
**Integração:** Google Calendar API via padrão Outbox

Detalhes, alternativas descartadas e o porquê da escolha em [docs/architecture.md](docs/architecture.md) e [docs/decisions.md](docs/decisions.md) · D-20.

## Como rodar

### Pré-requisitos (uma vez só)

- **JDK 21** — `winget install EclipseAdoptium.Temurin.21.JDK`, com `JAVA_HOME` configurado
- **Node 18+** — `winget install OpenJS.NodeJS.LTS`
- Variáveis de ambiente de usuário com as credenciais do banco: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

Maven não precisa ser instalado — o projeto usa o Maven Wrapper (`mvnw`).

### Rotina para começar a trabalhar

**1. Abrir o projeto**

```powershell
code C:\dev\Valtis
```

**2. Conferir o banco.** Entre em `supabase.com` e veja se o projeto está ativo.
No plano gratuito ele hiberna após alguns dias sem uso. Se estiver *Paused*,
clique em **Restore** e aguarde. Sintoma de banco pausado ao rodar:
`UnknownHostException` no endereço do Supabase.

**3. Backend** — terminal 1 no VS Code (`Ctrl + '`):

```powershell
cd C:\dev\Valtis\backend
.\mvnw.cmd spring-boot:run
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

| Endpoint | O que faz |
|---|---|
| `GET /api/painel` | Uma linha por válvula ativa, com status e dias restantes |
| `GET /api/painel/resumo` | Contagem por situação |
| `GET /api/health` | Sinal de vida, não depende do banco |

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
