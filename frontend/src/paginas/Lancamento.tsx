import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import type {
  Bomba,
  ClienteDetalhe,
  Estacao,
  ItemLancamento,
  ResultadoLancamento,
  TipoManutencao,
} from "../api/tipos";
import { bombas, clientes, servicos, usuarios } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, Modal, NotaGerido } from "../componentes/Basicos";
import { FormCliente, FormEstacao, FormValvula } from "../componentes/Formularios";
import { useSessao } from "../sessao/useSessao";
import { useSistema } from "../sessao/useSistema";
import { formatarData, hojeEmRecife, numeroOuNulo, ROTULO_MANUTENCAO } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/** O que pode variar de um equipamento para outro na mesma visita. */
type Detalhe = {
  tipoManutencao: TipoManutencao | "";
  pecasSubstituidas: string;
  pressaoEntrada: string;
  pressaoSaida: string;
  observacoes: string;
};

const DETALHE_VAZIO: Detalhe = {
  tipoManutencao: "",
  pecasSubstituidas: "",
  pressaoEntrada: "",
  pressaoSaida: "",
  observacoes: "",
};

const TIPOS_MANUTENCAO: TipoManutencao[] = ["Preventiva", "Corretiva", "Instalacao"];

/**
 * Lançamento de serviço (RF-14 a RF-18, RF-80).
 *
 * O registro é por equipamento (D-18, D-31): válvula ou bomba. O técnico preenche
 * UMA vez e a tela replica para os marcados (RF-15b). Só o que for diferente é
 * ajustado por equipamento. Cada um vira um registro independente no servidor.
 */
export function Lancamento() {
  const [parametros] = useSearchParams();
  const [clienteId, setClienteId] = useState<string | null>(parametros.get("cliente"));
  const [resultado, setResultado] = useState<ResultadoLancamento[] | null>(null);

  if (resultado) {
    return (
      <Concluido
        resultado={resultado}
        aoLancarOutro={() => {
          setResultado(null);
          setClienteId(null);
        }}
      />
    );
  }

  return (
    <>
      <Cabecalho titulo="Lançar serviço" />
      {clienteId ? (
        <FormularioLancamento
          clienteId={clienteId}
          valvulaInicial={parametros.get("valvula")}
          bombaInicial={parametros.get("bomba")}
          aoTrocarCliente={() => setClienteId(null)}
          aoConcluir={setResultado}
        />
      ) : (
        <EscolhaCliente aoEscolher={setClienteId} />
      )}
    </>
  );
}

function EscolhaCliente({ aoEscolher }: { aoEscolher: (id: string) => void }) {
  const { externo } = useSistema();
  const [busca, setBusca] = useState("");
  const [criando, setCriando] = useState(false);
  const buscaAtrasada = useAtrasado(busca);
  const { dados, carregando, erro } = useCarga(() => clientes.listar(buscaAtrasada, false), [buscaAtrasada]);

  return (
    <section className="etapa">
      <h2>1. Qual condomínio?</h2>
      <div className="filtros">
        <input
          type="search"
          autoFocus
          placeholder="Nome, código ou bairro"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          aria-label="Buscar condomínio"
        />
        {externo ? (
          <NotaGerido oQue="Novo condomínio" fonte="manutec_clientes" />
        ) : (
          <button type="button" className="botao" onClick={() => setCriando(true)}>
            + Novo condomínio
          </button>
        )}
      </div>
      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}
      <ul className="lista-escolha">
        {dados?.map((c) => (
          <li key={c.id}>
            <button type="button" className="item-escolha" onClick={() => aoEscolher(c.id)}>
              <strong>{c.nomeCondominio}</strong>
              <span className="secundario">
                {c.codigoReferencia}
                {c.bairro && ` · ${c.bairro}`}
              </span>
            </button>
          </li>
        ))}
      </ul>
      {dados?.length === 0 && (
        <p className="aviso">
          Nenhum condomínio encontrado.{externo ? " Os condomínios vêm do Manutec Clientes." : " Cadastre um novo."}
        </p>
      )}

      {!externo && (
        <Modal titulo="Novo condomínio" aberto={criando} aoFechar={() => setCriando(false)}>
          <FormCliente aoSalvar={(c) => aoEscolher(c.id)} />
        </Modal>
      )}
    </section>
  );
}

type PropsFormulario = {
  clienteId: string;
  valvulaInicial: string | null;
  bombaInicial: string | null;
  aoTrocarCliente: () => void;
  aoConcluir: (resultado: ResultadoLancamento[]) => void;
};

/** Válvula ou bomba marcada, já com o que a tela precisa mostrar e enviar. */
type Alvo = { id: string; tipo: "valvula" | "bomba"; codigo: string };

function FormularioLancamento({
  clienteId,
  valvulaInicial,
  bombaInicial,
  aoTrocarCliente,
  aoConcluir,
}: PropsFormulario) {
  const { usuario } = useSessao();
  const { externo } = useSistema();
  const cliente = useCarga(() => clientes.detalhar(clienteId), [clienteId]);
  const bombasDoCliente = useCarga(() => bombas.doCliente(clienteId), [clienteId]);
  const tecnicos = useCarga(() => usuarios.ativos(), []);

  const [estacaoId, setEstacaoId] = useState<string | null>(null);
  const [marcadas, setMarcadas] = useState<Set<string>>(new Set());
  const [bombasMarcadas, setBombasMarcadas] = useState<Set<string>>(() => new Set(bombaInicial ? [bombaInicial] : []));
  // RF-17: começa em hoje, mas a tela pede para conferir; lançar com atraso
  // sem trocar a data empurraria o próximo ciclo (D-02).
  const [dataRealizada, setDataRealizada] = useState(hojeEmRecife);
  const [tecnicoId, setTecnicoId] = useState(usuario?.id ?? "");
  const [comum, setComum] = useState<Detalhe>(DETALHE_VAZIO);
  const [ajustes, setAjustes] = useState<Record<string, Detalhe>>({});
  const [modal, setModal] = useState<"estacao" | "valvula" | null>(null);
  const envio = useEnvio();

  const estacoesAtivas = useMemo(() => (cliente.dados?.estacoes ?? []).filter((e) => e.ativo), [cliente.dados]);
  const estacao: Estacao | undefined = estacoesAtivas.find((e) => e.id === estacaoId);
  const valvulasAtivas = (estacao?.valvulas ?? []).filter((v) => v.ativo);
  // Bomba patrimonial da Manutec não recebe serviço por aqui: o servidor recusa (RF-80).
  const bombasAtendiveis: Bomba[] = (bombasDoCliente.dados ?? []).filter((b) => b.ativo && !b.patrimonioManutec);

  const alvos: Alvo[] = [
    ...valvulasAtivas
      .filter((v) => marcadas.has(v.id))
      .map((v): Alvo => ({ id: v.id, tipo: "valvula", codigo: v.codigoReferencia })),
    ...bombasAtendiveis
      .filter((b) => bombasMarcadas.has(b.id))
      .map((b): Alvo => ({ id: b.id, tipo: "bomba", codigo: b.codigoReferencia })),
  ];
  const temValvula = alvos.some((a) => a.tipo === "valvula");

  // Ao abrir pela ficha de uma válvula, já deixa a estação e a válvula escolhidas.
  // Ao abrir pela ficha de uma bomba, só a bomba vem marcada.
  useEffect(() => {
    if (!cliente.dados || estacaoId || bombaInicial) return;
    const daValvula = valvulaInicial
      ? estacoesAtivas.find((e) => e.valvulas.some((v) => v.id === valvulaInicial))
      : undefined;
    const escolhida = daValvula ?? (estacoesAtivas.length === 1 ? estacoesAtivas[0] : undefined);
    if (escolhida) escolherEstacao(escolhida, valvulaInicial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cliente.dados]);

  function escolherEstacao(e: Estacao, soEsta: string | null = null) {
    setEstacaoId(e.id);
    // Os ajustes das bombas continuam valendo: trocar de estação não mexe nelas.
    setAjustes((atual) => Object.fromEntries(Object.entries(atual).filter(([id]) => bombasMarcadas.has(id))));
    const ativas = e.valvulas.filter((v) => v.ativo).map((v) => v.id);
    // RF-15b: por padrão, a visita cobre todas as válvulas da estação.
    setMarcadas(new Set(soEsta && ativas.includes(soEsta) ? [soEsta] : ativas));
  }

  function alternarEm(definir: typeof setMarcadas, id: string) {
    definir((atual) => {
      const nova = new Set(atual);
      if (nova.has(id)) nova.delete(id);
      else nova.add(id);
      return nova;
    });
  }

  function ajustar(id: string, ligado: boolean) {
    setAjustes((atual) => {
      const novo = { ...atual };
      if (ligado) novo[id] = { ...comum };
      else delete novo[id];
      return novo;
    });
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const itens: ItemLancamento[] = alvos.map((a) => {
      const d = ajustes[a.id] ?? comum;
      const ehValvula = a.tipo === "valvula";
      return {
        valvulaId: ehValvula ? a.id : null,
        bombaId: ehValvula ? null : a.id,
        dataRealizada,
        tipoManutencao: (d.tipoManutencao || comum.tipoManutencao) as TipoManutencao,
        tecnicoResponsavelId: tecnicoId,
        pecasSubstituidas: d.pecasSubstituidas,
        // Bomba não tem pressão de entrada e saída (RF-80).
        pressaoEntrada: ehValvula ? numeroOuNulo(d.pressaoEntrada) : null,
        pressaoSaida: ehValvula ? numeroOuNulo(d.pressaoSaida) : null,
        observacoes: d.observacoes,
      };
    });
    const r = await envio.executar(() => servicos.lancar(itens));
    if (r.ok) aoConcluir(r.valor);
  }

  async function aposCriarCadastro() {
    setModal(null);
    cliente.recarregar();
  }

  if (cliente.carregando && !cliente.dados) return <Carregando />;
  if (cliente.erro || !cliente.dados) return <MensagemErro erro={cliente.erro} />;

  const c: ClienteDetalhe = cliente.dados;
  const podeEnviar = alvos.length > 0 && dataRealizada && comum.tipoManutencao && tecnicoId;
  const passoBombas = estacao ? 4 : 3;
  const passoDetalhe = passoBombas + (bombasAtendiveis.length > 0 ? 1 : 0);

  return (
    <>
      <form className="lancamento" onSubmit={enviar}>
        <section className="etapa">
          <div className="etapa-topo">
            <h2>1. {c.nomeCondominio}</h2>
            <button type="button" className="botao-texto" onClick={aoTrocarCliente}>
              Trocar
            </button>
          </div>
          <p className="secundario">
            {c.codigoReferencia}
            {c.bairro && ` · ${c.bairro}`}
          </p>
        </section>

        <section className="etapa">
          <div className="etapa-topo">
            <h2>2. Qual estação?</h2>
            {externo ? (
              <NotaGerido oQue="Nova estação" fonte="jiraya" />
            ) : (
              <button type="button" className="botao-texto" onClick={() => setModal("estacao")}>
                + Nova estação
              </button>
            )}
          </div>
          {estacoesAtivas.length === 0 && <p className="aviso">Este condomínio ainda não tem estação cadastrada.</p>}
          <div className="opcoes">
            {estacoesAtivas.map((e) => (
              <button
                key={e.id}
                type="button"
                className={`opcao ${e.id === estacaoId ? "selecionada" : ""}`}
                aria-pressed={e.id === estacaoId}
                onClick={() => escolherEstacao(e)}
              >
                <strong>{e.localizacaoInstalacao}</strong>
                <span className="secundario">
                  {e.andares} · {e.valvulas.filter((v) => v.ativo).length} válvula(s)
                </span>
                {e.requerFechamentoGeral && <span className="selo-alerta">fechamento geral</span>}
              </button>
            ))}
          </div>
        </section>

        {estacao && (
          <section className="etapa">
            <div className="etapa-topo">
              <h2>3. Válvulas atendidas</h2>
              {externo ? (
                <NotaGerido oQue="Nova válvula" fonte="jiraya" />
              ) : (
                <button type="button" className="botao-texto" onClick={() => setModal("valvula")}>
                  + Nova válvula
                </button>
              )}
            </div>
            {valvulasAtivas.length === 0 && <p className="aviso">Esta estação ainda não tem válvula cadastrada.</p>}
            <div className="opcoes">
              {valvulasAtivas.map((v) => (
                <label key={v.id} className={`opcao ${marcadas.has(v.id) ? "selecionada" : ""}`}>
                  <input type="checkbox" checked={marcadas.has(v.id)} onChange={() => alternarEm(setMarcadas, v.id)} />
                  <strong className="mono">{v.codigoReferencia}</strong>
                  <span className="secundario">
                    Nº {v.numeroValvula} · {v.especificacao}
                  </span>
                </label>
              ))}
            </div>
          </section>
        )}

        {bombasAtendiveis.length > 0 && (
          <section className="etapa">
            <h2>{passoBombas}. Bombas atendidas</h2>
            <MensagemErro erro={bombasDoCliente.erro} />
            <div className="opcoes">
              {bombasAtendiveis.map((b) => (
                <label key={b.id} className={`opcao ${bombasMarcadas.has(b.id) ? "selecionada" : ""}`}>
                  <input
                    type="checkbox"
                    checked={bombasMarcadas.has(b.id)}
                    onChange={() => alternarEm(setBombasMarcadas, b.id)}
                  />
                  <strong className="mono">{b.codigoReferencia}</strong>
                  <span className="secundario">{b.descricao}</span>
                </label>
              ))}
            </div>
          </section>
        )}

        {alvos.length > 0 && (
          <section className="etapa">
            <h2>{passoDetalhe}. O que foi feito</h2>
            <MensagemErro erro={envio.erro ?? tecnicos.erro} />
            <div className="linha-campos">
              <Campo
                rotulo="Data em que o serviço foi feito *"
                erro={envio.campos["itens[0].dataRealizada"]}
                dica={
                  dataRealizada === hojeEmRecife()
                    ? "Confira: é hoje? Se o serviço foi feito em outro dia, troque a data."
                    : undefined
                }
              >
                <input
                  type="date"
                  required
                  max={hojeEmRecife()}
                  value={dataRealizada}
                  onChange={(e) => setDataRealizada(e.target.value)}
                />
              </Campo>
              <Campo rotulo="Técnico responsável *">
                <select required value={tecnicoId} onChange={(e) => setTecnicoId(e.target.value)}>
                  <option value="">Escolha…</option>
                  {tecnicos.dados?.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.nome}
                    </option>
                  ))}
                </select>
              </Campo>
            </div>
            <CamposDetalhe valor={comum} aoMudar={setComum} obrigatorio comPressao={temValvula} />

            {alvos.length > 1 && (
              <div className="ajustes">
                <h3>Algum equipamento teve algo diferente?</h3>
                <p className="secundario">
                  O preenchimento acima vale para os {alvos.length} equipamentos. Ajuste só o que for diferente.
                </p>
                {alvos.map((a) => (
                  <div key={a.id} className="ajuste">
                    <label className="caixa">
                      <input type="checkbox" checked={a.id in ajustes} onChange={(e) => ajustar(a.id, e.target.checked)} />
                      Ajustar <span className="mono">{a.codigo}</span>
                    </label>
                    {ajustes[a.id] && (
                      <CamposDetalhe
                        valor={ajustes[a.id]}
                        aoMudar={(d) => setAjustes((atual) => ({ ...atual, [a.id]: d }))}
                        obrigatorio={false}
                        comPressao={a.tipo === "valvula"}
                      />
                    )}
                  </div>
                ))}
              </div>
            )}

            <button type="submit" className="botao primario largo" disabled={!podeEnviar || envio.enviando}>
              {envio.enviando
                ? "Salvando…"
                : `Salvar ${alvos.length} ${alvos.length === 1 ? "registro" : "registros"}`}
            </button>
          </section>
        )}
      </form>

      {!externo && (
        <>
          <Modal titulo="Nova estação" aberto={modal === "estacao"} aoFechar={() => setModal(null)}>
            <FormEstacao clienteId={c.id} aoSalvar={(e) => aposCriarCadastro().then(() => setEstacaoId(e.id))} />
          </Modal>
          <Modal titulo="Nova válvula" aberto={modal === "valvula"} aoFechar={() => setModal(null)}>
            {estacao && (
              <FormValvula
                estacaoId={estacao.id}
                aoSalvar={(v) => {
                  setMarcadas((m) => new Set(m).add(v.id));
                  void aposCriarCadastro();
                }}
              />
            )}
          </Modal>
        </>
      )}
    </>
  );
}

function CamposDetalhe({
  valor,
  aoMudar,
  obrigatorio,
  comPressao,
}: {
  valor: Detalhe;
  aoMudar: (d: Detalhe) => void;
  obrigatorio: boolean;
  /** Pressão só existe na válvula; no item de bomba os campos nem aparecem. */
  comPressao: boolean;
}) {
  const mudar = (campo: keyof Detalhe, texto: string) => aoMudar({ ...valor, [campo]: texto });
  return (
    <div className="detalhe">
      <Campo rotulo={obrigatorio ? "Tipo de manutenção *" : "Tipo de manutenção"}>
        <select
          required={obrigatorio}
          value={valor.tipoManutencao}
          onChange={(e) => mudar("tipoManutencao", e.target.value)}
        >
          <option value="">{obrigatorio ? "Escolha…" : "Igual aos demais"}</option>
          {TIPOS_MANUTENCAO.map((t) => (
            <option key={t} value={t}>
              {ROTULO_MANUTENCAO[t]}
            </option>
          ))}
        </select>
      </Campo>
      <Campo rotulo="Peças substituídas">
        <input value={valor.pecasSubstituidas} onChange={(e) => mudar("pecasSubstituidas", e.target.value)} />
      </Campo>
      {comPressao && (
        <div className="linha-campos">
          <Campo rotulo="Pressão de entrada" dica="Opcional. Só para válvula.">
            <input
              inputMode="decimal"
              value={valor.pressaoEntrada}
              onChange={(e) => mudar("pressaoEntrada", e.target.value)}
            />
          </Campo>
          <Campo rotulo="Pressão de saída" dica="Opcional. Só para válvula.">
            <input
              inputMode="decimal"
              value={valor.pressaoSaida}
              onChange={(e) => mudar("pressaoSaida", e.target.value)}
            />
          </Campo>
        </div>
      )}
      <Campo rotulo="Observações">
        <textarea rows={3} value={valor.observacoes} onChange={(e) => mudar("observacoes", e.target.value)} />
      </Campo>
    </div>
  );
}

function Concluido({ resultado, aoLancarOutro }: { resultado: ResultadoLancamento[]; aoLancarOutro: () => void }) {
  return (
    <>
      <Cabecalho titulo="Serviço salvo" />
      <div className="aviso sucesso" role="status">
        {resultado.length === 1 ? "1 registro salvo." : `${resultado.length} registros salvos.`} Os lembretes vão para a
        agenda automaticamente.
      </div>
      <ul className="lista-resultado">
        {resultado.map((r) => (
          <li key={r.servicoId}>
            {r.bombaId ? (
              <Link to={`/bombas/${r.bombaId}`} className="mono">
                {r.codigoBomba}
              </Link>
            ) : (
              <Link to={`/valvulas/${r.valvulaId}`} className="mono">
                {r.codigoValvula}
              </Link>
            )}
            <span>
              {r.proximaManutencao
                ? `Próxima manutenção em ${formatarData(r.proximaManutencao)}`
                : "Serviço mais antigo que o último registrado: entrou só no histórico."}
            </span>
          </li>
        ))}
      </ul>
      <div className="acoes">
        <button type="button" className="botao primario" onClick={aoLancarOutro}>
          Lançar outro serviço
        </button>
        <Link to="/" className="botao">
          Ver painel
        </Link>
      </div>
    </>
  );
}
