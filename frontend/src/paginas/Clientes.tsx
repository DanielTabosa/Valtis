import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { clientes } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro, Modal, NotaGerido } from "../componentes/Basicos";
import { FormCliente } from "../componentes/Formularios";
import { useSistema } from "../sessao/useSistema";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";

/** Cadastro de clientes (RF-05, RF-11). */
export function Clientes() {
  const navegar = useNavigate();
  const { externo } = useSistema();
  const [busca, setBusca] = useState("");
  const [incluirInativos, setIncluirInativos] = useState(false);
  const [criando, setCriando] = useState(false);
  const buscaAtrasada = useAtrasado(busca);
  const { dados, carregando, erro } = useCarga(
    () => clientes.listar(buscaAtrasada, incluirInativos),
    [buscaAtrasada, incluirInativos],
  );

  return (
    <>
      <Cabecalho titulo="Clientes">
        {externo ? (
          <NotaGerido oQue="Novos clientes" fonte="manutec_clientes" />
        ) : (
          <button type="button" className="botao primario" onClick={() => setCriando(true)}>
            + Novo cliente
          </button>
        )}
      </Cabecalho>

      <div className="filtros">
        <input
          type="search"
          placeholder="Nome, código, bairro ou cidade"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          aria-label="Buscar cliente"
        />
        <label className="caixa">
          <input type="checkbox" checked={incluirInativos} onChange={(e) => setIncluirInativos(e.target.checked)} />
          Mostrar inativos
        </label>
      </div>

      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}
      {dados?.length === 0 && <p className="aviso">Nenhum cliente encontrado.</p>}

      {dados && dados.length > 0 && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Código</th>
              <th>Condomínio</th>
              <th>Bairro</th>
              <th>Atendimento</th>
              <th>Situação</th>
            </tr>
          </thead>
          <tbody>
            {dados.map((c) => (
              <tr key={c.id} className={c.ativo ? "" : "inativo"}>
                <td data-rotulo="Código" className="mono">
                  {c.codigoReferencia}
                </td>
                <td data-rotulo="Condomínio">
                  <Link to={`/clientes/${c.id}`}>{c.nomeCondominio}</Link>
                  {c.cadastroIncompleto && <span className="selo">cadastro incompleto</span>}
                </td>
                <td data-rotulo="Bairro">{c.bairro ?? "—"}</td>
                <td data-rotulo="Atendimento">{c.tipoAtendimento}</td>
                <td data-rotulo="Situação">{c.ativo ? c.statusContrato : "Inativo no sistema"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!externo && (
        <Modal titulo="Novo cliente" aberto={criando} aoFechar={() => setCriando(false)}>
          <FormCliente aoSalvar={(c) => navegar(`/clientes/${c.id}`)} />
        </Modal>
      )}
    </>
  );
}
