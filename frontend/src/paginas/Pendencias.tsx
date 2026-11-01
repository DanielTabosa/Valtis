import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import type { Pendencia, ServicoDaPendencia } from "../api/tipos";
import { integracao } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, MensagemSucesso, Modal } from "../componentes/Basicos";
import { formatarData, formatarDataHora, ROTULO_ORIGEM_INTEGRACAO, ROTULO_TIPO_RECEBIDO } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

const SEM_MOTIVO = "Sem motivo informado";

type Grupo = { motivo: string; itens: Pendencia[] };

/** Agrupa por motivo para o admin resolver em lote o que tem a mesma causa. */
function agruparPorMotivo(lista: Pendencia[]): Grupo[] {
  const grupos = new Map<string, Pendencia[]>();
  for (const p of lista) {
    const motivo = p.motivo ?? SEM_MOTIVO;
    grupos.set(motivo, [...(grupos.get(motivo) ?? []), p]);
  }
  return [...grupos.entries()]
    .map(([motivo, itens]) => ({ motivo, itens }))
    .sort((a, b) => b.itens.length - a.itens.length);
}

/** A ficha do equipamento do serviço: é nela que o admin inativa o serviço (RN-26). */
function linkDoEquipamento(s: ServicoDaPendencia): string {
  return s.bombaId ? `/bombas/${s.bombaId}` : `/valvulas/${s.valvulaId}`;
}

/**
 * Pendências da integração (RF-75): itens recebidos do Manutec Clientes ou do Jiraya
 * que não puderam ser aplicados. O admin corrige a causa na fonte e reprocessa,
 * ou descarta com justificativa.
 */
export function Pendencias() {
  const { dados, carregando, erro, recarregar } = useCarga(() => integracao.pendencias(), []);
  const [descartando, setDescartando] = useState<Pendencia | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useEnvio();

  async function reprocessar(p: Pendencia) {
    setAviso(null);
    const r = await acao.executar(() => integracao.reprocessar(p.id));
    if (r.ok) {
      setAviso("Item enviado para reprocessar. Ele sai desta lista se for aplicado na próxima rodada.");
      recarregar();
    }
  }

  const grupos = agruparPorMotivo(dados ?? []);

  return (
    <>
      <Cabecalho titulo="Pendências da integração">
        <button type="button" className="botao" onClick={recarregar}>
          Atualizar
        </button>
        <Link to="/integracao" className="botao">
          Estado da integração
        </Link>
      </Cabecalho>
      <p className="secundario">
        Itens que chegaram das fontes e não puderam ser aplicados. Corrija a causa na fonte e reprocesse; se o item não
        deve entrar, descarte com uma justificativa.
      </p>
      <MensagemSucesso texto={aviso} />
      <MensagemErro erro={erro ?? acao.erro} />
      {carregando && !dados && <Carregando />}
      {dados?.length === 0 && <p className="aviso sucesso">Nenhuma pendência. Tudo o que chegou foi aplicado.</p>}

      {grupos.map((g) => (
        <section key={g.motivo} className="bloco">
          <div className="bloco-topo">
            <div>
              <h3>{g.motivo}</h3>
              <p className="secundario">{g.itens.length === 1 ? "1 item" : `${g.itens.length} itens`}</p>
            </div>
          </div>
          <table className="tabela tabela-cartoes">
            <thead>
              <tr>
                <th>Item</th>
                <th>Origem</th>
                <th>Recebido</th>
                <th>Serviços ligados</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {g.itens.map((p) => (
                <tr key={p.id} className={p.estado === "falhou" ? "linha-erro" : ""}>
                  <td data-rotulo="Item">
                    {p.resumo}
                    {p.estado === "falhou" && <span className="selo-alerta">falhou</span>}
                    {p.ultimoErro && <div className="secundario pequeno">{p.ultimoErro}</div>}
                  </td>
                  <td data-rotulo="Origem">
                    {ROTULO_ORIGEM_INTEGRACAO[p.origem]} · {ROTULO_TIPO_RECEBIDO[p.tipo]}
                    <div className="secundario mono pequeno">
                      id {p.idExterno} · versão {p.versao}
                    </div>
                  </td>
                  <td data-rotulo="Recebido" className="sem-quebra">
                    {formatarDataHora(p.recebidoEm)}
                    {p.tentativas > 0 && <div className="secundario pequeno">{p.tentativas} tentativa(s)</div>}
                  </td>
                  <td data-rotulo="Serviços">
                    {p.servicos.length === 0 && <span className="secundario">—</span>}
                    {p.servicos.map((s) => (
                      <div key={s.id} className={s.ativo ? "" : "secundario"}>
                        <Link to={linkDoEquipamento(s)} className="mono">
                          {s.codigoEquipamento}
                        </Link>{" "}
                        <span className="secundario">· {formatarData(s.dataRealizada)}</span>
                        {!s.ativo && <span className="selo">inativo</span>}
                      </div>
                    ))}
                  </td>
                  <td className="acoes-linha">
                    <button
                      type="button"
                      className="botao-texto"
                      disabled={acao.enviando}
                      onClick={() => reprocessar(p)}
                    >
                      Reprocessar
                    </button>
                    <button
                      type="button"
                      className="botao-texto perigo"
                      disabled={acao.enviando}
                      onClick={() => setDescartando(p)}
                    >
                      Descartar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}

      <Modal titulo="Descartar item" aberto={descartando !== null} aoFechar={() => setDescartando(null)}>
        {descartando && (
          <FormDescarte
            pendencia={descartando}
            aoSalvar={() => {
              setDescartando(null);
              setAviso("Item descartado. Ele não será mais aplicado.");
              recarregar();
            }}
          />
        )}
      </Modal>
    </>
  );
}

function FormDescarte({ pendencia, aoSalvar }: { pendencia: Pendencia; aoSalvar: () => void }) {
  const [justificativa, setJustificativa] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const r = await executar(() => integracao.descartar(pendencia.id, justificativa.trim()));
    if (r.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <p>
        <strong>{pendencia.resumo}</strong>
        <span className="secundario">
          {" "}
          · {ROTULO_ORIGEM_INTEGRACAO[pendencia.origem]}, id {pendencia.idExterno}
        </span>
      </p>
      <p className="secundario">
        O item fica registrado como descartado, com a sua justificativa, e não é aplicado.
      </p>
      <MensagemErro erro={erro} />
      <Campo rotulo="Justificativa *" erro={campos.justificativa}>
        <textarea
          rows={3}
          required
          autoFocus
          value={justificativa}
          onChange={(e) => setJustificativa(e.target.value)}
        />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando || justificativa.trim() === ""}>
        {enviando ? "Descartando…" : "Descartar item"}
      </button>
    </form>
  );
}
