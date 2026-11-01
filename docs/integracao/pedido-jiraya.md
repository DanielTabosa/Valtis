# Pedido pronto para a sessão do Jiraya

Texto para o Daniel colar ao abrir uma sessão em `C:\dev\Jiraya` (item 0e.3 do `HANDOFF.md`). Escrito em 01/11/2026 a partir do contrato aprovado em 31/10. **O Valtis não altera nada no Jiraya:** quem implementa é a sessão de lá, seguindo as regras e o ritmo do próprio repositório (spec → backend ‖ telas → QA → `main`).

Conferido no Jiraya (só leitura, 01/11/2026): `main` com a E7a; próximo da fila é E7b → E8. As specs ficam em `docs/requirements/expansao-*.md`, as integrações em `docs/architecture/*-integration.md` (há `manutec-clientes-integration.md` como modelo) e o contrato HTTP em `docs/api/api-contract.md`.

---

## O texto

> Quero acrescentar ao Jiraya uma **API de leitura para o Valtis**, como uma etapa própria **antes da E8**. O contrato já foi escrito e aprovado por mim do lado do Valtis: é a versão **CV-v1**, no arquivo `C:\dev\Valtis\docs\integracao\jiraya.md`. Leia-o inteiro antes de começar. Ele é a fonte da verdade do formato; se algo nele não couber no Jiraya, **pare e me pergunte** em vez de adaptar, porque mudança no contrato vira nova versão do lado do Valtis.
>
> O que a etapa entrega:
>
> 1. **J-1, tipo de serviço na OS** (`tipo_servico`: `manutencao_preventiva`, `manutencao_corretiva`, `instalacao`, `visita_tecnica`, `orcamento`, `outro`). Obrigatório para concluir a OS; as OS antigas ficam sem valor.
> 2. **J-2, data de execução na OS** (`data_execucao`, data sem hora). Obrigatória para concluir, vem preenchida com hoje, editável. É o dia do serviço no cliente, não o clique em "concluir".
> 3. **J-3, `atualizado_em` confiável** em equipamento e OS: muda sempre que muda qualquer campo que a API devolve, inclusive nos dois equipamentos de uma substituição.
> 4. **Cinco rotas só de leitura** sob `/api/integracao/v1/`: tipos de equipamento, equipamento, equipamentos alterados, OS e OS alteradas. Token Bearer próprio da integração (não é login de usuário), cursor `desde` + `apos_id`, itens completos no feed, OS em todas as situações (inclusive reaberta e cancelada).
>
> Como quero que você conduza:
>
> - Escreva a spec da etapa em `docs/requirements/`, a arquitetura em `docs/architecture/valtis-integration.md` (no molde do `manutec-clientes-integration.md`), as rotas no `docs/api/api-contract.md` e a decisão registrada (próximo `D-xx` livre).
> - **Me mostre o schema** (J-1, J-2 e o que mais precisar) **antes de gerar a migração.**
> - Autorização por anotação no método, nunca por texto de URI (lição do BUG-330).
> - O patrimônio de bombas emprestadas **não** entra nesta versão.
> - Testes da API com o formato exato do contrato (nomes de campo, `null` onde o contrato diz `null`, ordenação e cursor).

---

## Depois que o Jiraya entregar

1. Gerar o token no Jiraya e configurar no Valtis local: `VALTIS_CADASTROS_ORIGEM=externa`, `VALTIS_JIRAYA_URL`, `VALTIS_JIRAYA_TOKEN` (e as do Manutec Clientes).
2. Na tela **Integração** (`/integracao`) do Valtis, "Ler agora" e conferir **Pendências** (`/integracao/pendencias`).
3. Se o Jiraya precisou mudar algo no contrato, registrar como nova versão em `jiraya.md` (seção "Histórico do contrato") e ajustar os leitores em `integracao/`.
