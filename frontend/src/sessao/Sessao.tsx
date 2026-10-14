import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { guardarToken, lerToken, mensagemDe, registrarAoPerderSessao } from "../api/http";
import type { UsuarioSessao } from "../api/tipos";
import { autenticacao } from "../api/valtis";
import { ContextoSessao, type EstadoSessao } from "./useSessao";

/**
 * Guarda quem está logado. O token fica no localStorage para a sessão
 * sobreviver a fechar o navegador do celular (RF-02, D-22).
 */
export function ProvedorSessao({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSessao | null>(null);
  const [verificando, setVerificando] = useState(() => lerToken() !== null);
  const [erroConexao, setErroConexao] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);

  const sair = useCallback(() => {
    guardarToken(null);
    setErroConexao(null);
    setUsuario(null);
  }, []);

  useEffect(() => {
    registrarAoPerderSessao(sair);
  }, [sair]);

  // Ao abrir: se há token salvo, pergunta ao servidor quem é.
  // Token recusado (401) já chama sair() pelo cliente HTTP. Falta de conexão NÃO
  // desloga: o técnico sem sinal não pode perder a sessão por isso.
  useEffect(() => {
    if (!lerToken()) return;
    autenticacao
      .eu()
      .then(setUsuario)
      .catch((e: unknown) => {
        if (lerToken()) setErroConexao(mensagemDe(e));
      })
      .finally(() => setVerificando(false));
  }, [tentativa]);

  const entrar = useCallback(async (email: string, senha: string) => {
    const resposta = await autenticacao.entrar(email, senha);
    guardarToken(resposta.token);
    setErroConexao(null);
    setUsuario(resposta.usuario);
  }, []);

  const tentarDeNovo = useCallback(() => {
    setVerificando(true);
    setErroConexao(null);
    setTentativa((t) => t + 1);
  }, []);

  const valor = useMemo<EstadoSessao>(
    () => ({
      usuario,
      verificando,
      erroConexao,
      tentarDeNovo,
      entrar,
      sair,
      ehAdmin: usuario?.perfil === "admin",
    }),
    [usuario, verificando, erroConexao, tentarDeNovo, entrar, sair],
  );

  return <ContextoSessao.Provider value={valor}>{children}</ContextoSessao.Provider>;
}
