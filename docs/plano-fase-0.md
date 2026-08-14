# Plano da Fase 0 — Fundação

**Objetivo:** provar que o encanamento todo funciona, de ponta a ponta, **antes** de escrever qualquer regra de negócio.

**Duração estimada:** ~25h · 3 semanas a 10h/semana
**Marco final:** um evento criado pelo sistema aparece na agenda do celular das 3 pessoas.

> **Por que esta fase existe.** A parte mais arriscada do Valtis não é o CRUD — é a integração com o Google Calendar, que tem armadilhas de configuração que só aparecem quando você tenta. Descobrir isso na semana 3 é barato. Descobrir na semana 14, com metade do sistema pronto, é caro.

---

## ⛔ O que NÃO fazer nesta fase

Resistir à tentação é parte do exercício:

- Não criar as entidades do domínio (`cliente`, `estacao`, `valvula`, `servico`)
- Não modelar o banco de verdade
- Não desenhar telas do sistema
- Não escrever regra de negócio

Fase 0 é **só infraestrutura**. Uma tabela de teste e um endpoint bobo bastam.

---

## Semana 1 — Backend rodando e falando com o banco (~10h)

| # | Tarefa | Est. |
|---|---|---|
| 1.1 | Instalar **JDK 21**, **Maven** e uma IDE (IntelliJ IDEA Community é o padrão do mercado Java) | 1h |
| 1.2 | Gerar o projeto em `start.spring.io` — dependências: *Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Validation* | 1h |
| 1.3 | Subir local e criar um endpoint `GET /api/health` que responda `{"status":"ok"}` | 2h |
| 1.4 | Provisionar um PostgreSQL gerenciado (Neon, Supabase ou Railway) e conectar via `application.yml` | 2h |
| 1.5 | Primeira migration Flyway: uma tabela `teste_conexao` qualquer, só para ver o Flyway rodar | 2h |
| 1.6 | Um endpoint que lê essa tabela e devolve o conteúdo | 2h |

**Conceitos para estudar em paralelo** *(o essencial para um júnior)*:

- **Injeção de dependência e IoC** — por que você nunca dá `new` num service no Spring
- **`@RestController`, `@Service`, `@Repository`** — o que cada anotação significa e por que a separação existe
- **`application.yml` e profiles** — como a configuração muda entre local e produção
- **O que é um ORM** — o que o Hibernate faz por você e o que ele esconde
- **Por que migrations existem** — e por que `ddl-auto: update` é armadilha em produção

> **Decisão de aprendizado:** não use Lombok no começo. Escreva os getters e setters na mão por algumas semanas. Lombok economiza digitação, mas esconde o que a linguagem está fazendo — e você está aqui para ver. Para DTOs, use **`record`** do Java 21, que é nativo.

**Armadilhas conhecidas:** a URL de conexão do Postgres gerenciado geralmente exige `sslmode=require`. Se o Flyway reclamar de "schema not empty", é porque o `ddl-auto` do Hibernate criou tabela antes — deixe `ddl-auto: validate`.

**Concluído quando:** você chama `GET /api/health` no navegador e vê a resposta; e o segundo endpoint devolve dados vindos do banco de verdade.

---

## Semana 2 — Deploy e frontend no ar (~10h)

| # | Tarefa | Est. |
|---|---|---|
| 2.1 | `Dockerfile` do backend e deploy no **Railway** ou **Render** | 3h |
| 2.2 | Configurar variáveis de ambiente no provedor — nada de senha no código | 1h |
| 2.3 | Criar o frontend: `npm create vite@latest` com React + TypeScript. Deploy na **Vercel** | 2h |
| 2.4 | Fazer o frontend chamar o `/api/health` do backend publicado | 3h |
| 2.5 | Commits organizados no padrão do `AGENTS.md` e push | 1h |

**Conceitos para estudar em paralelo:**

- **Container e imagem** — o que o Docker resolve e por que o deploy pede isso
- **CORS** — você **vai** esbarrar nisso na tarefa 2.4. Entenda antes de copiar a solução: por que o navegador bloqueia, e por que a permissão é dada pelo servidor
- **Variável de ambiente vs arquivo de configuração** — e por que segredo nunca vai para o Git
- **Build estático vs aplicação servidora** — por que o React vai para a Vercel e o Java não

**Armadilhas conhecidas:** o Railway atribui a porta por variável de ambiente — o Spring precisa ler `PORT`, não fixar 8080. E o primeiro erro de CORS parece erro de backend, mas é o navegador barrando; leia a mensagem no console com calma.

**Concluído quando:** existe uma URL pública do frontend que exibe dados vindos de uma URL pública do backend, que por sua vez lê de um banco na nuvem. **Nesse momento a arquitetura inteira está provada.**

---

## Semana 3 — Google Calendar (~10h)

A parte mais arriscada. Se algo desandar, é aqui.

| # | Tarefa | Est. |
|---|---|---|
| 3.1 | Definir a **conta Gmail institucional** da Manutec, criar a agenda "Manutenções VRP" e compartilhar com os 3 usuários com permissão de alteração | 1h |
| 3.2 | Criar projeto no Google Cloud Console e ativar a **Google Calendar API** | 1h |
| 3.3 | Configurar a tela de consentimento OAuth e **publicar o app como "Em produção"** | 2h |
| 3.4 | Executar o fluxo OAuth uma vez, obter o **refresh token** e guardá-lo como variável de ambiente | 3h |
| 3.5 | Criar um evento de teste pela API, com os 3 como convidados | 2h |
| 3.6 | Guardar as credenciais num cofre de senhas acessível a mais de uma pessoa | 1h |

**Conceitos para estudar em paralelo:**

- **OAuth2, fluxo *authorization code*** — a diferença entre *access token* (curto) e *refresh token* (longo), e por que existem dois
- **Escopos** — por que pedir o mínimo necessário
- **Conta de serviço vs OAuth de usuário** — entender por que a conta de serviço **não** serve aqui ([decisions.md](decisions.md) · D-01)

**Armadilhas conhecidas — leia antes de começar:**

- Em modo *Testing*, o **refresh token expira em poucos dias**. Publicar o app em produção não é opcional (D-01).
- O app não será verificado pelo Google. Vai aparecer uma tela de aviso na autorização. É esperado.
- O `redirect_uri` precisa bater **exatamente** com o registrado no Console — incluindo `http` vs `https` e barra final.
- Convidado só recebe notificação se a chamada pedir explicitamente o envio de atualizações.

**Concluído quando:** você roda o programa, e o evento aparece na agenda do seu celular e no dos outros dois.

---

## Checklist de encerramento da Fase 0

- [ ] Backend Spring Boot publicado e acessível por URL
- [ ] Frontend React publicado e consumindo o backend
- [ ] PostgreSQL na nuvem, com Flyway aplicando migrations
- [ ] Nenhum segredo no repositório — tudo em variável de ambiente
- [ ] App OAuth **publicado em produção** no Google Cloud
- [ ] Refresh token obtido, guardado e testado
- [ ] Evento de teste criado pelo sistema, visível nos 3 celulares
- [ ] Credenciais no cofre, acessíveis a mais de uma pessoa
- [ ] Commits no padrão do `AGENTS.md`, com histórico limpo

---

## Depois da Fase 0

A próxima meta é a **fatia vertical** (~30h, por volta da semana 6): um fluxo completo no ar — cadastrar uma válvula, lançar um serviço, ver o vencimento no painel.

Ela ensina a stack inteira de uma vez e já produz algo demonstrável, bem antes de o sistema estar pronto. Só depois disso vale abrir o escopo completo da Fase 1.

---

## Nota sobre estudar e construir ao mesmo tempo

Nas primeiras semanas a proporção vai parecer ruim — muito tempo lendo, pouco código saindo. Isso é normal e melhora rápido.

Uma sugestão de método: **construa primeiro, entenda depois, mas no mesmo dia.** Faça a tarefa funcionar, e antes de fechar o computador volte e responda para si mesmo *por que* funcionou. Conceito estudado sem código na frente evapora; código que funcionou sem entendimento vira dívida na entrevista.
