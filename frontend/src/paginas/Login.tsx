import { useState, type FormEvent } from "react";
import { Navigate } from "react-router";
import { Campo, MensagemErro } from "../componentes/Basicos";
import { useSessao } from "../sessao/useSessao";
import { useEnvio } from "../util/useEnvio";

export function Login() {
  const { usuario, entrar } = useSessao();
  const { enviando, erro, executar } = useEnvio();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");

  if (usuario) return <Navigate to="/" replace />;

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    await executar(() => entrar(email, senha));
  }

  return (
    <div className="tela-login">
      <form className="cartao-login" onSubmit={enviar}>
        <h1>Valtis</h1>
        <p className="secundario">Controle de manutenção de válvulas · Manutec</p>
        <MensagemErro erro={erro} />
        <Campo rotulo="E-mail">
          <input
            type="email"
            autoComplete="username"
            inputMode="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Campo>
        <Campo rotulo="Senha">
          <input
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
        </Campo>
        <button type="submit" className="botao primario largo" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </button>
      </form>
    </div>
  );
}
