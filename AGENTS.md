# AGENTS.md — Regras de trabalho no projeto Valtis

Este arquivo é vinculante para **qualquer agente** que altere este repositório. Leia antes de agir, não depois.

Se uma regra daqui conflitar com uma instrução do usuário, **a instrução do usuário vence** — mas avise que está contrariando o AGENTS.md e por quê.

---

## 1. Contexto mínimo

Valtis é um sistema web de controle de manutenção de VRPs para a Manutec Válvulas. Registra manutenções executadas em campo e agenda o retorno de 12 meses no Google Calendar. Três usuários. Volume de ~100 atendimentos/ano.

O sistema lida com **dados reais de 296 clientes** e com a agenda de trabalho de pessoas reais. Erro aqui não é bug de demonstração: é uma manutenção que não acontece ou um cliente contatado indevidamente.

## 2. Ordem de leitura obrigatória

Antes de escrever a primeira linha de qualquer tarefa:

1. Este arquivo.
2. `docs/requirements.md` — localize o **RF/RNF/RN** que a tarefa atende. Se não existir, pare e pergunte (ver §10).
3. `docs/architecture.md` — confirme em qual camada a mudança pertence.
4. `docs/database.md` — se a tarefa toca dados.
5. `docs/decisions.md` — se a tarefa parece contrariar alguma decisão registrada.

Nunca use `_arquivo/` como fonte de verdade. É histórico.

## 3. Arquitetura

Detalhe completo em `docs/architecture.md`. As fronteiras invioláveis:

- **Casos de uso não conhecem infraestrutura.** Nenhum arquivo da camada de casos de uso importa `googleapis`, `@prisma/client`, SDK de storage ou `fetch`. Eles falam com interfaces; os adaptadores implementam.
- **Regra de negócio não mora na tela.** Se há um `if` decidindo status, vencimento ou elegibilidade dentro de um componente React, está no lugar errado.
- **Nenhuma chamada externa dentro da transação de escrita.** Integração com o Google acontece via **padrão Outbox** (`docs/architecture.md#outbox`). Salvar um serviço **jamais** pode falhar porque a API do Google falhou.
- **Monolito modular.** Não crie serviços separados, filas dedicadas, cache distribuído ou containers extras. O volume não justifica e a manutenção é de uma pessoa.
- Ao adicionar dependência nova: justifique no PR. A resposta padrão para "vale a pena instalar isso?" é **não**.

## 4. Padrões de código

### Idioma — regra fixa (D-14)

| Camada | Idioma | Exemplo |
|---|---|---|
| Tabelas, colunas, enums | **Português** | `valvula`, `numero_valvula`, `requer_fechamento_geral` |
| Entidades e casos de uso | **Português** | `Estacao`, `lancarServico`, `gerarOportunidade` |
| Infra, utilitários, componentes genéricos | **Inglês** | `findById`, `formatDate`, `Button`, `useDebounce` |
| Commits e branches | **Inglês** | `feat: add estacao entity` |

Termos de domínio **não se traduzem**. `sindico` não vira `manager`; `barrilete` e `shaft` não têm equivalente útil em inglês. Traduzir cria uma camada de tradução mental entre o que a equipe fala e o que o código diz — é onde nascem os erros de regra de negócio.

### Geral

- TypeScript em modo estrito. **`any` é proibido**; use `unknown` e refine.
- Sem números mágicos. `12` (periodicidade), `30` e `7` (janelas de alerta) são configuração, não literal espalhado.
- Erro se trata ou se propaga tipado. **Nunca** `catch` vazio nem `catch` que só loga e segue.
- Funções pequenas e com um propósito. Se precisa de comentário explicando *o que* faz, o nome está errado.
- Comentários explicam **por quê**, não o quê — e sempre em português.
- Nada de código morto, mock esquecido ou `console.log` no commit.
- Datas: armazenar em UTC, exibir em horário de Brasília. Nunca comparar datas como string.

## 5. Testes

Nem tudo precisa de teste. **Isto precisa, sem exceção:**

1. **Toda regra de negócio `RN-xx`** — cálculo de vencimento, classificação do painel, geração e fechamento de oportunidade, fluxo de recusa.
2. **O Outbox** — inclusive os caminhos de falha: API fora do ar, token revogado, retentativa, evento duplicado.
3. **O importador** — deduplicação, data aproximada, ausência de evento retroativo.
4. **Qualquer bug corrigido** — o teste que reproduz o bug vem antes da correção.

Não escreva teste para getters, componentes de layout ou wrappers triviais. Cobertura alta com testes vazios é pior que cobertura baixa: dá falsa confiança.

Testes usam dados fictícios. **Nunca** aponte teste para a base real nem para a agenda real do Google.

## 6. Git

- **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`.
- Uma mudança lógica por commit. Refatoração e mudança de comportamento **não** vão no mesmo commit.
- Mensagem referencia o requisito: `feat: calcula vencimento por estacao (RF-23, RN-01)`.
- Branch por requisito: `feat/rf-23-calculo-vencimento`.
- Não reescreva histórico já publicado. Não force push em `main`.
- Não commite `.env`, credenciais, tokens, dumps de banco ou planilha com dado real de cliente.

## 7. Documentação

**Mudou comportamento, atualiza o documento no mesmo commit.** Código e documentação divergentes são piores que documentação ausente — o próximo agente confia na versão errada.

- Requisito novo ou alterado → `docs/requirements.md`, com ID novo. **Não reutilize ID aposentado.**
- Mudança estrutural → `docs/architecture.md`.
- Mudança de schema → `docs/database.md`.
- Escolha relevante com alternativas descartadas → **nova entrada** em `docs/decisions.md`.

`docs/decisions.md` é **append-only**. Decisão errada não se apaga: cria-se outra que a substitui, e a antiga passa a `Substituída por D-xx`. O histórico do raciocínio é o que impede o mesmo erro duas vezes.

## 8. Segurança

- Segredo nenhum no código, em teste ou em log. Sempre variável de ambiente.
- O **refresh token do Google** é o segredo mais crítico do sistema. Cifrado, fora do repositório, com alerta se for revogado.
- Escopo OAuth mínimo: apenas gerenciamento de eventos da agenda de manutenções. Não peça acesso a Gmail, Drive ou contatos.
- **LGPD:** a base contém nome, telefone e e-mail de ~296 síndicos e administradoras. Não exponha em log, em mensagem de erro, em URL nem em tela pública. Não envie dado real para serviço de terceiro.
- Nenhuma rota sem autenticação, exceto a tela de login.
- Exclusão é sempre **lógica** (`ativo = false`). `DELETE` físico exige aprovação (§10).
- Toda entrada é validada no servidor, não só no formulário.

## 9. Alteração de funcionalidade existente

Antes de mudar algo que já funciona:

1. **Descubra por que existe.** Localize o `RF/RN` correspondente e a decisão `D-xx` que o originou.
2. **Se a mudança contraria uma decisão registrada**, pare. Isso é §10.
3. **Não "melhore" em silêncio.** Renomear, reorganizar ou reescrever algo que não faz parte da tarefa é ruído — dificulta revisão e esconde a mudança real no meio do diff.
4. **Preserve o comportamento não solicitado.** Se a tarefa é ajustar a janela de alerta, não aproveite para mudar o formato da data.
5. **Comportamento removido é documentado.** Diga o que deixou de existir e por quê.

Comentário estranho, verificação aparentemente redundante ou caso especial esquisito geralmente existem por um motivo que não está óbvio. Investigue antes de remover.

## 10. 🔴 Aprovação humana obrigatória

**Pare e pergunte ao Daniel antes de executar.** Não é sugestão: é bloqueio.

| Categoria | Exemplos |
|---|---|
| **Schema do banco** | Criar, renomear ou remover tabela ou coluna. Gerar ou rodar migração. Alterar enum ou constraint |
| **Regra de negócio (RN-xx)** | Periodicidade, cálculo de vencimento, janelas de 30 e 7 dias, classificação do painel, geração ou fechamento de oportunidade |
| **Google e segredos** | Credenciais, escopos OAuth, criação ou exclusão de eventos em agenda real, mudança na conta dona da agenda |
| **Dados e produção** | Qualquer `DELETE` físico, execução do importador sobre a base real, deploy para produção, alteração de variável de ambiente de produção |

Também pare e pergunte quando:

- A tarefa não corresponde a nenhum `RF/RNF/RN` existente — pode ser escopo novo disfarçado de ajuste.
- Duas fontes de verdade se contradizem.
- A solução exige contrariar uma regra deste arquivo.
- Você precisou **supor** uma regra de negócio para prosseguir. Suposição sobre o negócio de outra pessoa é chute; pergunte.

Ao pedir aprovação, apresente: o que pretende fazer, por que é necessário, o que quebra se der errado, e a alternativa que você descartou.

## 11. Ao encerrar uma tarefa

- [ ] A mudança atende a um `RF/RNF/RN` identificado
- [ ] Regras de negócio tocadas têm teste
- [ ] Documentação atualizada no mesmo commit
- [ ] Nenhum segredo, dado real de cliente ou `console.log` no diff
- [ ] Nada fora do escopo da tarefa foi alterado
- [ ] Itens do §10 foram aprovados, não apenas comunicados
