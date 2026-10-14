import type { StatusPainel } from "../api/tipos";

/** Classe CSS de cada situação. A cor segue o código que o servidor calculou; a tela não decide nada. */
const CLASSE_STATUS: Record<StatusPainel, string> = {
  VENCIDO: "vencido",
  PROXIMO_DO_VENCIMENTO: "proximo",
  SEM_REGISTRO: "sem-registro",
  EM_DIA: "em-dia",
};

export function classeStatus(status: StatusPainel): string {
  return CLASSE_STATUS[status];
}
