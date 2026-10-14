import { createContext, useContext } from "react";
import type { UsuarioSessao } from "../api/tipos";

export type EstadoSessao = {
  usuario: UsuarioSessao | null;
  /** true enquanto confere, ao abrir o app, se o token salvo ainda vale. */
  verificando: boolean;
  /** Havia sessão salva, mas o servidor não respondeu. A sessão continua guardada. */
  erroConexao: string | null;
  tentarDeNovo: () => void;
  entrar: (email: string, senha: string) => Promise<void>;
  sair: () => void;
  ehAdmin: boolean;
};

export const ContextoSessao = createContext<EstadoSessao | null>(null);

export function useSessao(): EstadoSessao {
  const contexto = useContext(ContextoSessao);
  if (!contexto) throw new Error("useSessao precisa estar dentro de ProvedorSessao.");
  return contexto;
}
