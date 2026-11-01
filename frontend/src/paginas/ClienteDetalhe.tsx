import { useState } from "react";
import { Link, useParams } from "react-router";
import type { Estacao, Valvula } from "../api/tipos";
import { clientes, estacoes } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro, Modal, NotaGerido, SeloOrigem } from "../componentes/Basicos";
import {
  FormCliente,
  FormComplementoCliente,
  FormComplementoEstacao,
  FormComplementoValvula,
  FormEstacao,
  FormValvula,
} from "../componentes/Formularios";
import { useSessao } from "../sessao/useSessao";
import { useSistema } from "../sessao/useSistema";
import { formatarData } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";
import { ConjuntosDoCliente } from "./ConjuntosDoCliente";

type Janela =
  | { tipo: "cliente" }
  | { tipo: "nova-estacao" }
  | { tipo: "estacao"; estacao: Estacao }
  | { tipo: "nova-valvula"; estacao: Estacao }
  | { tipo: "valvula"; valvula: Valvula };

/**
 * Ficha do cliente. Registro espelhado (do Manutec Clientes ou do Jiraya) edita só os
 * campos do Valtis, pela rota de complemento, e não se inativa por aqui (RN-24, D-32).
 */
export function ClienteDetalhe() {
  const { id = "" } = useParams();
  const { ehAdmin } = useSessao();
  const { externo } = useSistema();
  const { dados: c, carregando, erro, recarregar } = useCarga(() => clientes.detalhar(id), [id]);
  const [janela, setJanela] = useState<Janela | null>(null);
  const acao = useEnvio();

  function fecharERecarregar() {
    setJanela(null);
    recarregar();
  }

  async function alternarCliente() {
    if (!c) return;
    if (c.ativo && !confirm("Inativar este cliente? Ele sai do painel e da fila, mas o histórico fica guardado.")) {
      return;
    }
    const r = await acao.executar(() => (c.ativo ? clientes.inativar(c.id) : clientes.reativar(c.id)));
    if (r.ok) recarregar();
  }

  async function alternarEstacao(e: Estacao) {
    if (e.ativo && !confirm("Inativar esta estação? As válvulas dela saem do painel.")) return;
    const r = await acao.executar(() => (e.ativo ? estacoes.inativar(e.id) : estacoes.reativar(e.id)));
    if (r.ok) recarregar();
  }

  if (carregando && !c) return <Carregando />;
  if (!c) return <MensagemErro erro={erro} />;

  return (
    <>
      <Cabecalho titulo={c.nomeCondominio}>
        {c.ativo && (
          <Link to={`/lancar?cliente=${c.id}`} className="botao primario">
            Lançar serviço
          </Link>
        )}
        <button type="button" className="botao" onClick={() => setJanela({ tipo: "cliente" })}>
          {c.espelhado ? "Completar dados" : "Editar"}
        </button>
        {ehAdmin && !c.espelhado && (
          <button type="button" className="botao-texto" disabled={acao.enviando} onClick={alternarCliente}>
            {c.ativo ? "Inativar" : "Reativar"}
          </button>
        )}
      </Cabecalho>
      {c.espelhado && (
        <p className="secundario">
          <SeloOrigem fonte="manutec_clientes" /> Nome, endereço, contato e situação vêm do Manutec Clientes e só mudam
          lá.
        </p>
      )}
      <MensagemErro erro={acao.erro} />

      {!c.ativo && <div className="aviso alerta">Cliente inativo: fora do painel e da fila de oportunidades.</div>}
      {c.cadastroIncompleto && (
        <div className="aviso alerta">
          Cliente vindo da base antiga, ainda sem estações e válvulas. O cadastro se completa no primeiro lançamento.
        </div>
      )}

      <dl className="ficha">
        <div>
          <dt>Código</dt>
          <dd className="mono">{c.codigoReferencia}</dd>
        </div>
        <div>
          <dt>Endereço</dt>
          <dd>{[c.endereco, c.bairro, c.cidade].filter(Boolean).join(" · ") || "—"}</dd>
        </div>
        <div>
          <dt>Síndico ou responsável</dt>
          <dd>{c.sindicoResponsavel ?? "—"}</dd>
        </div>
        <div>
          <dt>Contato</dt>
          <dd>{[c.telefoneContato, c.emailContato].filter(Boolean).join(" · ") || "—"}</dd>
        </div>
        <div>
          <dt>Atendimento</dt>
          <dd>
            {c.tipoAtendimento} · contrato {c.statusContrato.toLowerCase()}
          </dd>
        </div>
        {c.ultimaManutencaoLegado && (
          <div>
            <dt>Última manutenção (base antiga)</dt>
            <dd>
              {formatarData(c.ultimaManutencaoLegado)}
              {c.dataLegadoAproximada && <span className="secundario"> · data aproximada</span>}
            </dd>
          </div>
        )}
      </dl>

      <div className="cabecalho-secao">
        <h2>Estações</h2>
        {externo ? (
          <NotaGerido oQue="Novas estações" fonte="jiraya" />
        ) : (
          c.ativo && (
          <button type="button" className="botao" onClick={() => setJanela({ tipo: "nova-estacao" })}>
            + Nova estação
            </button>
          )
        )}
      </div>

      {c.estacoes.length === 0 && <p className="aviso">Nenhuma estação cadastrada.</p>}

      {c.estacoes.map((e) => (
        <section key={e.id} className={`bloco ${e.ativo ? "" : "inativo"}`}>
          <div className="bloco-topo">
            <div>
              <h3>
                {e.localizacaoInstalacao}
                {e.derivada && <SeloOrigem fonte="jiraya" />}
              </h3>
              <p className="secundario">
                Andares: {e.andares}
                {e.requerFechamentoGeral && <span className="selo-alerta">fechamento geral</span>}
                {!e.ativo && <span className="selo">inativa</span>}
              </p>
            </div>
            <div className="acoes">
              {e.ativo && !externo && (
                <button type="button" className="botao" onClick={() => setJanela({ tipo: "nova-valvula", estacao: e })}>
                  + Válvula
                </button>
              )}
              <button type="button" className="botao-texto" onClick={() => setJanela({ tipo: "estacao", estacao: e })}>
                {e.derivada ? "Completar" : "Editar"}
              </button>
              {ehAdmin && !e.derivada && (
                <button type="button" className="botao-texto" onClick={() => alternarEstacao(e)}>
                  {e.ativo ? "Inativar" : "Reativar"}
                </button>
              )}
            </div>
          </div>
          {e.aCompletar && (
            <div className="aviso alerta">
              Andares a completar: o Jiraya não informa os andares desta estação.{" "}
              <button
                type="button"
                className="botao-texto"
                onClick={() => setJanela({ tipo: "estacao", estacao: e })}
              >
                Completar andares
              </button>
            </div>
          )}
          {e.valvulas.length === 0 ? (
            <p className="secundario">Nenhuma válvula nesta estação.</p>
          ) : (
            <ul className="lista-valvulas">
              {e.valvulas.map((v) => (
                <li key={v.id} className={v.ativo ? "" : "inativo"}>
                  <Link to={`/valvulas/${v.id}`} className="mono">
                    {v.codigoReferencia}
                  </Link>
                  <span>Nº {v.numeroValvula}</span>
                  <span className="secundario">{v.especificacao}</span>
                  {v.espelhada && <SeloOrigem fonte="jiraya" />}
                  {v.numeroSerie && <span className="secundario">série {v.numeroSerie}</span>}
                  {v.periodicidadeMeses !== 12 && <span className="selo">a cada {v.periodicidadeMeses} meses</span>}
                  {!v.ativo && <span className="selo">inativa</span>}
                  <button type="button" className="botao-texto" onClick={() => setJanela({ tipo: "valvula", valvula: v })}>
                    {v.espelhada ? "Completar" : "Editar"}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      ))}

      <ConjuntosDoCliente clienteId={c.id} clienteAtivo={c.ativo} />

      <Modal
        titulo={c.espelhado ? "Completar cliente" : "Editar cliente"}
        aberto={janela?.tipo === "cliente"}
        aoFechar={() => setJanela(null)}
      >
        {c.espelhado ? (
          <FormComplementoCliente cliente={c} aoSalvar={fecharERecarregar} />
        ) : (
          <FormCliente cliente={c} aoSalvar={fecharERecarregar} />
        )}
      </Modal>
      <Modal titulo="Nova estação" aberto={janela?.tipo === "nova-estacao"} aoFechar={() => setJanela(null)}>
        <FormEstacao clienteId={c.id} aoSalvar={fecharERecarregar} />
      </Modal>
      <Modal
        titulo={janela?.tipo === "estacao" && janela.estacao.derivada ? "Completar estação" : "Editar estação"}
        aberto={janela?.tipo === "estacao"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "estacao" &&
          (janela.estacao.derivada ? (
            <FormComplementoEstacao estacao={janela.estacao} aoSalvar={fecharERecarregar} />
          ) : (
            <FormEstacao clienteId={c.id} estacao={janela.estacao} aoSalvar={fecharERecarregar} />
          ))}
      </Modal>
      <Modal titulo="Nova válvula" aberto={janela?.tipo === "nova-valvula"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "nova-valvula" && <FormValvula estacaoId={janela.estacao.id} aoSalvar={fecharERecarregar} />}
      </Modal>
      <Modal
        titulo={janela?.tipo === "valvula" && janela.valvula.espelhada ? "Completar válvula" : "Editar válvula"}
        aberto={janela?.tipo === "valvula"} aoFechar={() => setJanela(null)}>
        {janela?.tipo === "valvula" &&
          (janela.valvula.espelhada ? (
            <FormComplementoValvula valvula={janela.valvula} aoSalvar={fecharERecarregar} />
          ) : (
            <FormValvula estacaoId={janela.valvula.estacaoId} valvula={janela.valvula} aoSalvar={fecharERecarregar} />
          ))}
      </Modal>
    </>
  );
}
