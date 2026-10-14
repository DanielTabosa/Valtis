import { agenda } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro } from "../componentes/Basicos";
import { useSessao } from "../sessao/useSessao";
import { formatarData, formatarDataHora, ROTULO_EVENTO } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/**
 * Sincronização com o Google Calendar (RF-33): o que ainda não chegou à agenda.
 * Os eventos são criados em segundo plano (Outbox, D-13); esta tela só mostra e reprocessa.
 */
export function Agenda() {
  const { ehAdmin } = useSessao();
  const { dados, carregando, erro, recarregar } = useCarga(() => agenda.situacao(), []);
  const acao = useEnvio();

  async function executarERecarregar(operacao: () => Promise<void>) {
    const r = await acao.executar(operacao);
    if (r.ok) recarregar();
  }

  if (carregando && !dados) return <Carregando />;
  if (!dados) return <MensagemErro erro={erro} />;

  return (
    <>
      <Cabecalho titulo="Agenda">
        <button type="button" className="botao" onClick={recarregar}>
          Atualizar
        </button>
        {ehAdmin && dados.sincronizacaoAtiva && (
          <button
            type="button"
            className="botao primario"
            disabled={acao.enviando}
            onClick={() => executarERecarregar(agenda.sincronizarAgora)}
          >
            Sincronizar agora
          </button>
        )}
      </Cabecalho>
      <MensagemErro erro={acao.erro} />

      {!dados.sincronizacaoAtiva && (
        <div className="aviso alerta">
          A integração com a agenda está <strong>desativada</strong> neste ambiente. Os alertas ficam guardados na fila
          e serão enviados quando ela for ligada.
        </div>
      )}
      {dados.estado.erroDeAutorizacao && (
        <div className="aviso erro" role="alert">
          <strong>A agenda recusou a credencial.</strong> {dados.estado.erroDeAutorizacao} Enquanto isso não for
          resolvido, nenhum alerta é enviado (desde {formatarDataHora(dados.estado.erroDeAutorizacaoEm)}).
        </div>
      )}

      <dl className="ficha">
        <div>
          <dt>Provedor</dt>
          <dd>{dados.provedor === "simulada" ? "Simulada (desenvolvimento)" : dados.provedor}</dd>
        </div>
        <div>
          <dt>Última rodada</dt>
          <dd>{formatarDataHora(dados.estado.ultimaExecucao)}</dd>
        </div>
        <div>
          <dt>Último envio com sucesso</dt>
          <dd>{formatarDataHora(dados.estado.ultimoSucesso)}</dd>
        </div>
      </dl>

      <section className="resumo" aria-label="Eventos por situação">
        {(["pendente", "criado", "falhou", "descartado"] as const).map((s) => (
          <div key={s} className={`cartao-resumo evento-${s}`}>
            <span className="numero">{dados.totais[s] ?? 0}</span>
            <span className="rotulo">{ROTULO_EVENTO[s]}</span>
          </div>
        ))}
      </section>

      <h2>Aguardando envio</h2>
      {dados.aguardando.length === 0 && <p className="aviso">Nada pendente: todos os alertas já estão na agenda.</p>}
      {dados.aguardando.length > 0 && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Data do alerta</th>
              <th>Tipo</th>
              <th>Condomínio</th>
              <th>Situação</th>
              <th>Último erro</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {dados.aguardando.map((e) => (
              <tr key={e.id}>
                <td data-rotulo="Data">{formatarData(e.dataEvento)}</td>
                <td data-rotulo="Tipo">{ROTULO_EVENTO[e.tipo]}</td>
                <td data-rotulo="Condomínio">
                  {e.nomeCondominio} <span className="secundario mono">{e.alvo}</span>
                </td>
                <td data-rotulo="Situação">
                  {ROTULO_EVENTO[e.status]}
                  {e.tentativas > 0 && <span className="secundario"> · {e.tentativas} tentativa(s)</span>}
                </td>
                <td data-rotulo="Erro" className="secundario">
                  {e.ultimoErro ?? "—"}
                </td>
                <td className="acoes-linha">
                  {ehAdmin && e.status === "falhou" && (
                    <button
                      type="button"
                      className="botao-texto"
                      disabled={acao.enviando}
                      onClick={() => executarERecarregar(() => agenda.reprocessar(e.id))}
                    >
                      Tentar de novo
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
