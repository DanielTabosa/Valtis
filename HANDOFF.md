# HANDOFF — Valtis

**Para:** qualquer agente que vá trabalhar neste repositório
**Atualizado em:** 17/10/2026
**Branch de trabalho:** `feat/fase-1-mvp` (não publicada, não mesclada na `main`)

Este arquivo é o resumo de situação. Ele **não substitui** `AGENTS.md`, `docs/requirements.md`, `docs/architecture.md`, `docs/database.md` e `docs/decisions.md` — leia-os conforme a ordem do `CLAUDE.md`. O que este arquivo faz é te dizer, rápido: onde o projeto está, o que já foi decidido, o que falta e o que você **não** pode fazer sozinho.

---

## 1. O produto em um parágrafo

A **Manutec Válvulas** (Recife/PE) faz manutenção de VRPs — válvulas redutoras de pressão — em prédios. Toda manutenção abre uma janela de reventa cerca de 12 meses depois. Hoje esse rastreamento é manual e falha. O **Valtis** registra as manutenções executadas, calcula o vencimento do próximo ciclo e agenda o retorno no Google Calendar. Três usuários: dois perfis, `tecnico` e `admin`.

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

**Fase 1 implementada na branch `feat/fase-1-mvp` (publicada no GitHub, não mesclada).** Testada e aprovada pelo Daniel em 16/10/2026, com ajustes. Todas as suposições S-01 a S-11 e as decisões D-21 a D-25 estão validadas. **Produção fica para depois, por decisão do Daniel**: ainda há ajustes a fazer. A `main` e a produção continuam na Fase 0 (só o painel, sem login).

| Ambiente | URL | Versão |
|---|---|---|
| Frontend | https://valtis-gray.vercel.app | Fase 0 |
| Backend | https://valtis-production.up.railway.app | Fase 0 |
| Repositório | https://github.com/DanielTabosa/Valtis | `main` = Fase 0 · `feat/fase-1-mvp` = Fase 1 |
| Banco de produção | PostgreSQL no Supabase, São Paulo | migrations V1–V3 |
| Banco local | Docker, `docker-compose.yml`, porta 5433 | V1–V4 |

### O que a branch entrega

| Módulo | Requisitos | Observação |
|---|---|---|
| Acesso (M1) | RF-01 a RF-03 | JWT de 14 dias, BCrypt, bloqueio após 5 erros, perfil lido do banco a cada requisição (D-22) |
| Cadastros (M2) | RF-05 a RF-13 | Código `COND-001` e `COND-001-VRP-01` automáticos, duplicidade barrada, inativação lógica |
| Lançamento (M3) | RF-14 a RF-18, RF-15b, RF-10 | Uma transação para N válvulas; replicação na tela; cadastro sem sair do lançamento |
| Painel (M4) | RF-34 a RF-39 | Filtros por busca, situação, tipo de atendimento; os cartões do resumo respeitam busca e tipo, mas não zeram ao escolher a situação |
| Recorrência (M5) | RN-01 a RN-07, RN-10 a RN-12 | Regras em `RegrasRecorrencia`; "hoje" em `America/Recife` via `Clock` injetado |
| Calendar (M6) | RF-26 a RF-31, RF-33 | Outbox em `evento_agenda` + worker; adaptador Google por REST **sem credencial real**; agenda simulada no local |
| Pipeline (M10) | RF-41 a RF-46 | Assumir, devolver, desfecho, recusa com pergunta e nova data (RN-09); marca "sem lembrete na agenda" (RN-13) |
| Importação (M7) | RF-51 a RF-58 | Tela Importar (admin): .xlsx/.xls/.csv, colunas reconhecidas pelo cabeçalho, descarta o resto, prévia editável, gravação tudo ou nada (D-25). **Nunca rodou com a planilha real** |
| Interface (17/10) | — | Revisão visual com identidade Manutec: lateral no desktop, barra inferior com "Lançar" no celular, tema claro/escuro, Inter auto-hospedada, ícones SVG próprios, arte do login. Crítica e filosofia em `docs/design/`. **Ainda não vista pelo Daniel** |
| Qualidade | AGENTS.md §5 | 78 testes (unitários + integração com Testcontainers), CI no GitHub Actions |

### O que NÃO existe ainda

| Faltando | Por quê |
|---|---|
| Credencial real do Google Calendar | Exige o app OAuth publicado pelo Daniel (D-01). O código está pronto |
| Rodar a importação dos 296 clientes | O importador existe; falta a planilha. Rodar em produção exige aprovação (§10) |
| Fotos (RF-19) e PDF (RF-48) | Fase 2 |
| Auditoria (RNF-17), rascunho (RF-20), exportar CSV (RF-40) | Não priorizados |
| Atualizar/remover evento já criado no Google (RF-32) | Hoje, mudar a data só reprograma eventos ainda não enviados |
| Filtro do painel por técnico e período (parte do RF-38) | Não implementado |

---

## 3. Como testar (o Daniel, manualmente)

Detalhe no `README.md`, seção "Rodar localmente". Resumo:

```powershell
cd C:\dev\Valtis
git checkout feat/fase-1-mvp
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
# outro terminal
cd C:\dev\Valtis\frontend
npm install
npm run dev
```

Login local: `backend/src/main/resources/application-local.yml`, bloco `admin-inicial`. O banco local também tem um técnico de teste (`tec@valtis.local`), criado pela tela de Usuários. A agenda simulada escreve os eventos no log do backend (`[agenda simulada]`) a cada 30 segundos.

**Roteiro sugerido:** criar um usuário técnico → entrar como ele → cadastrar condomínio, estação com 2 válvulas → lançar serviço nas duas, ajustando uma → ver no painel → na fila, assumir e recusar pedindo nova data → conferir a tela Agenda → entrar como admin e inativar uma válvula.

---

## 4. Para publicar em produção — checklist (exige o Daniel, AGENTS.md §10)

Nada disto foi feito. Merge na `main` **publica automaticamente** no Railway e na Vercel.

1. **Railway — variáveis novas** (sem a primeira, o backend **não sobe**):
   - `VALTIS_JWT_SEGREDO`: 32+ caracteres aleatórios. Gerar e guardar no cofre.
   - `VALTIS_ADMIN_EMAIL`, `VALTIS_ADMIN_SENHA`, `VALTIS_ADMIN_NOME`: primeiro admin. Remover depois do primeiro login e troca de senha.
   - `VALTIS_URL_FRONTEND`: `https://valtis-gray.vercel.app` (vai nos links dos eventos).
   - `VALTIS_AGENDA_PROVEDOR`: deixar sem definir (= `desativada`) até ter a credencial do Google.
2. **Migration V4 no Supabase:** cria `oportunidade` e `evento_agenda`, **apaga a tabela `teste_conexao`** e gera oportunidades a partir dos serviços do seed. Roda sozinha na primeira subida.
3. **Seed V3 em produção:** continua lá (usuários e cliente fictícios). Removê-lo é `DELETE` físico — decidir antes de dado real.
4. **Vercel:** nada a configurar; o `vercel.json` cuida das rotas.
5. **CI:** o Railway e a Vercel não esperam o CI. Ligar "Wait for CI" no Railway, ou só fazer merge com CI verde.

### Para ligar o Google Calendar (D-01, D-23)

App OAuth **publicado em produção** no Google Cloud, escopo só `calendar.events`, refresh token da conta institucional, e no Railway: `VALTIS_AGENDA_PROVEDOR=google`, `VALTIS_GOOGLE_CLIENT_ID`, `VALTIS_GOOGLE_CLIENT_SECRET`, `VALTIS_GOOGLE_REFRESH_TOKEN`, `VALTIS_GOOGLE_CALENDARIO_ID`, `VALTIS_AGENDA_CONVIDADOS` (e-mails separados por vírgula). O adaptador nunca rodou contra o Google real: testar primeiro com uma agenda de teste.

---

## 5. Suposições feitas sem o Daniel — todas validadas em 16/10/2026

O Daniel autorizou seguir sem aprovação durante a codificação (13/10/2026). Estas escolhas foram feitas para não travar; cada uma é reversível.

| # | Suposição | Onde mexer se estiver errada |
|---|---|---|
| S-01 | ✅ **Validada com ajuste em 16/10.** Técnico cadastra tudo e também **inativa, reativa e exclui válvula** (exclusão só sem histórico, RF-12a). Cliente, estação, especificação e usuários: só admin inativa | `@PreAuthorize` nos controllers |
| S-02 | ✅ **Validada em 16/10.** RN-10 por válvula, não por estação | `OportunidadeService.abrirNovoCiclo` |
| S-03 | ✅ Serviço lançado com data **anterior** ao último já registrado entra no histórico, mas **não** muda o ciclo | `LancamentoService.lancarItem` |
| S-04 | ✅ **Validada em 16/10 e virou RN-13.** Alerta passado não vai para a agenda; a fila marca "sem lembrete na agenda" e a tela Agenda lista os descartados | `FilaDaAgenda`, `OportunidadeService.resposta` |
| S-05 | ✅ Quem registra desfecho de oportunidade sem dono **passa a ser o dono** | `OportunidadeService.registrarDesfecho` |
| S-06 | ✅ `executado` só por lançamento de serviço, nunca por clique | `Oportunidade.registrarDesfecho` |
| S-07 | ✅ Mudar a periodicidade reprograma o ciclo aberto, exceto oportunidades de retorno de recusa (data escolhida pelo usuário) | `OportunidadeService.reprogramarCicloDaValvula` |
| S-08 | ✅ Sessão de 14 dias; bloqueio de 15 min após 5 senhas erradas | `application.yml`, bloco `seguranca` |
| S-09 | ✅ Painel e lista de clientes importados ficam em telas separadas (P-08). A lista própria ainda não existe | — |
| S-10 | ✅ **Revista em 16/10.** A data vem com **hoje** e o aviso "Confira: é hoje?" (RF-17, D-02 revisto) | `Lancamento.tsx` |
| S-11 | ✅ D-21, D-23, D-24 aceitas. D-22 aceita **com gatilho**: com domínio único (P-07), reavaliar cookie `HttpOnly` | `docs/decisions.md` |

---

## 6. Mapa do código

```
backend/src/main/java/br/com/manutec/valtis/
├── comum/          config (ValtisPropriedades, Relógio, CORS), erros (RFC 9457), enums de banco
├── seguranca/      SegurancaConfig, login, emissão e leitura do JWT, força bruta, admin inicial
├── usuario/        Usuario, Perfil, gestão de usuários
├── cliente/ especificacao/ estacao/ valvula/   cadastros (entidade, repositório, serviço, controller)
├── servico/        Servico e o caso de uso LancamentoService
├── recorrencia/    RegrasRecorrencia (RN-01, RN-04, RN-06) e StatusPainel
├── oportunidade/   Oportunidade e OportunidadeService (ciclo, fila, desfecho)
├── agenda/         Outbox: EventoAgenda, FilaDaAgenda, SincronizadorDaAgenda, AgendaGateway (Google/simulada)
├── painel/         PainelService e filtros
└── importacao/     LeitorDePlanilha (POI/CSV), CampoImportavel (cabeçalhos aceitos), ImportacaoService
```

`frontend/src/`: `api/` (cliente HTTP e tipos), `sessao/`, `componentes/` (layout, formulários), `paginas/` (uma por tela), `util/`.

### Onde moram as regras de negócio

- **`recorrencia/RegrasRecorrencia`**: RN-01, RN-04, RN-06. Único lugar.
- **`servico/LancamentoService`** + **`oportunidade/OportunidadeService`**: RN-07, RN-09, RN-10, RN-12.
- **`oportunidade/Oportunidade`**: transições de estado (RN-08).
- **`agenda/SincronizadorDaAgenda`** + **`EventoAgenda`**: Outbox, retentativa, idempotência.
- O React **não** decide status, vencimento nem urgência: tudo vem calculado.

---

## 7. Decisões já tomadas

Detalhe e justificativa em `docs/decisions.md`. **Não reabra sem motivo novo.**

| ID | Decisão |
|---|---|
| D-01 | Gmail comum → agenda compartilhada + OAuth refresh token. App OAuth **publicado em produção** |
| D-02 | Lançamento não é no local → sem offline; `data_realizada` explícita |
| D-04 | Fila sem dono fixo |
| D-05 | Recusa: o sistema pergunta se e quando voltar a lembrar |
| D-07 / D-19 | Estação é entidade própria, com N válvulas |
| D-08 | Dois alertas: 30 e 7 dias |
| D-09 | Importar só os 296 clientes + última manutenção |
| D-11 | Monolito modular |
| D-13 | Outbox para o Calendar |
| D-14 | Domínio em português, infra em inglês |
| D-15 | Status do painel calculado, nunca gravado |
| D-18 | Serviço por válvula individual |
| D-20 | Java 21 + Spring Boot + React |
| D-21 a D-24 | Outbox em tabela própria, JWT (revisar com domínio único), Google via REST, CSS próprio |
| D-25 | Importador reconhece colunas pelo cabeçalho; prévia antes de gravar; tudo ou nada |

---

## 8. Decisões pendentes — exigem o Daniel

| # | Pergunta | Trava |
|---|---|---|
| P-01b | A estação inclui filtro Y, manômetros, registros? | Modelagem de `estacao` |
| P-01c | A periodicidade é da válvula ou da estação? | RN-02 (hoje: da válvula) |
| P-03 | Há clientes sob contrato fora das 595 NFs? | Importação |
| P-04 | `valor_cobrado` entra no serviço? | RF-47 |
| P-05 | Os 3 são de campo ou alguém é só de escritório? | Perfis (ver S-01) |
| P-06 | Plaqueta física? QR Code? | RF-11 |
| P-07 | Teto de custo, dados no Brasil, domínio. Tendência: **domínio único** (reabre D-22) | Hospedagem |
| P-09 | Plano pago do Supabase? O gratuito hiberna | Confiabilidade |
| P-10 | Vercel Hobby proíbe uso comercial | Conformidade |
| P-12 | Planilha dos 296 clientes | Rodar a importação |

---

## 9. Dívidas técnicas conhecidas

| Dívida | Ação |
|---|---|
| Migration `V3` insere dados fictícios | Remover por nova migration antes de dado real (exige aprovação: DELETE) |
| Worker do Outbox assume **uma** instância do backend | Se escalar para 2+, usar `SELECT ... FOR UPDATE SKIP LOCKED` |
| Proteção contra força bruta fica em memória | Zera ao reiniciar; aceitável com uma instância |
| Token no `localStorage` | Risco de XSS documentado em D-22 |
| Pasta duplicada e obsoleta em `C:\Users\didit\OneDrive\Área de Trabalho\Valtis` | Apagar; o repositório ativo é `C:\dev\Valtis` |

---

## 10. Regras rígidas

Resumo. O texto que vale é o `AGENTS.md`.

### 🔴 Exige aprovação humana ANTES de executar (§10)

1. **Schema** — criar, renomear ou remover tabela/coluna; gerar ou rodar migration; alterar enum ou constraint
2. **Regra de negócio `RN-xx`** — periodicidade, cálculo de vencimento, janelas de 30/7 dias, classificação do painel
3. **Google e segredos** — credenciais, escopos OAuth, eventos em agenda real
4. **Dados e produção** — qualquer `DELETE` físico, importador sobre base real, deploy, variável de produção

### Código

- Domínio em português, infra em inglês (D-14)
- `any` proibido no TypeScript (modo estrito ligado)
- Sem Lombok; `record` para DTOs
- Datas em UTC no banco; "hoje" sempre pelo `Clock` injetado, nunca `LocalDate.now()` sem argumento
- Conventional Commits, referenciando o requisito
- Documentação atualizada **no mesmo commit** que muda comportamento

---

## 11. Armadilhas já pagas — não repita

| Sintoma | Causa | Solução |
|---|---|---|
| `UnknownHostException` no host do Supabase | Projeto hibernado | Restaurar em supabase.com |
| `SocketException: Network is unreachable` no servidor | Conexão direta do Supabase é IPv6-only | Usar **Session pooler** |
| 500 em consulta nativa com projeção | Postgres rebaixa apelido sem aspas | Apelidos entre aspas: `AS "valvulaId"` |
| CORS bloqueado | Origem não autorizada | `CORS_ORIGENS` **sem barra no final** |
| Variável mudada no Railway sem efeito | Ele nem sempre reimplanta | Deploy manual |
| Frontend chamando `localhost` em produção | `VITE_*` é gravada no build | Alterar variável **e** redeploy na Vercel |
| `function lower(bytea) does not exist` | Parâmetro de texto **nulo** numa query JPQL vai como bytea | Passar `""` e comparar com `''` |
| Corpo JSON recusado com `Cannot map null into type boolean` | Jackson 3 recusa campo primitivo ausente | Usar `Boolean` nos DTOs de entrada |
| Índice único de "um ciclo aberto" recusando o lançamento | Hibernate grava INSERT antes de UPDATE | `repository.flush()` depois de fechar, antes de abrir |
| `HttpStatus.UNPROCESSABLE_ENTITY` | Renomeado no Spring 7 | `UNPROCESSABLE_CONTENT` |

**Ao diagnosticar erro de Spring: a causa raiz está no ÚLTIMO `Caused by:` da pilha, não no primeiro.**

---

## 12. Sobre o Daniel — leia antes de despejar código

O Valtis tem **duplo objetivo**: resolver a dor da empresa **e** ser o portfólio de entrada do Daniel na área de tecnologia. Ele está aprendendo Java e Spring, dispõe de ~10h por semana, e quer **conseguir defender cada decisão em entrevista**.

- **Explique antes de construir.** Analogia do mundo real primeiro, depois o termo técnico
- **Um conceito por vez.** Aprofundar em regras de negócio, Outbox, segurança, JPA e transações; acelerar em CRUD, telas e configuração
- **Avise o erro antes de ele acontecer** quando for previsível
- Ele executa os comandos no terminal integrado do VS Code, em duas abas (backend e frontend)

A revisão técnica foi **adiada a pedido dele** (13/10/2026): ele vai estudar por uma apostila feita para isso (Claude Docs, "Apostila Valtis — conceitos e entrevista"), com questionário e gabarito.

---

## 13. Próxima tarefa sugerida

1. ~~Teste manual do Daniel na branch~~ — refeito em 16/10/2026: tudo funcionou. Ajustes pedidos e já feitos: editar/inativar/excluir válvula pelo técnico, data de hoje no lançamento, cartões do painel que não zeram ao filtrar, atrasados sem lembrete marcados na fila, tela Agenda com cartões que filtram a lista (como o painel). Testado e aprovado pelo Daniel.
2. ~~Validar S-01 a S-11 e D-21 a D-24~~ — feito em 16/10/2026.
3. **Daniel avaliar a nova interface** (17/10): rodar local e dizer o que ajustar. Crítica em `docs/design/critica-interface.md`.
4. **Testar a tela Importar com o Daniel** (ainda não testada por ele): começar com um CSV fictício, depois a planilha real **no banco local**.
5. **Começar a próxima sessão perguntando que outros ajustes ele quer**: ele disse que "vamos ajustar outras coisas ainda" antes de produção.
6. Lista "Base antiga, cadastro incompleto" (P-08).
7. Só depois: checklist de produção da §4, merge na `main`, credencial do Google.

**Rodar localmente:** o painel de visualização do app de desktop encerra os servidores depois de um tempo. Para o Daniel testar, suba backend e frontend no Terminal (duas abas), como na §3.
