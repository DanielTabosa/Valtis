import { useState, type FormEvent } from "react";
import { Navigate } from "react-router";
import { Campo, MensagemErro } from "../componentes/Basicos";
import { Marca } from "../componentes/Icone";
import { Mostrador } from "../componentes/Mostrador";
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
    <div className="tela-entrada">
      <div className="entrada-lado">
        <div className="marca-login">
          <Marca tamanho={48} />
          <h1>Valtis</h1>
        </div>
        <p className="secundario entrada-sub">
          O ciclo de manutenção de cada válvula redutora de pressão, acompanhado do serviço ao próximo retorno.
        </p>
        <form className="formulario" onSubmit={enviar}>
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
        <p className="login-rodape">MANUTEC VÁLVULAS · RECIFE</p>
      </div>
      <div className="entrada-palco">
        <Mostrador />
        <div className="mostrador-legenda" aria-hidden="true">
          <span className="em-dia">Em dia</span>
          <span className="proximo">Próximo</span>
          <span className="vencido">Vencido</span>
        </div>
      </div>
    </div>
  );
}
