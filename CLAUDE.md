# CLAUDE.md

As regras deste projeto estão em **[AGENTS.md](AGENTS.md)**. Leia-o por completo antes de qualquer alteração no repositório.

Ordem de leitura obrigatória:

1. `AGENTS.md` — regras de arquitetura, código, testes, Git, segurança e aprovação humana
2. `docs/requirements.md` — requisitos com IDs estáveis (RF, RNF, RN)
3. `docs/architecture.md` — camadas, fronteiras e padrão Outbox
4. `docs/database.md` — modelo de dados
5. `docs/decisions.md` — decisões e seus porquês (D-01 a D-19)

`_arquivo/` é histórico. **Não** é fonte de verdade.

⚠️ Quatro categorias exigem aprovação humana explícita antes de executar: alteração de schema, mudança de regra de negócio `RN-xx`, integração Google ou segredos, e exclusão de dados ou execução em produção. Detalhes em AGENTS.md §10.
