import { useState } from "react";
import { Link, useParams } from "react-router";
import { valvulas } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro, Modal } from "../componentes/Basicos";
import { FormValvula } from "../componentes/Formularios";
import { useSessao } from "../sessao/useSessao";
import { formatarData, formatarDataHora, ROTULO_OPORTUNIDADE, textoPrazo } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/** Ficha da válvula: situação, histórico de serviços (RF-21) e histórico comercial. */
export function ValvulaDetalhe() {
  const { id = "" } = useParams();
  const { ehAdmin } = useSessao();
  const { dados: d, carregando, erro, recarregar } = useCarga(() => valvulas.detalhar(id), [id]);
  const [editando, setEditando] = useState(false);
  const acao = useEnvio();

  async function alternar() {
    if (!d) return;
    const v = d.valvula;
    if (v.ativo && !confirm("Inativar esta válvula? Ela sai do painel, mas o histórico fica guardado.")) return;
    const r = await acao.executar(() => (v.ativo ? valvulas.inativar(v.id) : valvulas.reativar(v.id)));
    if (r.ok) recarregar();
  }

  if (carregando && !d) return <Carregando />;
  if (!d) return <MensagemErro erro={erro} />;

  const v = d.valvula;
  const operante = v.ativo && d.estacaoAtiva && d.clienteAtivo;

  return (
    <>
      <Cabecalho titulo={v.codigoReferencia}>
        {operante && (
          <Link to={`/lancar?cliente=${d.clienteId}&valvula=${v.id}`} className="botao primario">
            Lançar serviço
          </Link>
        )}
        <button type="button" className="botao" onClick={() => setEditando(true)}>
          Editar
        </button>
        {ehAdmin && (
          <button type="button" className="botao-texto" disabled={acao.enviando} onClick={alternar}>
            {v.ativo ? "Inativar" : "Reativar"}
          </button>
        )}
      </Cabecalho>
      <MensagemErro erro={acao.erro} />
      {!operante && (
        <div className="aviso alerta">
          {v.ativo ? "A estação ou o cliente desta válvula está inativo." : "Válvula inativa."} Ela não aparece no
          painel.
        </div>
      )}

      <div className="situacao">
        <EtiquetaStatus status={d.status} rotulo={d.statusRotulo} />
        <span>
          Próxima manutenção: <strong>{formatarData(d.proximaManutencao)}</strong>
          {d.diasRestantes !== null && <span className="secundario"> · {textoPrazo(d.diasRestantes)}</span>}
        </span>
      </div>

      <dl className="ficha">
        <div>
          <dt>Condomínio</dt>
          <dd>
            <Link to={`/clientes/${d.clienteId}`}>{d.nomeCondominio}</Link>
            <span className="secundario"> · {d.codigoCliente}</span>
          </dd>
        </div>
        <div>
          <dt>Estação</dt>
          <dd>
            {d.localizacaoInstalacao} · {d.andares}
            {d.requerFechamentoGeral && <span className="selo-alerta">fechamento geral</span>}
          </dd>
        </div>
        <div>
          <dt>Número na estação</dt>
          <dd>{v.numeroValvula}</dd>
        </div>
        <div>
          <dt>Especificação</dt>
          <dd>{v.especificacao}</dd>
        </div>
        <div>
          <dt>Número de série</dt>
          <dd>{v.numeroSerie ?? "—"}</dd>
        </div>
        <div>
          <dt>Instalação</dt>
          <dd>{formatarData(v.dataInstalacao)}</dd>
        </div>
        <div>
          <dt>Periodicidade</dt>
          <dd>{v.periodicidadeMeses} meses</dd>
        </div>
      </dl>

      <h2>Histórico de serviços</h2>
      {d.historico.length === 0 && <p className="aviso">Nenhum serviço lançado. Situação: sem registro.</p>}
      <ol className="linha-do-tempo">
        {d.historico.map((s) => (
          <li key={s.id}>
            <div className="linha-do-tempo-topo">
              <strong>{formatarData(s.dataRealizada)}</strong>
              <span className="selo">{s.tipoManutencao}</span>
              <span className="secundario">por {s.tecnicoResponsavel}</span>
            </div>
            {s.pecasSubstituidas && <p>Peças: {s.pecasSubstituidas}</p>}
            {(s.pressaoEntrada !== null || s.pressaoSaida !== null) && (
              <p>
                Pressão: entrada {s.pressaoEntrada ?? "—"} · saída {s.pressaoSaida ?? "—"}
              </p>
            )}
            {s.observacoes && <p>{s.observacoes}</p>}
            <p className="secundario pequeno">
              Lançado por {s.lancadoPor} em {formatarDataHora(s.lancadoEm)}
            </p>
          </li>
        ))}
      </ol>

      {d.oportunidades.length > 0 && (
        <>
          <h2>Histórico comercial</h2>
          <table className="tabela tabela-cartoes">
            <thead>
              <tr>
                <th>Previsto</th>
                <th>Desfecho</th>
                <th>Responsável</th>
                <th>Encerrada</th>
              </tr>
            </thead>
            <tbody>
              {d.oportunidades.map((o) => (
                <tr key={o.id}>
                  <td data-rotulo="Previsto">{formatarData(o.dataPrevista)}</td>
                  <td data-rotulo="Desfecho">
                    <span className={`etiqueta oportunidade-${o.status}`}>{ROTULO_OPORTUNIDADE[o.status]}</span>
                    {o.motivoRecusa && <span className="secundario"> · {o.motivoRecusa}</span>}
                  </td>
                  <td data-rotulo="Responsável">{o.responsavel ?? "—"}</td>
                  <td data-rotulo="Encerrada">{formatarDataHora(o.encerradaEm)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      <Modal titulo="Editar válvula" aberto={editando} aoFechar={() => setEditando(false)}>
        <FormValvula
          estacaoId={d.estacaoId}
          valvula={v}
          aoSalvar={() => {
            setEditando(false);
            recarregar();
          }}
        />
      </Modal>
    </>
  );
}
