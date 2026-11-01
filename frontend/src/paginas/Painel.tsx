import { useState } from "react";
import { Link, useSearchParams } from "react-router";
import type { StatusPainel } from "../api/tipos";
import { painel } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro } from "../componentes/Basicos";
import { classeStatus } from "../util/status";
import { formatarData, textoPrazo } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";

/** Painel de status (RF-34 a RF-39): uma linha por válvula ativa, calculado no servidor. */
export function Painel() {
  const [parametros] = useSearchParams();
  // "?busca=" permite chegar já filtrado, como a tela de pendências faz pelo código do equipamento.
  const [busca, setBusca] = useState(() => parametros.get("busca") ?? "");
  const [status, setStatus] = useState<StatusPainel | "">("");
  const [tipoAtendimento, setTipoAtendimento] = useState("");
  const [bairro, setBairro] = useState("");
  const [tecnico, setTecnico] = useState("");
  const [vencimentoDe, setVencimentoDe] = useState("");
  const [vencimentoAte, setVencimentoAte] = useState("");
  const buscaAtrasada = useAtrasado(busca);

  const { dados, carregando, erro } = useCarga(
    () =>
      painel.montar({
        busca: buscaAtrasada,
        status: status || undefined,
        tipoAtendimento,
        bairro,
        tecnico,
        vencimentoDe: vencimentoDe || undefined,
        vencimentoAte: vencimentoAte || undefined,
      }),
    [buscaAtrasada, status, tipoAtendimento, bairro, tecnico, vencimentoDe, vencimentoAte],
  );

  const temPeriodo = vencimentoDe !== "" || vencimentoAte !== "";

  const totalResumo = dados?.resumo.reduce((soma, r) => soma + r.total, 0) ?? 0;

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
              <span className="rotulo">{r.rotulo}</span>
              <span className="numero">{r.total}</span>
              <span className="proporcao" aria-hidden="true">
                <span style={{ width: `${totalResumo ? (r.total / totalResumo) * 100 : 0}%` }} />
              </span>
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
        <select value={bairro} onChange={(e) => setBairro(e.target.value)} aria-label="Bairro">
          <option value="">Todos os bairros</option>
          {dados?.opcoes.bairros.map((b) => (
            <option key={b} value={b}>
              {b}
            </option>
          ))}
        </select>
        <select value={tecnico} onChange={(e) => setTecnico(e.target.value)} aria-label="Técnico do último serviço">
          <option value="">Todos os técnicos</option>
          {dados?.opcoes.tecnicos.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <label className="filtro-periodo">
          Vence de
          <input
            type="date"
            value={vencimentoDe}
            max={vencimentoAte || undefined}
            onChange={(e) => setVencimentoDe(e.target.value)}
          />
        </label>
        <label className="filtro-periodo">
          até
          <input
            type="date"
            value={vencimentoAte}
            min={vencimentoDe || undefined}
            onChange={(e) => setVencimentoAte(e.target.value)}
          />
        </label>
        {temPeriodo && (
          <button
            type="button"
            className="botao-texto"
            onClick={() => {
              setVencimentoDe("");
              setVencimentoAte("");
            }}
          >
            Limpar período
          </button>
        )}
        {status && (
          <button type="button" className="botao-texto" onClick={() => setStatus("")}>
            Limpar situação
          </button>
        )}
      </div>

      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}

      {dados && dados.linhas.length === 0 && (
        <p className="aviso">
          Nenhuma válvula encontrada com esses filtros.
          {temPeriodo && " Com período, válvulas sem registro ficam de fora: não têm vencimento."}
        </p>
      )}

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
                <td data-rotulo="Última" className="sem-quebra">
                  {formatarData(l.ultimaManutencao)}
                  {l.ultimoTecnico && <span className="secundario"> · {l.ultimoTecnico}</span>}
                </td>
                <td data-rotulo="Próxima" className="sem-quebra">{formatarData(l.proximaManutencao)}</td>
                <td data-rotulo="Prazo" className="sem-quebra">{textoPrazo(l.diasRestantes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
