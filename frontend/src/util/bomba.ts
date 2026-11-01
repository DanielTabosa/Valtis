import type { Bomba } from "../api/tipos";

/** Onde a bomba está, em uma linha. */
export function textoPosicao(b: Bomba): string {
  if (b.posicoes.length === 0) return b.patrimonioManutec ? "Disponível na Manutec" : "Fora de posição";
  return b.posicoes
    .map((p) => `${p.nomeCondominio} · ${p.conjunto} · pos. ${p.numeroPosicao}${p.natureza === "emprestimo" ? " (emprestada)" : ""}`)
    .join(" / ");
}
