import { useState } from "react";
import { Link } from "react-router";
import type { StatusPainel } from "../api/tipos";
import { painel } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro } from "../componentes/Basicos";
import { classeStatus } from "../util/status";
import { formatarData, textoPrazo } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";

/** Painel de status (RF-34 a RF-39): uma linha por válvula ativa, calculado no servidor. */
export function Painel() {
  const [busca, setBusca] = useState("");
  const [status, setStatus] = useState<StatusPainel | "">("");
  const [tipoAtendimento, setTipoAtendimento] = useState("");
  const buscaAtrasada = useAtrasado(busca);

  const { dados, carregando, erro } = useCarga(
    () =>
      painel.montar({
        busca: buscaAtrasada,
        status: status || undefined,
        tipoAtendimento,
      }),
    [buscaAtrasada, status, tipoAtendimento],
  );

  return (
    <>
      <Cabecalho titulo="Painel de válvulas">
        <Link to="/lancar" className="botao primario">
          Lançar serviço
        </Link>
      </Cabecalho>

      {dados && (
        <section className="resumo" aria-label="Resumo por situação">
          {dados.resumo.map((r) => (
            <button
              key={r.status}
              type="button"
              className={`cartao-resumo ${classeStatus(r.status)} ${status === r.status ? "selecionado" : ""}`}
              aria-pressed={status === r.status}
              onClick={() => setStatus(status === r.status ? "" : r.status)}
            >
              <span className="numero">{r.total}</span>
              <span className="rotulo">{r.rotulo}</span>
            </button>
          ))}
        </section>
      )}

      <div className="filtros">
        <input
          type="search"
          placeholder="Buscar condomínio, código, bairro ou nº de série"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          aria-label="Buscar"
        />
        <select value={tipoAtendimento} onChange={(e) => setTipoAtendimento(e.target.value)} aria-label="Atendimento">
          <option value="">Contrato e avulso</option>
          <option value="Contrato">Só contrato</option>
          <option value="Avulso">Só avulso</option>
        </select>
        {status && (
          <button type="button" className="botao-texto" onClick={() => setStatus("")}>
            Limpar situação
          </button>
        )}
      </div>

      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}

      {dados && dados.linhas.length === 0 && <p className="aviso">Nenhuma válvula encontrada com esses filtros.</p>}

      {dados && dados.linhas.length > 0 && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Situação</th>
              <th>Código</th>
              <th>Condomínio</th>
              <th>Estação</th>
              <th>Última</th>
              <th>Próxima</th>
              <th>Prazo</th>
            </tr>
          </thead>
          <tbody>
            {dados.linhas.map((l) => (
              <tr key={l.valvulaId}>
                <td data-rotulo="Situação">
                  <EtiquetaStatus status={l.status} rotulo={l.statusRotulo} />
                </td>
                <td data-rotulo="Código" className="mono">
                  <Link to={`/valvulas/${l.valvulaId}`}>{l.codigoReferencia}</Link>
                </td>
                <td data-rotulo="Condomínio">
                  <Link to={`/clientes/${l.clienteId}`}>{l.nomeCondominio}</Link>
                  {l.bairro && <span className="secundario"> · {l.bairro}</span>}
                </td>
                <td data-rotulo="Estação">
                  {l.localizacaoInstalacao}
                  <span className="secundario"> · {l.andaresAtendidos}</span>
                  {l.requerFechamentoGeral && <span className="selo-alerta">fechamento geral</span>}
                </td>
                <td data-rotulo="Última">{formatarData(l.ultimaManutencao)}</td>
                <td data-rotulo="Próxima">{formatarData(l.proximaManutencao)}</td>
                <td data-rotulo="Prazo">{textoPrazo(l.diasRestantes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
