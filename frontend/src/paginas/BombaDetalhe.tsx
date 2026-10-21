import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import type { ClienteResumo, Conjunto, Ocupacao } from "../api/tipos";
import { bombas, clientes, conjuntos } from "../api/valtis";
import { Cabecalho, Carregando, EtiquetaStatus, MensagemErro, MensagemSucesso, Modal } from "../componentes/Basicos";
import { FormBomba, FormEncerramento, FormInstalacao } from "../componentes/FormulariosBombas";
import { formatarData, textoPrazo } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

type Janela = { tipo: "editar" } | { tipo: "instalar" } | { tipo: "encerrar"; ocupacao: Ocupacao };

const ROTULO_NATUREZA = { instalacao: "Instalação", emprestimo: "Empréstimo" } as const;
const ROTULO_MOTIVO = { retirada: "retirada", devolucao: "devolução" } as const;

/** Ficha da bomba: situação, onde está, dados técnicos e o histórico de posições (RF-67). */
export function BombaDetalhe() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
  const { dados: d, carregando, erro, recarregar } = useCarga(() => bombas.detalhar(id), [id]);
  const [janela, setJanela] = useState<Janela | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useEnvio();

  function fecharERecarregar(avisoNovo: string | null = null) {
    setJanela(null);
    setAviso(avisoNovo);
    recarregar();
  }

  async function alternar() {
    if (!d) return;
    const b = d.bomba;
    if (b.ativo && !confirm("Inativar esta bomba? Ela sai das listas, mas o histórico fica guardado.")) return;
    const r = await acao.executar(() => (b.ativo ? bombas.inativar(b.id) : bombas.reativar(b.id)));
    if (r.ok) recarregar();
  }

  async function excluir() {
    if (!d) return;
    if (d.historico.length > 0) {
      alert(`${d.bomba.codigoReferencia} já tem histórico de posição e não pode ser excluída. Use Inativar.`);
      return;
    }
    if (!confirm(`Excluir ${d.bomba.codigoReferencia}? Ela some do sistema de vez.`)) return;
    const r = await acao.executar(() => bombas.excluir(d.bomba.id));
    if (r.ok) navegar(d.bomba.clienteId ? `/clientes/${d.bomba.clienteId}` : "/patrimonio", { replace: true });
  }

  if (carregando && !d) return <Carregando />;
  if (!d) return <MensagemErro erro={erro} />;

  const b = d.bomba;
  const abertas = d.historico.filter((o) => o.aberta);

  return (
    <>
      <Cabecalho titulo={b.codigoReferencia}>
        {b.ativo && b.posicoes.length === 0 && (
          <button type="button" className="botao primario" onClick={() => setJanela({ tipo: "instalar" })}>
            {b.patrimonioManutec ? "Emprestar" : "Instalar"}
          </button>
        )}
        <button type="button" className="botao" onClick={() => setJanela({ tipo: "editar" })}>
          Editar
        </button>
        <button type="button" className="botao-texto" disabled={acao.enviando} onClick={alternar}>
          {b.ativo ? "Inativar" : "Reativar"}
        </button>
        <button type="button" className="botao-texto perigo" disabled={acao.enviando} onClick={excluir}>
          Excluir
        </button>
      </Cabecalho>
      <MensagemErro erro={acao.erro} />
      <MensagemSucesso texto={aviso} />
      {!b.ativo && <div className="aviso alerta">Bomba inativa. Ela não aparece no painel.</div>}
      {b.dadosTecnicosIncompletos && (
        <div className="aviso alerta">Dados técnicos incompletos: falta fabricante, modelo, potência, tensão ou corrente.</div>
      )}

      <div className="situacao">
        <EtiquetaStatus status={b.status} rotulo={b.statusRotulo} />
        <span>
          Próxima manutenção: <strong>{formatarData(b.proximaManutencao)}</strong>
          {b.diasRestantes !== null && <span className="secundario"> · {textoPrazo(b.diasRestantes)}</span>}
        </span>
      </div>

      <dl className="ficha">
        <div>
          <dt>{b.patrimonioManutec ? "Dona" : "Condomínio"}</dt>
          <dd>
            {b.patrimonioManutec ? "Manutec (patrimônio)" : <Link to={`/clientes/${b.clienteId}`}>{b.nomeCondominio}</Link>}
          </dd>
        </div>
        <div>
          <dt>Onde está</dt>
          <dd>
            {b.posicoes.length === 0 && (
              <span className={b.disponivel ? "etiqueta em-dia" : "secundario"}>
                {b.disponivel ? "Disponível" : "Fora de posição"}
              </span>
            )}
            {b.posicoes.map((p) => (
              <div key={p.ocupacaoId}>
                <Link to={`/clientes/${p.clienteId}`}>{p.nomeCondominio}</Link> · {p.conjunto} · posição {p.numeroPosicao}
                {p.natureza === "emprestimo" && <span className="selo">emprestada</span>}
                <span className="secundario"> · desde {formatarData(p.desde)}</span>
              </div>
            ))}
          </dd>
        </div>
        <div>
          <dt>Fabricante e modelo</dt>
          <dd>{[b.fabricante, b.modelo].filter(Boolean).join(" ") || "—"}</dd>
        </div>
        <div>
          <dt>Potência · tensão · corrente</dt>
          <dd>
            {b.potenciaCv ?? "—"} CV · {b.tensaoVolts ?? "—"} V · {b.correnteAmperes ?? "—"} A
          </dd>
        </div>
        <div>
          <dt>Função</dt>
          <dd>{b.funcao ?? "—"}</dd>
        </div>
        <div>
          <dt>Instalação</dt>
          <dd>{formatarData(b.dataInstalacao)}</dd>
        </div>
        <div>
          <dt>Periodicidade</dt>
          <dd>{b.periodicidadeMeses} meses</dd>
        </div>
        <div>
          <dt>Última manutenção</dt>
          <dd>{formatarData(b.ultimaManutencao)}</dd>
        </div>
      </dl>

      <h2>Histórico de posições</h2>
      {d.historico.length === 0 && <p className="aviso">Esta bomba ainda não foi colocada em nenhuma posição.</p>}
      <ol className="linha-do-tempo">
        {d.historico.map((o) => (
          <li key={o.id}>
            <div className="linha-do-tempo-topo">
              <strong>
                {formatarData(o.inicio)} → {o.fim ? formatarData(o.fim) : "hoje"}
              </strong>
              <span className="selo">{ROTULO_NATUREZA[o.natureza]}</span>
              {o.motivoEncerramento && <span className="secundario">saiu por {ROTULO_MOTIVO[o.motivoEncerramento]}</span>}
            </div>
            <p>
              <Link to={`/clientes/${o.clienteId}`}>{o.nomeCondominio}</Link> · {o.conjunto} · posição {o.numeroPosicao}
              {o.codigoBombaSubstituida && (
                <span className="secundario">
                  {" "}
                  · no lugar de{" "}
                  <Link to={`/bombas/${o.bombaSubstituidaId}`} className="mono">
                    {o.codigoBombaSubstituida}
                  </Link>
                </span>
              )}
            </p>
            {o.observacoes && <p className="secundario">{o.observacoes}</p>}
            <p className="secundario pequeno">Registrado por {o.registradoPor}</p>
            {o.aberta && (
              <button type="button" className="botao" onClick={() => setJanela({ tipo: "encerrar", ocupacao: o })}>
                {o.natureza === "emprestimo" ? "Registrar devolução" : "Registrar retirada"}
              </button>
            )}
          </li>
        ))}
      </ol>

      <Modal titulo="Editar bomba" aberto={janela?.tipo === "editar"} aoFechar={() => setJanela(null)}>
        <FormBomba bomba={b} aoSalvar={() => fecharERecarregar()} />
      </Modal>
      <Modal
        titulo={b.patrimonioManutec ? "Emprestar a um condomínio" : "Colocar em posição"}
        aberto={janela?.tipo === "instalar"}
        aoFechar={() => setJanela(null)}
      >
        {janela?.tipo === "instalar" && <EscolhaDeConjunto bomba={b} aoSalvar={fecharERecarregar} />}
      </Modal>
      <Modal
        titulo={janela?.tipo === "encerrar" && janela.ocupacao.natureza === "emprestimo" ? "Registrar devolução" : "Registrar retirada"}
        aberto={janela?.tipo === "encerrar"}
        aoFechar={() => setJanela(null)}
      >
        {janela?.tipo === "encerrar" && <FormEncerramento ocupacao={janela.ocupacao} aoSalvar={() => fecharERecarregar()} />}
      </Modal>
      {abertas.length > 1 && (
        <div className="aviso alerta">Esta bomba consta em mais de uma posição ao mesmo tempo. Confira o histórico.</div>
      )}
    </>
  );
}

/**
 * Para a bomba do condomínio, os conjuntos são os dele. Para o patrimônio,
 * primeiro se escolhe o condomínio que vai receber o empréstimo.
 */
function EscolhaDeConjunto({ bomba, aoSalvar }: { bomba: BombaDetalheBomba; aoSalvar: (aviso: string | null) => void }) {
  const [busca, setBusca] = useState("");
  const [clienteId, setClienteId] = useState(bomba.clienteId ?? "");
  const buscaAtrasada = useAtrasado(busca);
  const candidatos = useCarga(
    () => (bomba.patrimonioManutec ? clientes.listar(buscaAtrasada, false) : Promise.resolve([] as ClienteResumo[])),
    [buscaAtrasada, bomba.patrimonioManutec],
  );
  const lista = useCarga(
    () => (clienteId ? conjuntos.doCliente(clienteId) : Promise.resolve([] as Conjunto[])),
    [clienteId],
  );

  return (
    <div>
      {bomba.patrimonioManutec && (
        <div className="formulario">
          <label className="campo">
            <span className="campo-rotulo">Condomínio que recebe *</span>
            <input
              type="search"
              placeholder="Buscar condomínio"
              value={busca}
              onChange={(e) => {
                setBusca(e.target.value);
                setClienteId("");
              }}
            />
          </label>
          <MensagemErro erro={candidatos.erro} />
          {!clienteId && candidatos.dados && candidatos.dados.length > 0 && (
            <ul className="lista-valvulas">
              {candidatos.dados.slice(0, 8).map((c) => (
                <li key={c.id}>
                  <button type="button" className="botao-texto" onClick={() => setClienteId(c.id)}>
                    {c.nomeCondominio}
                  </button>
                  <span className="secundario">{c.bairro ?? ""}</span>
                </li>
              ))}
            </ul>
          )}
          {clienteId && candidatos.dados && (
            <p>
              <strong>{candidatos.dados.find((c) => c.id === clienteId)?.nomeCondominio}</strong>{" "}
              <button type="button" className="botao-texto" onClick={() => setClienteId("")}>
                trocar
              </button>
            </p>
          )}
        </div>
      )}
      <MensagemErro erro={lista.erro} />
      {clienteId && lista.dados && lista.dados.filter((c) => c.ativo).length === 0 && (
        <p className="aviso">Este condomínio não tem conjunto de bombeamento cadastrado. Cadastre na tela do cliente.</p>
      )}
      {clienteId && lista.dados && lista.dados.filter((c) => c.ativo).length > 0 && (
        <FormInstalacao bomba={bomba} conjuntosDisponiveis={lista.dados.filter((c) => c.ativo)} aoSalvar={aoSalvar} />
      )}
    </div>
  );
}

type BombaDetalheBomba = Parameters<typeof FormInstalacao>[0]["bomba"];
