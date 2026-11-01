import { useState } from "react";
import { Link } from "react-router";
import type { EstadoEventoRecebido, OrigemIntegracao, ResumoDaLeitura } from "../api/tipos";
import { integracao } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro, MensagemSucesso } from "../componentes/Basicos";
import { formatarDataHora, ROTULO_ESTADO_RECEBIDO, ROTULO_ORIGEM_INTEGRACAO } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

const ESTADOS: EstadoEventoRecebido[] = ["pendente", "aplicado", "pendencia", "falhou", "descartado", "superado"];

function nomeDaOrigem(origem: string): string {
  return origem in ROTULO_ORIGEM_INTEGRACAO ? ROTULO_ORIGEM_INTEGRACAO[origem as OrigemIntegracao] : origem;
}

function textoDaLeitura(r: ResumoDaLeitura): string {
  if (r.recebidos === 0) return "Leitura feita: nada novo nas fontes.";
  return `Leitura feita: ${r.recebidos} ${r.recebidos === 1 ? "item recebido" : "itens recebidos"}, ${r.aplicados} ${
    r.aplicados === 1 ? "aplicado" : "aplicados"
  }.`;
}

/**
 * Estado do modo integrado (RF-78): de onde vêm os cadastros, até onde cada fonte foi
 * lida e quantos itens recebidos estão em cada estado. A leitura roda sozinha em
 * segundo plano; "Ler agora" só antecipa a próxima rodada.
 */
export function Integracao() {
  const { dados, carregando, erro, recarregar } = useCarga(() => integracao.estado(), []);
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useEnvio();

  async function lerAgora() {
    setAviso(null);
    const r = await acao.executar(() => integracao.lerAgora());
    if (r.ok) {
      setAviso(textoDaLeitura(r.valor));
      recarregar();
    }
  }

  if (carregando && !dados) return <Carregando />;
  if (!dados) return <MensagemErro erro={erro} />;

  const pendencias = (dados.eventos.pendencia ?? 0) + (dados.eventos.falhou ?? 0);

  return (
    <>
      <Cabecalho titulo="Integração">
        <button type="button" className="botao" onClick={recarregar}>
          Atualizar
        </button>
        {dados.leituraAtiva && (
          <button type="button" className="botao primario" disabled={acao.enviando} onClick={lerAgora}>
            {acao.enviando ? "Lendo…" : "Ler agora"}
          </button>
        )}
      </Cabecalho>
      <MensagemErro erro={acao.erro ?? erro} />
      <MensagemSucesso texto={aviso} />

      {!dados.leituraAtiva && (
        <div className="aviso alerta">
          A leitura das fontes está <strong>desligada</strong> neste ambiente. Nada novo chega do Manutec Clientes nem
          do Jiraya até ela ser ligada.
        </div>
      )}
      {pendencias > 0 && (
        <div className="aviso alerta" role="status">
          {pendencias === 1 ? "1 item recebido precisa de atenção." : `${pendencias} itens recebidos precisam de atenção.`}{" "}
          <Link to="/integracao/pendencias">Ver pendências</Link>
        </div>
      )}

      <dl className="ficha">
        <div>
          <dt>Cadastros</dt>
          <dd>
            {dados.modoCadastros === "externa"
              ? "Clientes no Manutec Clientes; equipamentos e OS no Jiraya"
              : "Próprios do Valtis"}
          </dd>
        </div>
        <div>
          <dt>Leitura automática</dt>
          <dd>{dados.leituraAtiva ? "Ligada" : "Desligada"}</dd>
        </div>
      </dl>

      <section className="resumo seis" aria-label="Itens recebidos por estado">
        {ESTADOS.map((e) => (
          <div key={e} className={`cartao-resumo integracao-${e}`}>
            <span className="numero">{dados.eventos[e] ?? 0}</span>
            <span className="rotulo">{ROTULO_ESTADO_RECEBIDO[e]}</span>
          </div>
        ))}
      </section>

      <h2>Leitura por fonte</h2>
      {dados.cursores.length === 0 && <p className="aviso">Nenhuma fonte configurada.</p>}
      {dados.cursores.length > 0 && (
        <table className="tabela tabela-cartoes">
          <thead>
            <tr>
              <th>Fonte</th>
              <th>Recurso</th>
              <th>Última leitura</th>
              <th>Última leitura sem erro</th>
              <th>Lido até</th>
              <th>Erro</th>
            </tr>
          </thead>
          <tbody>
            {dados.cursores.map((c) => (
              <tr key={`${c.origem}-${c.recurso}`} className={c.ultimoErro ? "linha-erro" : ""}>
                <td data-rotulo="Fonte">{nomeDaOrigem(c.origem)}</td>
                <td data-rotulo="Recurso" className="mono">
                  {c.recurso}
                </td>
                <td data-rotulo="Última leitura" className="sem-quebra">
                  {c.ultimaLeituraEm ? formatarDataHora(c.ultimaLeituraEm) : "nunca"}
                </td>
                <td data-rotulo="Sem erro" className="sem-quebra">
                  {formatarDataHora(c.ultimaLeituraOkEm)}
                </td>
                {/* O cursor é opaco (marca da fonte): mostrado como veio, só para conferência. */}
                <td data-rotulo="Lido até" className="mono secundario">
                  {c.desde ?? "—"}
                </td>
                <td data-rotulo="Erro" className="secundario">
                  {c.ultimoErro ?? "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
