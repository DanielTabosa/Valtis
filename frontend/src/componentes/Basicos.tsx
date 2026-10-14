import { useEffect, useRef, type ReactNode } from "react";
import type { StatusPainel } from "../api/tipos";
import { classeStatus } from "../util/status";

export function Carregando({ texto = "Carregando…" }: { texto?: string }) {
  return (
    <p className="aviso" role="status">
      {texto}
    </p>
  );
}

export function MensagemErro({ erro }: { erro: string | null }) {
  if (!erro) return null;
  return (
    <div className="aviso erro" role="alert">
      {erro}
    </div>
  );
}

export function MensagemSucesso({ texto }: { texto: string | null }) {
  if (!texto) return null;
  return (
    <div className="aviso sucesso" role="status">
      {texto}
    </div>
  );
}

export function EtiquetaStatus({ status, rotulo }: { status: StatusPainel; rotulo: string }) {
  return <span className={`etiqueta ${classeStatus(status)}`}>{rotulo}</span>;
}

export function Cabecalho({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="cabecalho-pagina">
      <h1>{titulo}</h1>
      {children && <div className="acoes">{children}</div>}
    </div>
  );
}

type PropsModal = {
  titulo: string;
  aberto: boolean;
  aoFechar: () => void;
  children: ReactNode;
};

/** Janela modal com o <dialog> nativo: foco, Esc e fundo escurecido já vêm do navegador. */
export function Modal({ titulo, aberto, aoFechar, children }: PropsModal) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (aberto && !dialogo.open) dialogo.showModal();
    if (!aberto && dialogo.open) dialogo.close();
  }, [aberto]);

  return (
    <dialog ref={ref} className="modal" onClose={aoFechar} aria-labelledby="titulo-modal">
      <div className="modal-topo">
        <h2 id="titulo-modal">{titulo}</h2>
        <button type="button" className="botao-texto" onClick={aoFechar} aria-label="Fechar">
          Fechar
        </button>
      </div>
      {aberto && children}
    </dialog>
  );
}

type PropsCampo = {
  rotulo: string;
  erro?: string;
  dica?: string;
  children: ReactNode;
};

export function Campo({ rotulo, erro, dica, children }: PropsCampo) {
  return (
    <label className={`campo ${erro ? "com-erro" : ""}`}>
      <span className="campo-rotulo">{rotulo}</span>
      {children}
      {dica && !erro && <span className="campo-dica">{dica}</span>}
      {erro && <span className="campo-erro">{erro}</span>}
    </label>
  );
}
