// Formatação para exibição. Nenhuma regra de negócio aqui: status e prazos
// já chegam calculados do servidor (AGENTS.md §3).

/** "2026-07-03" → "03/07/2026". Corta o texto em vez de usar Date, que mudaria o dia pelo fuso. */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [ano, mes, dia] = iso.slice(0, 10).split("-");
  return `${dia}/${mes}/${ano}`;
}

/** Data e hora de um instante (timestamptz), no horário de Brasília. */
export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("pt-BR", {
    timeZone: "America/Recife",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function textoPrazo(dias: number | null | undefined): string {
  if (dias === null || dias === undefined) return "—";
  if (dias < 0) return `${Math.abs(dias)} ${Math.abs(dias) === 1 ? "dia" : "dias"} em atraso`;
  if (dias === 0) return "vence hoje";
  return `em ${dias} ${dias === 1 ? "dia" : "dias"}`;
}

/** Hoje no calendário de Recife, como "AAAA-MM-DD", para o valor padrão de campos de data. */
export function hojeEmRecife(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Recife" }).format(new Date());
}

export const ROTULO_OPORTUNIDADE: Record<string, string> = {
  agendado: "Agendado",
  contatado: "Contatado",
  aceito: "Aceito",
  recusado: "Recusado",
  executado: "Executado",
  perdido: "Perdido",
};

export const ROTULO_EVENTO: Record<string, string> = {
  alerta_comercial: "Contato comercial",
  lembrete_final: "Lembrete final",
  pendente: "Pendente",
  criado: "Na agenda",
  falhou: "Falhou",
  descartado: "Descartado",
};

/** Texto vazio vira null, para o servidor não gravar "" (o backend também limpa). */
export function textoOuNulo(texto: string): string | null {
  const limpo = texto.trim();
  return limpo === "" ? null : limpo;
}

/** Converte o valor de um campo numérico; vazio ou inválido vira null. */
export function numeroOuNulo(texto: string): number | null {
  if (texto.trim() === "") return null;
  const numero = Number(texto.replace(",", "."));
  return Number.isFinite(numero) ? numero : null;
}
