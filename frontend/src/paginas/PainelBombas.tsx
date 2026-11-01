import { useState } from "react";
import { Link, useSearchParams } from "react-router";
import type { StatusPainel } from "../api/tipos";
import { bombas } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro } from "../componentes/Basicos";
import { classeStatus } from "../util/status";
import { formatarData, textoPrazo } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { textoPosicao } from "../util/bomba";
import { useCarga } from "../util/useCarga";

/** Painel de bombas (RF-68): mesma classificação do painel de válvulas (RN-16), calculada no servidor. */
export function PainelBombas() {
  const [parametros] = useSearchParams();
  // "?busca=" permite chegar já filtrado, como a tela de pendências faz pelo código do equipamento.
  const [busca, setBusca] = useState(() => parametros.get("busca") ?? "");
  const [status, setStatus] = useState<StatusPainel | "">("");
  const buscaAtrasada = useAtrasado(busca);
  const { dados, carregando, erro } = useCarga(() => bombas.painel(status, buscaAtrasada), [status, buscaAtrasada]);
  const totalResumo = dados?.resumo.reduce((soma, r) => soma + r.total, 0) ?? 0;

  return (
    <>
      <Cabecalho titulo="Painel de bombas">
        <Link to="/patrimonio" className="botao">
          Patrimônio da Manutec
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
          placeholder="Buscar condomínio, código, fabricante ou modelo"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          aria-label="Buscar"
        />
        {status && (
          <button type="button" className="botao-texto" onClick={() => setStatus("")}>
            Limpar situação
          </button>
        )}
      </div>

      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}
      {dados && dados.linhas.length === 0 && <p className="aviso">Nenhuma bomba encontrada com esses filtros.</p>}

      {dados && dados.linhas.length > 0 && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Situação</th>
              <th>Código</th>
              <th>Bomba</th>
              <th>Onde está</th>
              <th>Última</th>
              <th>Próxima</th>
              <th>Prazo</th>
            </tr>
          </thead>
          <tbody>
            {dados.linhas.map((b) => (
              <tr key={b.id}>
                <td data-rotulo="Situação">
                  <EtiquetaStatus status={b.status} rotulo={b.statusRotulo} />
                </td>
                <td data-rotulo="Código" className="mono">
                  <Link to={`/bombas/${b.id}`}>{b.codigoReferencia}</Link>
                </td>
                <td data-rotulo="Bomba">
                  {b.descricao}
                  {b.dadosTecnicosIncompletos && <span className="selo">dados incompletos</span>}
                </td>
                <td data-rotulo="Onde está">
                  {b.posicoes.length > 0 ? (
                    <Link to={`/clientes/${b.posicoes[0].clienteId}`}>{textoPosicao(b)}</Link>
                  ) : (
                    <span className="secundario">{textoPosicao(b)}</span>
                  )}
                </td>
                <td data-rotulo="Última" className="sem-quebra">{formatarData(b.ultimaManutencaoConsiderada)}</td>
                <td data-rotulo="Próxima" className="sem-quebra">{formatarData(b.proximaManutencao)}</td>
                <td data-rotulo="Prazo" className="sem-quebra">{textoPrazo(b.diasRestantes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
