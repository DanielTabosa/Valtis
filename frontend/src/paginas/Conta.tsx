import { useState, type FormEvent } from "react";
import { autenticacao } from "../api/valtis";
import { Cabecalho, Campo, MensagemErro, MensagemSucesso } from "../componentes/Basicos";
import { useSessao } from "../sessao/useSessao";
import { useEnvio } from "../util/useEnvio";

export function Conta() {
  const { usuario } = useSessao();
  const [atual, setAtual] = useState("");
  const [nova, setNova] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [sucesso, setSucesso] = useState<string | null>(null);
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setSucesso(null);
    const r = await executar(() => autenticacao.trocarSenha(atual, nova));
    if (r.ok) {
      setAtual("");
      setNova("");
      setConfirmacao("");
      setSucesso("Senha trocada.");
    }
  }

  const confere = nova === confirmacao;

  return (
    <>
      <Cabecalho titulo="Minha conta" />
      <dl className="ficha">
        <div>
          <dt>Nome</dt>
          <dd>{usuario?.nome}</dd>
        </div>
        <div>
          <dt>E-mail</dt>
          <dd>{usuario?.email}</dd>
        </div>
        <div>
          <dt>Perfil</dt>
          <dd>{usuario?.perfil === "admin" ? "Administrador" : "Técnico"}</dd>
        </div>
      </dl>

      <h2>Trocar senha</h2>
      <form className="formulario estreito" onSubmit={enviar}>
        <MensagemSucesso texto={sucesso} />
        <MensagemErro erro={erro} />
        <Campo rotulo="Senha atual" erro={campos.senhaAtual}>
          <input
            type="password"
            autoComplete="current-password"
            required
            value={atual}
            onChange={(e) => setAtual(e.target.value)}
          />
        </Campo>
        <Campo rotulo="Nova senha" erro={campos.novaSenha} dica="Mínimo de 8 caracteres.">
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={nova}
            onChange={(e) => setNova(e.target.value)}
          />
        </Campo>
        <Campo rotulo="Repita a nova senha" erro={confere ? undefined : "As senhas não conferem."}>
          <input
            type="password"
            autoComplete="new-password"
            required
            value={confirmacao}
            onChange={(e) => setConfirmacao(e.target.value)}
          />
        </Campo>
        <button type="submit" className="botao primario" disabled={enviando || !confere}>
          {enviando ? "Salvando…" : "Trocar senha"}
        </button>
      </form>
    </>
  );
}
