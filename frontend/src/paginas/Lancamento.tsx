import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import type { ClienteDetalhe, Estacao, ItemLancamento, ResultadoLancamento, TipoManutencao } from "../api/tipos";
import { clientes, servicos, usuarios } from "../api/valtis";
import { Cabecalho, Campo, Carregando, MensagemErro, Modal } from "../componentes/Basicos";
import { FormCliente, FormEstacao, FormValvula } from "../componentes/Formularios";
import { useSessao } from "../sessao/useSessao";
import { formatarData, hojeEmRecife, numeroOuNulo } from "../util/formato";
import { useAtrasado } from "../util/useAtrasado";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";

/** O que pode variar de uma válvula para outra na mesma visita. */
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

/**
 * Lançamento de serviço (RF-14 a RF-18).
 *
 * O registro é por válvula (D-18), mas o técnico preenche UMA vez e a tela
 * replica para as válvulas marcadas (RF-15b). Só o que for diferente é
 * ajustado por válvula. Cada válvula vira um registro independente no servidor.
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
        <button type="button" className="botao" onClick={() => setCriando(true)}>
          + Novo condomínio
        </button>
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
      {dados?.length === 0 && <p className="aviso">Nenhum condomínio encontrado. Cadastre um novo.</p>}

      <Modal titulo="Novo condomínio" aberto={criando} aoFechar={() => setCriando(false)}>
        <FormCliente aoSalvar={(c) => aoEscolher(c.id)} />
      </Modal>
    </section>
  );
}

type PropsFormulario = {
  clienteId: string;
  valvulaInicial: string | null;
  aoTrocarCliente: () => void;
  aoConcluir: (resultado: ResultadoLancamento[]) => void;
};

function FormularioLancamento({ clienteId, valvulaInicial, aoTrocarCliente, aoConcluir }: PropsFormulario) {
  const { usuario } = useSessao();
  const cliente = useCarga(() => clientes.detalhar(clienteId), [clienteId]);
  const tecnicos = useCarga(() => usuarios.ativos(), []);

  const [estacaoId, setEstacaoId] = useState<string | null>(null);
  const [marcadas, setMarcadas] = useState<Set<string>>(new Set());
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

  // Ao abrir pela ficha de uma válvula, já deixa a estação e a válvula escolhidas.
  useEffect(() => {
    if (!cliente.dados || estacaoId) return;
    const daValvula = valvulaInicial
      ? estacoesAtivas.find((e) => e.valvulas.some((v) => v.id === valvulaInicial))
      : undefined;
    const escolhida = daValvula ?? (estacoesAtivas.length === 1 ? estacoesAtivas[0] : undefined);
    if (escolhida) escolherEstacao(escolhida, valvulaInicial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cliente.dados]);

  function escolherEstacao(e: Estacao, soEsta: string | null = null) {
    setEstacaoId(e.id);
    setAjustes({});
    const ativas = e.valvulas.filter((v) => v.ativo).map((v) => v.id);
    // RF-15b: por padrão, a visita cobre todas as válvulas da estação.
    setMarcadas(new Set(soEsta && ativas.includes(soEsta) ? [soEsta] : ativas));
  }

  function alternar(valvulaId: string) {
    setMarcadas((atual) => {
      const nova = new Set(atual);
      if (nova.has(valvulaId)) nova.delete(valvulaId);
      else nova.add(valvulaId);
      return nova;
    });
  }

  function ajustar(valvulaId: string, ligado: boolean) {
    setAjustes((atual) => {
      const novo = { ...atual };
      if (ligado) novo[valvulaId] = { ...comum };
      else delete novo[valvulaId];
      return novo;
    });
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const itens: ItemLancamento[] = valvulasAtivas
      .filter((v) => marcadas.has(v.id))
      .map((v) => {
        const d = ajustes[v.id] ?? comum;
        return {
          valvulaId: v.id,
          dataRealizada,
          tipoManutencao: (d.tipoManutencao || comum.tipoManutencao) as TipoManutencao,
          tecnicoResponsavelId: tecnicoId,
          pecasSubstituidas: d.pecasSubstituidas,
          pressaoEntrada: numeroOuNulo(d.pressaoEntrada),
          pressaoSaida: numeroOuNulo(d.pressaoSaida),
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
  const podeEnviar = marcadas.size > 0 && dataRealizada && comum.tipoManutencao && tecnicoId;

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
            <button type="button" className="botao-texto" onClick={() => setModal("estacao")}>
              + Nova estação
            </button>
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
              <button type="button" className="botao-texto" onClick={() => setModal("valvula")}>
                + Nova válvula
              </button>
            </div>
            {valvulasAtivas.length === 0 && <p className="aviso">Esta estação ainda não tem válvula cadastrada.</p>}
            <div className="opcoes">
              {valvulasAtivas.map((v) => (
                <label key={v.id} className={`opcao ${marcadas.has(v.id) ? "selecionada" : ""}`}>
                  <input type="checkbox" checked={marcadas.has(v.id)} onChange={() => alternar(v.id)} />
                  <strong className="mono">{v.codigoReferencia}</strong>
                  <span className="secundario">
                    Nº {v.numeroValvula} · {v.especificacao}
                  </span>
                </label>
              ))}
            </div>
          </section>
        )}

        {estacao && marcadas.size > 0 && (
          <section className="etapa">
            <h2>4. O que foi feito</h2>
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
            <CamposDetalhe valor={comum} aoMudar={setComum} obrigatorio />

            {marcadas.size > 1 && (
              <div className="ajustes">
                <h3>Alguma válvula teve algo diferente?</h3>
                <p className="secundario">
                  O preenchimento acima vale para as {marcadas.size} válvulas. Ajuste só a que for diferente.
                </p>
                {valvulasAtivas
                  .filter((v) => marcadas.has(v.id))
                  .map((v) => (
                    <div key={v.id} className="ajuste">
                      <label className="caixa">
                        <input
                          type="checkbox"
                          checked={v.id in ajustes}
                          onChange={(e) => ajustar(v.id, e.target.checked)}
                        />
                        Ajustar <span className="mono">{v.codigoReferencia}</span>
                      </label>
                      {ajustes[v.id] && (
                        <CamposDetalhe
                          valor={ajustes[v.id]}
                          aoMudar={(d) => setAjustes((a) => ({ ...a, [v.id]: d }))}
                          obrigatorio={false}
                        />
                      )}
                    </div>
                  ))}
              </div>
            )}

            <button type="submit" className="botao primario largo" disabled={!podeEnviar || envio.enviando}>
              {envio.enviando
                ? "Salvando…"
                : `Salvar ${marcadas.size} ${marcadas.size === 1 ? "registro" : "registros"}`}
            </button>
          </section>
        )}
      </form>

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
  );
}

function CamposDetalhe({
  valor,
  aoMudar,
  obrigatorio,
}: {
  valor: Detalhe;
  aoMudar: (d: Detalhe) => void;
  obrigatorio: boolean;
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
          <option value="">{obrigatorio ? "Escolha…" : "Igual às demais"}</option>
          <option value="Preventiva">Preventiva</option>
          <option value="Corretiva">Corretiva</option>
        </select>
      </Campo>
      <Campo rotulo="Peças substituídas">
        <input value={valor.pecasSubstituidas} onChange={(e) => mudar("pecasSubstituidas", e.target.value)} />
      </Campo>
      <div className="linha-campos">
        <Campo rotulo="Pressão de entrada" dica="Opcional">
          <input
            inputMode="decimal"
            value={valor.pressaoEntrada}
            onChange={(e) => mudar("pressaoEntrada", e.target.value)}
          />
        </Campo>
        <Campo rotulo="Pressão de saída" dica="Opcional">
          <input
            inputMode="decimal"
            value={valor.pressaoSaida}
            onChange={(e) => mudar("pressaoSaida", e.target.value)}
          />
        </Campo>
      </div>
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
            <Link to={`/valvulas/${r.valvulaId}`} className="mono">
              {r.codigoValvula}
            </Link>
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
