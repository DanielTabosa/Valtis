import { useState, type FormEvent } from "react";
import type { TipoBomba } from "../api/tipos";
import { tiposBomba } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, Modal } from "../componentes/Basicos";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/** Catálogo de tipos de bombeamento (RF-60). Só admin. */
export function TiposBomba() {
  const [incluirInativos, setIncluirInativos] = useState(false);
  const [editando, setEditando] = useState<TipoBomba | "novo" | null>(null);
  const { dados, carregando, erro, recarregar } = useCarga(() => tiposBomba.listar(incluirInativos), [incluirInativos]);
  const acao = useEnvio();

  async function alternar(t: TipoBomba) {
    const r = await acao.executar(() => (t.ativo ? tiposBomba.inativar(t.id) : tiposBomba.reativar(t.id)));
    if (r.ok) recarregar();
  }

  return (
    <>
      <Cabecalho titulo="Tipos de bombeamento">
        <button type="button" className="botao primario" onClick={() => setEditando("novo")}>
          + Novo tipo
        </button>
      </Cabecalho>
      <p className="secundario">
        Cada conjunto de bombeamento tem um tipo. Inativar um tipo tira-o da lista; os conjuntos que já o usam
        continuam como estão.
      </p>
      <label className="caixa">
        <input type="checkbox" checked={incluirInativos} onChange={(e) => setIncluirInativos(e.target.checked)} />
        Mostrar inativos
      </label>

      <MensagemErro erro={erro ?? acao.erro} />
      {carregando && !dados && <Carregando />}

      {dados && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Nome</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {dados.map((t) => (
              <tr key={t.id} className={t.ativo ? "" : "inativo"}>
                <td data-rotulo="Nome">
                  {t.nome}
                  {!t.ativo && <span className="selo">inativo</span>}
                </td>
                <td className="acoes-linha">
                  <button type="button" className="botao-texto" onClick={() => setEditando(t)}>
                    Renomear
                  </button>
                  <button type="button" className="botao-texto" disabled={acao.enviando} onClick={() => alternar(t)}>
                    {t.ativo ? "Inativar" : "Reativar"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal
        titulo={editando === "novo" ? "Novo tipo" : "Renomear tipo"}
        aberto={editando !== null}
        aoFechar={() => setEditando(null)}
      >
        <FormTipo
          tipo={editando === "novo" || editando === null ? undefined : editando}
          aoSalvar={() => {
            setEditando(null);
            recarregar();
          }}
        />
      </Modal>
    </>
  );
}

function FormTipo({ tipo, aoSalvar }: { tipo?: TipoBomba; aoSalvar: () => void }) {
  const [nome, setNome] = useState(tipo?.nome ?? "");
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const r = await executar(() => (tipo ? tiposBomba.renomear(tipo.id, nome) : tiposBomba.criar(nome)));
    if (r.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <Campo rotulo="Nome *" erro={campos.nome}>
        <input required autoFocus value={nome} onChange={(e) => setNome(e.target.value)} />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : tipo ? "Salvar" : "Cadastrar tipo"}
      </button>
    </form>
  );
}
