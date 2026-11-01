import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { valvulas } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro, Modal, SeloOrigem } from "../componentes/Basicos";
import { FormComplementoValvula, FormValvula } from "../componentes/Formularios";
import { HistoricoComercial, HistoricoServicos } from "../componentes/Historico";
import { formatarData, textoPrazo } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/**
 * Ficha da válvula: situação, histórico de serviços (RF-21) e histórico comercial.
 * A espelhada do Jiraya só edita os campos do Valtis e não se inativa nem exclui aqui (RN-24).
 */
export function ValvulaDetalhe() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
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

  async function excluir() {
    if (!d) return;
    const temHistorico = d.historico.length > 0 || d.oportunidades.length > 0;
    if (temHistorico) {
      alert(
        `${d.valvula.codigoReferencia} já tem serviço lançado e não pode ser excluída. ` +
          "Para tirá-la do painel sem perder o histórico, use Inativar.",
      );
      return;
    }
    if (!confirm(`Excluir ${d.valvula.codigoReferencia}? Ela some do sistema de vez.`)) return;
    const r = await acao.executar(() => valvulas.excluir(d.valvula.id));
    if (r.ok) navegar(`/clientes/${d.clienteId}`, { replace: true });
  }

  function aposSalvar() {
    setEditando(false);
    recarregar();
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
          {v.espelhada ? "Completar dados" : "Editar"}
        </button>
        {!v.espelhada && (
          <>
            <button type="button" className="botao-texto" disabled={acao.enviando} onClick={alternar}>
              {v.ativo ? "Inativar" : "Reativar"}
            </button>
            {/* RF-12a: sempre visível; com histórico, o servidor recusa e explica o porquê. */}
            <button type="button" className="botao-texto perigo" disabled={acao.enviando} onClick={excluir}>
              Excluir
            </button>
          </>
        )}
      </Cabecalho>
      {v.espelhada && (
        <p className="secundario">
          <SeloOrigem fonte="jiraya" /> Número, fabricante, modelo e situação vêm do Jiraya e só mudam lá.
        </p>
      )}
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
        {v.espelhada && (
          <div>
            <dt>Fabricante e modelo</dt>
            <dd>{[v.fabricante, v.modelo].filter(Boolean).join(" ") || "—"}</dd>
          </div>
        )}
        {v.identificacaoExterna && (
          <div>
            <dt>Identificação no Jiraya</dt>
            <dd className="mono">{v.identificacaoExterna}</dd>
          </div>
        )}
        <div>
          <dt>Especificação</dt>
          <dd>
            {v.especificacao}
            {v.especificacaoId === null && <span className="selo">a completar</span>}
          </dd>
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

      <HistoricoServicos
        servicos={d.historico}
        vazio="Nenhum serviço lançado. Situação: sem registro."
        aoMudar={recarregar}
        comPressao
      />
      <HistoricoComercial oportunidades={d.oportunidades} />

      <Modal titulo={v.espelhada ? "Completar válvula" : "Editar válvula"} aberto={editando} aoFechar={() => setEditando(false)}>
        {v.espelhada ? (
          <FormComplementoValvula valvula={v} aoSalvar={aposSalvar} />
        ) : (
          <FormValvula estacaoId={d.estacaoId} valvula={v} aoSalvar={aposSalvar} />
        )}
      </Modal>
    </>
  );
}
