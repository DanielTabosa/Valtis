# Crítica de design — interface da Fase 1 (17/10/2026)

Feita sobre a branch `feat/fase-1-mvp` antes da revisão visual. Telas vistas: login, painel, lançamento, oportunidades, cadastros, agenda e importação.

## Impressão geral

A interface funciona e é honesta: alvos de toque de 44px, tabelas que viram cartões no celular, foco visível, `<dialog>` nativo. O que falta é **identidade e hierarquia**. Parece um formulário genérico e não um produto da Manutec. A maior oportunidade: deixar a situação das válvulas (vencido, próximo, em dia) saltar aos olhos, e tornar o caminho principal do técnico ("lançar serviço") sempre à mão.

## Usabilidade

| Achado | Severidade | Recomendação |
|---|---|---|
| No celular, a navegação fica atrás de um botão "Menu" com 8 itens em texto. O técnico precisa de dois toques para lançar um serviço | 🔴 Crítico | Barra inferior fixa com Painel, Oportunidades, **Lançar** (botão central destacado), Clientes e Mais |
| No desktop, 8 itens em linha no topo disputam espaço com o nome do usuário; em 1024px já aperta | 🟡 Moderado | Barra lateral fixa, agrupada por tarefa (Operação, Cadastros, Administração) |
| Itens de menu só com texto: leitura lenta, sem âncora visual | 🟡 Moderado | Ícones SVG de traço único ao lado de cada rótulo |
| Cartões do resumo não dizem que são clicáveis | 🟡 Moderado | Estado de hover/pressionado, ícone de status e anel de seleção |
| Carregamento é um texto "Carregando…" que empurra o layout | 🟢 Menor | Indicador animado discreto, respeitando `prefers-reduced-motion` |
| Tema segue só o sistema; não há como escolher | 🟢 Menor | Chave claro/escuro, lembrada no navegador |

## Hierarquia visual

- **O que puxa o olho primeiro:** a barra azul do topo e o botão "Lançar serviço". Deveria ser o resumo de situação (quantas válvulas vencidas).
- **Fluxo de leitura:** título → cartões → filtros → tabela. Correto, mas os cartões têm o mesmo peso visual da tabela.
- **Ênfase:** a cor de status aparece só numa borda de 5px e numa etiqueta pequena. Na tabela, códigos sublinhados em azul competem com o status.

## Consistência

| Elemento | Problema | Recomendação |
|---|---|---|
| Cor | Paleta própria (`#0f3d5e`, `#1667a6`) diferente da identidade Manutec usada no Manutec Clientes | Adotar os primitivos Manutec (`azul-900 #0e2a5c`, `azul-600 #1f5fbf`, gelo, tinta) |
| Tipografia | Fonte do sistema: muda entre Windows, Android e iPhone | Inter auto-hospedada (mesma do Manutec Clientes) com algarismos tabulares |
| Raio e sombra | Um único raio de 10px e nenhuma elevação: tudo parece do mesmo plano | Escala de raio (8/12/16) e três níveis de sombra |
| Espaçamento | Valores soltos (10, 14, 20, 28px) | Escala de 4px em variáveis |

## Acessibilidade

- **Contraste:** texto fraco `#5f6b78` sobre `#f5f7f9` ≈ 5,2:1 (passa). No escuro, `#95a1ae` sobre `#171d24` ≈ 6,9:1 (passa). Manter ≥ 4,5:1 na nova paleta.
- **Alvos de toque:** 44px em botões e campos (passa). Links na tabela têm altura de linha de texto: aumentar a área clicável.
- **Leitura:** corpo de 16px e entrelinha de 1,45 (passa). Rótulos de 0,75rem em maiúsculas ficam no limite: subir para 0,78rem com espaçamento.
- **Movimento:** hoje não há animação. As novas devem respeitar `prefers-reduced-motion`.

## O que funciona bem

- Mobile-first de verdade: tabelas viram cartões com rótulo à esquerda.
- O React não decide regra: status e prazo vêm prontos do servidor, então a revisão visual não toca em regra de negócio.
- Formulários com rótulo visível, dica e erro junto do campo.

## Prioridades

1. **Navegação por tarefa**: lateral no desktop, barra inferior com "Lançar" central no celular.
2. **Identidade Manutec, versão "futurista"**: azul-noite profundo, superfícies com leve translucidez, brilho ciano nos estados ativos, Inter + mono para códigos.
3. **Status como protagonista**: cartões de resumo com ícone, número grande, barra de proporção; ponto de status pulsando nos vencidos.
