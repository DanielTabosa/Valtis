# Opções da tela de entrada (17/10/2026)

Galeria com as 4 propostas de login. **Escolhida: C · Mostrador**, já aplicada no app (`frontend/src/paginas/Login.tsx` e `componentes/Mostrador.tsx`).

| Opção | Ideia |
|---|---|
| A · Pressão Contida | Cartão de vidro sobre linhas de fluxo que se estreitam numa válvula |
| B · Isóbaras | Tela dividida: curvas de pressão e frase de valor à esquerda, formulário claro à direita |
| **C · Mostrador** | Formulário à esquerda; à direita, um manômetro do ciclo de 12 meses (verde → vermelho) |
| D · Planta técnica | Fundo claro com o esquema de uma estação redutora, cartão branco à direita |

Ver: abrir `index.html` por um servidor local (as imagens usam caminho relativo), por exemplo `python -m http.server 5180 --directory docs/design/opcoes-login`. Resumo visual em `capturas/comparativo.png`. As artes B e D saem de `artes.py` (Pillow).
