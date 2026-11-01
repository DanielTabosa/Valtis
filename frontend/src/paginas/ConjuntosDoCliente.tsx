import { useState } from "react";
import { Link } from "react-router";
import type { Bomba, Conjunto } from "../api/tipos";
import { bombas, conjuntos } from "../api/valtis";
import { Carregando, MensagemErro, MensagemSucesso, Modal, NotaGerido, SeloOrigem } from "../componentes/Basicos";
import { FormBomba, FormConjunto, FormInstalacao } from "../componentes/FormulariosBombas";
import { useSessao } from "../sessao/useSessao";
import { useSistema } from "../sessao/useSistema";
import { formatarData } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

type Janela =
  | { tipo: "novo-conjunto" }
  | { tipo: "conjunto"; conjunto: Conjunto }
  | { tipo: "nova-bomba" }
  | { tipo: "bomba"; bomba: Bomba }
  | { tipo: "instalar"; bomba: Bomba };

/**
 * Seção "Bombas" da tela do cliente: conjuntos com as posições e as bombas dele (RF-61, RF-62, RF-68).
 * No modo integrado, conjuntos, bombas e posições vêm do Jiraya (RF-73, RF-77): aqui só se lê
 * e se completa a bomba.
 */
export function ConjuntosDoCliente({ clienteId, clienteAtivo }: { clienteId: string; clienteAtivo: boolean }) {
  const { ehAdmin } = useSessao();
  const { externo } = useSistema();
  const lista = useCarga(() => conjuntos.doCliente(clienteId), [clienteId]);
  const doCliente = useCarga(() => bombas.doCliente(clienteId), [clienteId]);
  const [janela, setJanela] = useState<Janela | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useEnvio();

  function fecharERecarregar(avisoNovo: string | null = null) {
    setJanela(null);
    setAviso(avisoNovo);
    lista.recarregar();
    doCliente.recarregar();
  }

  async function alternarConjunto(c: Conjunto) {
    if (c.ativo && !confirm("Inativar este conjunto? As bombas continuam cadastradas.")) return;
    const r = await acao.executar(() => (c.ativo ? conjuntos.inativar(c.id) : conjuntos.reativar(c.id)));
    if (r.ok) fecharERecarregar();
  }

  const todas = doCliente.dados ?? [];
  const foraDePosicao = todas.filter((b) => b.ativo && b.posicoes.length === 0);

  return (
    <>
      <div className="cabecalho-secao">
        <h2>Bombas</h2>
        {externo && <NotaGerido oQue="Conjuntos e bombas" fonte="jiraya" />}
        {clienteAtivo && !externo && (
          <div className="acoes">
            <button type="button" className="botao" onClick={() => setJanela({ tipo: "novo-conjunto" })}>
              + Novo conjunto
            </button>
            <button type="button" className="botao" onClick={() => setJanela({ tipo: "nova-bomba" })}>
              + Bomba
            </button>
          </div>
        )}
      </div>
      <MensagemErro erro={lista.erro ?? doCliente.erro ?? acao.erro} />
      <MensagemSucesso texto={aviso} />
      {lista.carregando && !lista.dados && <Carregando />}
      {lista.dados && lista.dados.length === 0 && <p className="aviso">Nenhum conjunto de bombeamento cadastrado.</p>}

      {lista.dados?.map((c) => (
        <section key={c.id} className={`bloco ${c.ativo ? "" : "inativo"}`}>
          <div className="bloco-topo">
            <div>
              <h3>
                {c.tipo} · {c.localizacao}
                {c.derivado && <SeloOrigem fonte="jiraya" />}
              </h3>
              <p className="secundario">
                {c.observacoes}
                {!c.ativo && <span className="selo">inativo</span>}
              </p>
            </div>
            {/* O derivado acompanha o Jiraya: não se edita nem se inativa aqui. */}
            {!c.derivado && (
              <div className="acoes">
                <button
                  type="button"
                  className="botao-texto"
                  onClick={() => setJanela({ tipo: "conjunto", conjunto: c })}
                >
                  Editar
                </button>
                {ehAdmin && (
                  <button type="button" className="botao-texto" onClick={() => alternarConjunto(c)}>
                    {c.ativo ? "Inativar" : "Reativar"}
                  </button>
                )}
              </div>
            )}
          </div>
          {c.posicoes.length === 0 ? (
            <p className="secundario">Nenhuma bomba em posição neste conjunto.</p>
          ) : (
            <ul className="lista-valvulas">
              {c.posicoes.map((p) => (
                <li key={p.ocupacaoId}>
                  <span>Pos. {p.numeroPosicao}</span>
                  <Link to={`/bombas/${p.bombaId}`} className="mono">
                    {p.codigoBomba}
                  </Link>
                  <span className="secundario">{p.descricaoBomba}</span>
                  {p.natureza === "emprestimo" && <span className="selo-alerta">emprestada pela Manutec</span>}
                  <span className="secundario">desde {formatarData(p.desde)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      ))}

      {foraDePosicao.length > 0 && (
        <section className="bloco">
          <div className="bloco-topo">
            <div>
              <h3>Bombas fora de posição</h3>
              <p className="secundario">Do condomínio, mas sem posição registrada: em conserto, guardadas ou recém-cadastradas.</p>
            </div>
          </div>
          <ul className="lista-valvulas">
            {foraDePosicao.map((b) => (
              <li key={b.id}>
                <Link to={`/bombas/${b.id}`} className="mono">
                  {b.codigoReferencia}
                </Link>
                <span className="secundario">{b.descricao}</span>
                {b.dadosTecnicosIncompletos && <span className="selo">dados incompletos</span>}
                {b.espelhada && <SeloOrigem fonte="jiraya" />}
                {clienteAtivo && !externo && lista.dados && lista.dados.some((c) => c.ativo) && (
                  <button type="button" className="botao-texto" onClick={() => setJanela({ tipo: "instalar", bomba: b })}>
                    Instalar
                  </button>
                )}
                <button type="button" className="botao-texto" onClick={() => setJanela({ tipo: "bomba", bomba: b })}>
                  {b.espelhada ? "Completar" : "Editar"}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      <Modal titulo="Novo conjunto de bombeamento" aberto={janela?.tipo === "novo-conjunto"} aoFechar={() => setJanela(null)}>
        <FormConjunto clienteId={clienteId} aoSalvar={() => fecharERecarregar()} />
      </Modal>
      <Modal titulo="Editar conjunto" aberto={janela?.tipo === "conjunto"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "conjunto" && (
          <FormConjunto clienteId={clienteId} conjunto={janela.conjunto} aoSalvar={() => fecharERecarregar()} />
        )}
      </Modal>
      <Modal titulo="Nova bomba" aberto={janela?.tipo === "nova-bomba"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "nova-bomba" && (
          <FormBomba clienteId={clienteId} conjuntosDoCliente={lista.dados ?? []} aoSalvar={() => fecharERecarregar()} />
        )}
      </Modal>
      <Modal titulo="Editar bomba" aberto={janela?.tipo === "bomba"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "bomba" && <FormBomba bomba={janela.bomba} aoSalvar={() => fecharERecarregar()} />}
      </Modal>
      <Modal titulo="Colocar em posição" aberto={janela?.tipo === "instalar"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "instalar" && (
          <FormInstalacao
            bomba={janela.bomba}
            conjuntosDisponiveis={(lista.dados ?? []).filter((c) => c.ativo)}
            aoSalvar={fecharERecarregar}
          />
        )}
      </Modal>
    </>
  );
}
