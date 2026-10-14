import { useState } from "react";
import type { Especificacao } from "../api/tipos";
import { especificacoes } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro, Modal } from "../componentes/Basicos";
import { FormEspecificacao } from "../componentes/Formularios";
import { useSessao } from "../sessao/useSessao";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/** Catálogo de especificações técnicas (RF-06). */
export function Especificacoes() {
  const { ehAdmin } = useSessao();
  const [incluirInativas, setIncluirInativas] = useState(false);
  const [editando, setEditando] = useState<Especificacao | "nova" | null>(null);
  const { dados, carregando, erro, recarregar } = useCarga(
    () => especificacoes.listar(incluirInativas),
    [incluirInativas],
  );
  const acao = useEnvio();

  async function alternar(e: Especificacao) {
    const r = await acao.executar(() => (e.ativo ? especificacoes.inativar(e.id) : especificacoes.reativar(e.id)));
    if (r.ok) recarregar();
  }

  return (
    <>
      <Cabecalho titulo="Especificações">
        <button type="button" className="botao primario" onClick={() => setEditando("nova")}>
          + Nova especificação
        </button>
      </Cabecalho>
      <p className="secundario">
        Cada combinação de marca, modelo, diâmetro e registro é cadastrada uma vez e reaproveitada pelas válvulas.
      </p>
      <label className="caixa">
        <input type="checkbox" checked={incluirInativas} onChange={(e) => setIncluirInativas(e.target.checked)} />
        Mostrar inativas
      </label>

      <MensagemErro erro={erro ?? acao.erro} />
      {carregando && !dados && <Carregando />}

      {dados && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Descrição</th>
              <th>Marca</th>
              <th>Modelo</th>
              <th>Diâmetro</th>
              <th>Registro</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {dados.map((e) => (
              <tr key={e.id} className={e.ativo ? "" : "inativo"}>
                <td data-rotulo="Descrição">{e.descricaoCompleta}</td>
                <td data-rotulo="Marca">{e.marca}</td>
                <td data-rotulo="Modelo">{e.modelo}</td>
                <td data-rotulo="Diâmetro">{e.diametroPolegadas}"</td>
                <td data-rotulo="Registro">{e.tipoRegistro}</td>
                <td className="acoes-linha">
                  <button type="button" className="botao-texto" onClick={() => setEditando(e)}>
                    Editar
                  </button>
                  {ehAdmin && (
                    <button type="button" className="botao-texto" disabled={acao.enviando} onClick={() => alternar(e)}>
                      {e.ativo ? "Inativar" : "Reativar"}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal
        titulo={editando === "nova" ? "Nova especificação" : "Editar especificação"}
        aberto={editando !== null}
        aoFechar={() => setEditando(null)}
      >
        <FormEspecificacao
          especificacao={editando === "nova" || editando === null ? undefined : editando}
          aoSalvar={() => {
            setEditando(null);
            recarregar();
          }}
        />
      </Modal>
    </>
  );
}
