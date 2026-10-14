import { useState } from "react";
import { ErroApi, mensagemDe } from "../api/http";

/**
 * Estado de um formulário sendo enviado: trava o botão, guarda a mensagem
 * de erro geral e os erros por campo que o servidor devolveu.
 */
export function useEnvio() {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [campos, setCampos] = useState<Record<string, string>>({});

  /** Devolve `ok: false` quando falhou; a mensagem já fica em `erro`. */
  async function executar<T>(acao: () => Promise<T>): Promise<{ ok: true; valor: T } | { ok: false }> {
    setEnviando(true);
    setErro(null);
    setCampos({});
    try {
      return { ok: true, valor: await acao() };
    } catch (e: unknown) {
      setErro(mensagemDe(e));
      if (e instanceof ErroApi) setCampos(e.campos);
      return { ok: false };
    } finally {
      setEnviando(false);
    }
  }

  function limpar() {
    setErro(null);
    setCampos({});
  }

  return { enviando, erro, campos, executar, limpar };
}
