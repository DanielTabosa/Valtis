import { useState, type FormEvent } from "react";
import type { Perfil, Usuario } from "../api/tipos";
import { usuarios } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, MensagemSucesso, Modal } from "../componentes/Basicos";
import { useSessao } from "../sessao/useSessao";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

type Janela = { tipo: "novo" } | { tipo: "editar"; usuario: Usuario } | { tipo: "senha"; usuario: Usuario };

/** Gestão de usuários (RF-03). Só administrador. */
export function Usuarios() {
  const { usuario: eu } = useSessao();
  const { dados, carregando, erro, recarregar } = useCarga(() => usuarios.listar(), []);
  const [janela, setJanela] = useState<Janela | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useEnvio();

  async function alternar(u: Usuario) {
    if (u.ativo && !confirm(`Inativar ${u.nome}? O acesso é cortado na hora.`)) return;
    const r = await acao.executar(() => (u.ativo ? usuarios.inativar(u.id) : usuarios.reativar(u.id)));
    if (r.ok) recarregar();
  }

  function concluir(mensagem: string) {
    setJanela(null);
    setAviso(mensagem);
    recarregar();
  }

  return (
    <>
      <Cabecalho titulo="Usuários">
        <button type="button" className="botao primario" onClick={() => setJanela({ tipo: "novo" })}>
          + Novo usuário
        </button>
      </Cabecalho>
      <MensagemSucesso texto={aviso} />
      <MensagemErro erro={erro ?? acao.erro} />
      {carregando && !dados && <Carregando />}

      {dados && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Nome</th>
              <th>E-mail</th>
              <th>Perfil</th>
              <th>Situação</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {dados.map((u) => (
              <tr key={u.id} className={u.ativo ? "" : "inativo"}>
                <td data-rotulo="Nome">{u.nome}</td>
                <td data-rotulo="E-mail">{u.email}</td>
                <td data-rotulo="Perfil">{u.perfil === "admin" ? "Administrador" : "Técnico"}</td>
                <td data-rotulo="Situação">{u.ativo ? "Ativo" : "Inativo"}</td>
                <td className="acoes-linha">
                  <button
                    type="button"
                    className="botao-texto"
                    onClick={() => setJanela({ tipo: "editar", usuario: u })}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    className="botao-texto"
                    onClick={() => setJanela({ tipo: "senha", usuario: u })}
                  >
                    Nova senha
                  </button>
                  {u.id !== eu?.id && (
                    <button type="button" className="botao-texto" disabled={acao.enviando} onClick={() => alternar(u)}>
                      {u.ativo ? "Inativar" : "Reativar"}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal titulo="Novo usuário" aberto={janela?.tipo === "novo"} aoFechar={() => setJanela(null)}>
        <FormUsuario aoSalvar={() => concluir("Usuário criado. Passe a senha para a pessoa por um canal seguro.")} />
      </Modal>
      <Modal titulo="Editar usuário" aberto={janela?.tipo === "editar"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "editar" && (
          <FormUsuario usuario={janela.usuario} aoSalvar={() => concluir("Usuário atualizado.")} />
        )}
      </Modal>
      <Modal titulo="Definir nova senha" aberto={janela?.tipo === "senha"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "senha" && (
          <FormNovaSenha usuario={janela.usuario} aoSalvar={() => concluir("Senha redefinida.")} />
        )}
      </Modal>
    </>
  );
}

function FormUsuario({ usuario, aoSalvar }: { usuario?: Usuario; aoSalvar: () => void }) {
  const [nome, setNome] = useState(usuario?.nome ?? "");
  const [email, setEmail] = useState(usuario?.email ?? "");
  const [perfil, setPerfil] = useState<Perfil>(usuario?.perfil ?? "tecnico");
  const [senha, setSenha] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const r = await executar(() =>
      usuario ? usuarios.atualizar(usuario.id, nome, perfil) : usuarios.criar(nome, email, perfil, senha),
    );
    if (r.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <Campo rotulo="Nome *" erro={campos.nome}>
        <input required value={nome} onChange={(e) => setNome(e.target.value)} />
      </Campo>
      <Campo rotulo="E-mail *" erro={campos.email} dica={usuario ? "O e-mail não muda depois de criado." : undefined}>
        <input
          type="email"
          required
          disabled={usuario !== undefined}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </Campo>
      <Campo rotulo="Perfil *" erro={campos.perfil}>
        <select value={perfil} onChange={(e) => setPerfil(e.target.value as Perfil)}>
          <option value="tecnico">Técnico: lança serviços, cadastra e trabalha a fila</option>
          <option value="admin">Administrador: tudo, mais inativar e gerenciar usuários</option>
        </select>
      </Campo>
      {!usuario && (
        <Campo rotulo="Senha inicial *" erro={campos.senha} dica="Mínimo de 8 caracteres.">
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
        </Campo>
      )}
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : usuario ? "Salvar" : "Criar usuário"}
      </button>
    </form>
  );
}

function FormNovaSenha({ usuario, aoSalvar }: { usuario: Usuario; aoSalvar: () => void }) {
  const [senha, setSenha] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const r = await executar(() => usuarios.redefinirSenha(usuario.id, senha));
    if (r.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <p>
        Nova senha para <strong>{usuario.nome}</strong>.
      </p>
      <MensagemErro erro={erro} />
      <Campo rotulo="Nova senha *" erro={campos.senha} dica="Mínimo de 8 caracteres.">
        <input
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
        />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : "Definir senha"}
      </button>
    </form>
  );
}
