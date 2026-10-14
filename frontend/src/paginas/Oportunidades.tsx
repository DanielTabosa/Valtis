import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import type { Oportunidade, StatusOportunidade } from "../api/tipos";
import { oportunidades, type FiltroResponsavel, type Situacao } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, Modal } from "../componentes/Basicos";
import { useSessao } from "../sessao/useSessao";
import { formatarData, hojeEmRecife, ROTULO_OPORTUNIDADE, textoPrazo } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/**
 * Fila de oportunidades (RF-41 a RF-46). Sem dono fixo: qualquer um assume (D-04).
 * O destaque "sem dono" (RF-43) vem calculado do servidor.
 */
export function Oportunidades() {
  const { usuario } = useSessao();
  const [situacao, setSituacao] = useState<Situacao>("ABERTAS");
  const [responsavel, setResponsavel] = useState<FiltroResponsavel>("QUALQUER");
  const [emDesfecho, setEmDesfecho] = useState<Oportunidade | null>(null);
  const acao = useEnvio();
  const { dados, carregando, erro, recarregar } = useCarga(
    () => oportunidades.fila(situacao, responsavel),
    [situacao, responsavel],
  );

  async function assumir(o: Oportunidade) {
    const r = await acao.executar(() => oportunidades.assumir(o.id));
    if (r.ok) recarregar();
  }

  async function liberar(o: Oportunidade) {
    const r = await acao.executar(() => oportunidades.liberar(o.id));
    if (r.ok) recarregar();
  }

  const semDono = dados?.filter((o) => o.semDonoUrgente).length ?? 0;

  return (
    <>
      <Cabecalho titulo="Oportunidades" />

      <div className="filtros">
        <div className="segmentado" role="group" aria-label="Situação">
          {(["ABERTAS", "ENCERRADAS", "TODAS"] as const).map((s) => (
            <button key={s} type="button" aria-pressed={situacao === s} onClick={() => setSituacao(s)}>
              {s === "ABERTAS" ? "Abertas" : s === "ENCERRADAS" ? "Encerradas" : "Todas"}
            </button>
          ))}
        </div>
        <div className="segmentado" role="group" aria-label="Responsável">
          {(["QUALQUER", "EU", "NINGUEM"] as const).map((r) => (
            <button key={r} type="button" aria-pressed={responsavel === r} onClick={() => setResponsavel(r)}>
              {r === "QUALQUER" ? "Todos" : r === "EU" ? "Minhas" : "Sem dono"}
            </button>
          ))}
        </div>
      </div>

      {semDono > 0 && situacao !== "ENCERRADAS" && (
        <div className="aviso alerta" role="status">
          {semDono === 1
            ? "1 oportunidade já está na hora do contato e ninguém assumiu."
            : `${semDono} oportunidades já estão na hora do contato e ninguém assumiu.`}
        </div>
      )}

      <MensagemErro erro={erro ?? acao.erro} />
      {carregando && !dados && <Carregando />}
      {dados?.length === 0 && <p className="aviso">Nenhuma oportunidade com esses filtros.</p>}

      <ul className="lista-oportunidades">
        {dados?.map((o) => (
          <li key={o.id} className={`oportunidade ${o.semDonoUrgente ? "urgente" : ""}`}>
            <div className="oportunidade-topo">
              <div>
                <Link to={`/clientes/${o.clienteId}`}>
                  <strong>{o.nomeCondominio}</strong>
                </Link>
                <div className="secundario">
                  {o.codigoValvula ? (
                    <Link to={`/valvulas/${o.valvulaId}`} className="mono">
                      {o.codigoValvula}
                    </Link>
                  ) : (
                    <span>{o.codigoCliente} · cliente da base antiga</span>
                  )}
                  {o.localizacaoInstalacao && ` · ${o.localizacaoInstalacao}`}
                  {o.bairro && ` · ${o.bairro}`}
                </div>
              </div>
              <span className={`etiqueta oportunidade-${o.status}`}>{ROTULO_OPORTUNIDADE[o.status]}</span>
            </div>

            <dl className="dados">
              <div>
                <dt>Previsto</dt>
                <dd>
                  {formatarData(o.dataPrevista)}
                  {o.aberta && <span className="secundario"> · {textoPrazo(o.diasParaVencimento)}</span>}
                </dd>
              </div>
              <div>
                <dt>Responsável</dt>
                <dd>{o.responsavelNome ?? (o.semDonoUrgente ? <strong>ninguém ainda</strong> : "ninguém ainda")}</dd>
              </div>
              {o.sindicoResponsavel && (
                <div>
                  <dt>Contato</dt>
                  <dd>
                    {o.sindicoResponsavel}
                    {o.telefoneContato && (
                      <>
                        {" · "}
                        <a href={`tel:${o.telefoneContato.replace(/[^\d+]/g, "")}`}>{o.telefoneContato}</a>
                      </>
                    )}
                  </dd>
                </div>
              )}
              {o.requerFechamentoGeral && (
                <div>
                  <dt>Atenção</dt>
                  <dd>
                    <span className="selo-alerta">fechamento geral</span>
                  </dd>
                </div>
              )}
              {o.notas && (
                <div className="largo">
                  <dt>Notas</dt>
                  <dd>{o.notas}</dd>
                </div>
              )}
              {o.motivoRecusa && (
                <div className="largo">
                  <dt>Motivo da recusa</dt>
                  <dd>{o.motivoRecusa}</dd>
                </div>
              )}
            </dl>

            {o.aberta && (
              <div className="acoes">
                {!o.responsavelId && (
                  <button type="button" className="botao primario" disabled={acao.enviando} onClick={() => assumir(o)}>
                    Assumir
                  </button>
                )}
                <button type="button" className="botao" onClick={() => setEmDesfecho(o)}>
                  Registrar desfecho
                </button>
                {o.responsavelId === usuario?.id && (
                  <button type="button" className="botao-texto" disabled={acao.enviando} onClick={() => liberar(o)}>
                    Devolver à fila
                  </button>
                )}
              </div>
            )}
          </li>
        ))}
      </ul>

      <Modal titulo="Registrar desfecho" aberto={emDesfecho !== null} aoFechar={() => setEmDesfecho(null)}>
        {emDesfecho && (
          <FormDesfecho
            oportunidade={emDesfecho}
            aoSalvar={() => {
              setEmDesfecho(null);
              recarregar();
            }}
          />
        )}
      </Modal>
    </>
  );
}

const DESFECHOS: { valor: StatusOportunidade; rotulo: string; ajuda: string }[] = [
  { valor: "contatado", rotulo: "Contatado", ajuda: "Falei com o cliente; ainda sem resposta." },
  { valor: "aceito", rotulo: "Aceito", ajuda: "O cliente aprovou. Falta executar." },
  { valor: "recusado", rotulo: "Recusado", ajuda: "O cliente não quer agora." },
  { valor: "perdido", rotulo: "Perdido", ajuda: "Sem retorno, ou fechou com outra empresa." },
];

function FormDesfecho({ oportunidade, aoSalvar }: { oportunidade: Oportunidade; aoSalvar: () => void }) {
  const [status, setStatus] = useState<StatusOportunidade | "">("");
  const [notas, setNotas] = useState(oportunidade.notas ?? "");
  const [motivo, setMotivo] = useState("");
  const [lembrar, setLembrar] = useState<boolean | null>(null);
  const [lembrarEm, setLembrarEm] = useState("");
  const { enviando, erro, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (!status) return;
    const r = await executar(() =>
      oportunidades.desfecho(oportunidade.id, {
        status,
        notas,
        motivoRecusa: motivo,
        lembrarNovamente: status === "recusado" ? lembrar : null,
        lembrarEm: status === "recusado" && lembrar ? lembrarEm : null,
      }),
    );
    if (r.ok) aoSalvar();
  }

  const recusa = status === "recusado";
  const completo = status !== "" && (!recusa || lembrar === false || (lembrar === true && lembrarEm !== ""));

  return (
    <form className="formulario" onSubmit={enviar}>
      <p>
        <strong>{oportunidade.nomeCondominio}</strong>
        <span className="secundario"> · {oportunidade.codigoValvula ?? oportunidade.codigoCliente}</span>
      </p>
      <MensagemErro erro={erro} />
      <fieldset className="opcoes-radio">
        <legend>O que aconteceu?</legend>
        {DESFECHOS.map((d) => (
          <label key={d.valor} className={`opcao ${status === d.valor ? "selecionada" : ""}`}>
            <input
              type="radio"
              name="desfecho"
              value={d.valor}
              checked={status === d.valor}
              onChange={() => setStatus(d.valor)}
            />
            <strong>{d.rotulo}</strong>
            <span className="secundario">{d.ajuda}</span>
          </label>
        ))}
      </fieldset>

      {recusa && (
        <>
          <Campo rotulo="Motivo da recusa">
            <input value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Ex.: sem verba este ano" />
          </Campo>
          {/* RN-09 / D-05: o sistema pergunta, em vez de decidir sozinho. */}
          <fieldset className="opcoes-radio">
            <legend>Voltar a lembrar deste cliente?</legend>
            <label className={`opcao ${lembrar === true ? "selecionada" : ""}`}>
              <input type="radio" name="lembrar" checked={lembrar === true} onChange={() => setLembrar(true)} />
              <strong>Sim, numa data</strong>
            </label>
            <label className={`opcao ${lembrar === false ? "selecionada" : ""}`}>
              <input type="radio" name="lembrar" checked={lembrar === false} onChange={() => setLembrar(false)} />
              <strong>Não, encerrar</strong>
              <span className="secundario">Só volta se alguém reativar manualmente.</span>
            </label>
          </fieldset>
          {lembrar && (
            <Campo rotulo="Lembrar em *" dica="Os alertas de 30 e 7 dias contam a partir desta data.">
              <input
                type="date"
                required
                min={hojeEmRecife()}
                value={lembrarEm}
                onChange={(e) => setLembrarEm(e.target.value)}
              />
            </Campo>
          )}
        </>
      )}

      <Campo rotulo="Notas">
        <textarea rows={3} value={notas} onChange={(e) => setNotas(e.target.value)} />
      </Campo>

      <button type="submit" className="botao primario" disabled={!completo || enviando}>
        {enviando ? "Salvando…" : "Salvar desfecho"}
      </button>
    </form>
  );
}
