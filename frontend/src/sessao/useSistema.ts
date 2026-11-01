import { createContext, useContext } from "react";
import type { ModoCadastros } from "../api/tipos";

export type EstadoSistema = {
  /** Nulo enquanto não chegou a resposta de /api/sistema (ou sem sessão). */
  modo: ModoCadastros | null;
  /** Cadastros geridos fora (Manutec Clientes e Jiraya): as telas escondem o "criar". */
  externo: boolean;
};

export const ContextoSistema = createContext<EstadoSistema | null>(null);

export function useSistema(): EstadoSistema {
  const contexto = useContext(ContextoSistema);
  if (!contexto) throw new Error("useSistema precisa estar dentro de ProvedorSistema.");
  return contexto;
}
