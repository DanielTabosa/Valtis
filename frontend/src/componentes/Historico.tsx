// Histórico de manutenção e comercial de um equipamento. Serve a ficha da válvula e
// a da bomba, que mostram exatamente o mesmo formato (RF-21, RF-80).
import { useState, type FormEvent } from "react";
import type { OportunidadeDoHistorico, ServicoDoHistorico } from "../api/tipos";
import { servicos as apiServicos } from "../api/valtis";
import { useSessao } from "../sessao/useSessao";
import { formatarData, formatarDataHora, ROTULO_MANUTENCAO, ROTULO_OPORTUNIDADE } from "../util/formato";
import { useEnvio } from "../util/useEnvio";
import { Campo, MensagemErro, Modal } from "./Basicos";

type PropsServicos = {
  servicos: ServicoDoHistorico[];
  /** Mostrado quando a lista está vazia. */
  vazio: string;
  /** Chamado depois de inativar um serviço, para a ficha recarregar a situação. */
  aoMudar: () => void;
  /** Mostra as pressões (só válvula tem). */
  comPressao: boolean;
};

export function HistoricoServicos({ servicos, vazio, aoMudar, comPressao }: PropsServicos) {
  const { ehAdmin } = useSessao();
  const [inativando, setInativando] = useState<ServicoDoHistorico | null>(null);

  return (
    <>
      <h2>Histórico de serviços</h2>
      {servicos.length === 0 && <p className="aviso">{vazio}</p>}
      <ol className="linha-do-tempo">
        {servicos.map((s) => (
          <li key={s.id} className={s.ativo ? "" : "servico-inativo"}>
            <div className="linha-do-tempo-topo">
              <strong>{formatarData(s.dataRealizada)}</strong>
              <span className="selo">{ROTULO_MANUTENCAO[s.tipoManutencao]}</span>
              {s.origem === "jiraya" && (
                <span className="selo selo-origem">
                  {s.jirayaOsId !== null ? `OS ${s.jirayaOsId} do Jiraya` : "do Jiraya"}
                </span>
              )}
              {!s.ativo && <span className="selo">inativo</span>}
              <span className="secundario">por {s.tecnicoResponsavel}</span>
            </div>
            {!s.ativo && (
              <p>
                <strong>Inativado:</strong> {s.motivoInativacao ?? "sem motivo registrado"}. Não conta para o
                vencimento.
              </p>
            )}
            {s.pecasSubstituidas && <p>Peças: {s.pecasSubstituidas}</p>}
            {comPressao && (s.pressaoEntrada !== null || s.pressaoSaida !== null) && (
              <p>
                Pressão: entrada {s.pressaoEntrada ?? "—"} · saída {s.pressaoSaida ?? "—"}
              </p>
            )}
            {s.observacoes && <p>{s.observacoes}</p>}
            <p className="secundario pequeno">
              Lançado por {s.lancadoPor} em {formatarDataHora(s.lancadoEm)}
            </p>
            {ehAdmin && s.ativo && (
              <button type="button" className="botao-texto perigo" onClick={() => setInativando(s)}>
                Inativar serviço
              </button>
            )}
          </li>
        ))}
      </ol>

      <Modal titulo="Inativar serviço" aberto={inativando !== null} aoFechar={() => setInativando(null)}>
        {inativando && (
          <FormInativacao
            servico={inativando}
            aoSalvar={() => {
              setInativando(null);
              aoMudar();
            }}
          />
        )}
      </Modal>
    </>
  );
}

/**
 * Inativar não apaga (RN-26): o serviço fica no histórico, riscado, e deixa de contar
 * para o vencimento. O servidor recusa (422) quando é o único do equipamento.
 */
function FormInativacao({ servico, aoSalvar }: { servico: ServicoDoHistorico; aoSalvar: () => void }) {
  const [motivo, setMotivo] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const r = await executar(() => apiServicos.inativar(servico.id, motivo.trim()));
    if (r.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <p className="secundario">
        Serviço de {formatarData(servico.dataRealizada)} ({ROTULO_MANUTENCAO[servico.tipoManutencao]}). Ele continua
        no histórico, riscado, e o vencimento passa a contar pelo serviço anterior.
      </p>
      <MensagemErro erro={erro} />
      <Campo rotulo="Motivo *" erro={campos.motivo}>
        <textarea
          rows={3}
          required
          autoFocus
          value={motivo}
          placeholder="Ex.: lançado no equipamento errado"
          onChange={(e) => setMotivo(e.target.value)}
        />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando || motivo.trim() === ""}>
        {enviando ? "Inativando…" : "Inativar serviço"}
      </button>
    </form>
  );
}

export function HistoricoComercial({ oportunidades }: { oportunidades: OportunidadeDoHistorico[] }) {
  if (oportunidades.length === 0) return null;
  return (
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
          {oportunidades.map((o) => (
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
  );
}
