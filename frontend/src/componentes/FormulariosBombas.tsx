// Formulários do módulo de bombas (RF-61, RF-62, RF-66). Mesmo desenho dos de válvula.
import { useState, type FormEvent } from "react";
import type { Bomba, Conjunto, DadosBomba, DadosConjunto, DadosInstalacao, Ocupacao, TipoBomba } from "../api/tipos";
import { bombas, conjuntos, tiposBomba } from "../api/valtis";
import { hojeEmRecife, numeroOuNulo, textoOuNulo } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";
import { Campo, MensagemErro } from "./Basicos";

type PropsFormConjunto = { clienteId: string; conjunto?: Conjunto; aoSalvar: (conjunto: Conjunto) => void };

export function FormConjunto({ clienteId, conjunto, aoSalvar }: PropsFormConjunto) {
  const catalogo = useCarga(() => tiposBomba.listar(false), []);
  const [dados, setDados] = useState<DadosConjunto>({
    tipoId: conjunto?.tipoId ?? "",
    localizacao: conjunto?.localizacao ?? "",
    observacoes: conjunto?.observacoes ?? "",
  });
  const { enviando, erro, campos, executar } = useEnvio();
  const mudar = (campo: keyof DadosConjunto, valor: string) => setDados((d) => ({ ...d, [campo]: valor }));
  const tipos: TipoBomba[] = catalogo.dados ?? [];

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const resultado = await executar(() =>
      conjunto ? conjuntos.atualizar(conjunto.id, dados) : conjuntos.criar(clienteId, dados),
    );
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro ?? catalogo.erro} />
      <Campo rotulo="Tipo *" erro={campos.tipoId} dica="Recalque, incêndio, drenagem…">
        <select required value={dados.tipoId} onChange={(e) => mudar("tipoId", e.target.value)}>
          <option value="">Escolha…</option>
          {tipos.map((t) => (
            <option key={t.id} value={t.id}>
              {t.nome}
            </option>
          ))}
        </select>
      </Campo>
      <Campo rotulo="Localização *" erro={campos.localizacao} dica="Ex.: Casa de bombas - subsolo 2">
        <input required value={dados.localizacao} onChange={(e) => mudar("localizacao", e.target.value)} />
      </Campo>
      <Campo rotulo="Observações" erro={campos.observacoes}>
        <textarea rows={2} value={dados.observacoes} onChange={(e) => mudar("observacoes", e.target.value)} />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : conjunto ? "Salvar alterações" : "Cadastrar conjunto"}
      </button>
    </form>
  );
}

type PropsFormBomba = {
  /** Cliente dono. Ausente = bomba patrimonial da Manutec. */
  clienteId?: string;
  /** Conjuntos do cliente, para já instalar ao cadastrar. */
  conjuntosDoCliente?: Conjunto[];
  bomba?: Bomba;
  aoSalvar: (bomba: Bomba) => void;
};

export function FormBomba({ clienteId, conjuntosDoCliente = [], bomba, aoSalvar }: PropsFormBomba) {
  const [fabricante, setFabricante] = useState(bomba?.fabricante ?? "");
  const [modelo, setModelo] = useState(bomba?.modelo ?? "");
  const [potencia, setPotencia] = useState(bomba?.potenciaCv === null || !bomba ? "" : String(bomba.potenciaCv));
  const [tensao, setTensao] = useState(bomba?.tensaoVolts === null || !bomba ? "" : String(bomba.tensaoVolts));
  const [corrente, setCorrente] = useState(
    bomba?.correnteAmperes === null || !bomba ? "" : String(bomba.correnteAmperes),
  );
  const [funcao, setFuncao] = useState(bomba?.funcao ?? "");
  const [dataInstalacao, setDataInstalacao] = useState(bomba?.dataInstalacao ?? "");
  const [periodicidade, setPeriodicidade] = useState(bomba ? String(bomba.periodicidadeMeses) : "");
  const [ultima, setUltima] = useState(bomba?.ultimaManutencao ?? "");
  const [conjuntoId, setConjuntoId] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();
  const ativos = conjuntosDoCliente.filter((c) => c.ativo);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const dados: DadosBomba = {
      fabricante,
      modelo,
      potenciaCv: numeroOuNulo(potencia),
      tensaoVolts: numeroOuNulo(tensao),
      correnteAmperes: numeroOuNulo(corrente),
      funcao,
      dataInstalacao: dataInstalacao || null,
      periodicidadeMeses: numeroOuNulo(periodicidade),
      ultimaManutencao: ultima || null,
      conjuntoId: conjuntoId || null,
    };
    const resultado = await executar(() =>
      bomba
        ? bombas.atualizar(bomba.id, dados)
        : clienteId
          ? bombas.criarDoCondominio(clienteId, dados)
          : bombas.criarPatrimonial(dados),
    );
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <p className="secundario">Os dados técnicos podem ficar em branco e ser completados depois.</p>
      <div className="linha-campos">
        <Campo rotulo="Fabricante" erro={campos.fabricante}>
          <input placeholder="WEG" value={fabricante} onChange={(e) => setFabricante(e.target.value)} />
        </Campo>
        <Campo rotulo="Modelo" erro={campos.modelo}>
          <input placeholder="BC-92" value={modelo} onChange={(e) => setModelo(e.target.value)} />
        </Campo>
      </div>
      <div className="linha-campos">
        <Campo rotulo="Potência (CV)" erro={campos.potenciaCv}>
          <input inputMode="decimal" value={potencia} onChange={(e) => setPotencia(e.target.value)} />
        </Campo>
        <Campo rotulo="Tensão (V)" erro={campos.tensaoVolts}>
          <input inputMode="numeric" value={tensao} onChange={(e) => setTensao(e.target.value)} />
        </Campo>
        <Campo rotulo="Corrente (A)" erro={campos.correnteAmperes}>
          <input inputMode="decimal" value={corrente} onChange={(e) => setCorrente(e.target.value)} />
        </Campo>
      </div>
      <div className="linha-campos">
        <Campo rotulo="Função" erro={campos.funcao} dica="Ex.: principal, reserva">
          <input value={funcao} onChange={(e) => setFuncao(e.target.value)} />
        </Campo>
        <Campo rotulo="Data de instalação" erro={campos.dataInstalacao}>
          <input type="date" value={dataInstalacao} onChange={(e) => setDataInstalacao(e.target.value)} />
        </Campo>
      </div>
      <div className="linha-campos">
        <Campo rotulo="Periodicidade (meses) *" erro={campos.periodicidadeMeses}>
          <input
            type="number"
            inputMode="numeric"
            required
            min={1}
            max={60}
            value={periodicidade}
            onChange={(e) => setPeriodicidade(e.target.value)}
          />
        </Campo>
        <Campo rotulo="Última manutenção" erro={campos.ultimaManutencao} dica="A próxima é calculada.">
          <input type="date" value={ultima} onChange={(e) => setUltima(e.target.value)} />
        </Campo>
      </div>
      {!bomba && clienteId && ativos.length > 0 && (
        <Campo rotulo="Já instalar em" erro={campos.conjuntoId} dica="Entra na próxima posição livre do conjunto.">
          <select value={conjuntoId} onChange={(e) => setConjuntoId(e.target.value)}>
            <option value="">Só cadastrar, sem posição</option>
            {ativos.map((c) => (
              <option key={c.id} value={c.id}>
                {c.tipo} · {c.localizacao}
              </option>
            ))}
          </select>
        </Campo>
      )}
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : bomba ? "Salvar alterações" : clienteId ? "Cadastrar bomba" : "Cadastrar no patrimônio"}
      </button>
    </form>
  );
}

type PropsFormInstalacao = {
  bomba: Bomba;
  /** Conjuntos onde ela pode entrar. Para patrimônio, os do cliente escolhido. */
  conjuntosDisponiveis: Conjunto[];
  aoSalvar: (aviso: string | null) => void;
};

/** Colocar a bomba numa posição: instalação (do condomínio) ou empréstimo (patrimônio). */
export function FormInstalacao({ bomba, conjuntosDisponiveis, aoSalvar }: PropsFormInstalacao) {
  const [conjuntoId, setConjuntoId] = useState(conjuntosDisponiveis[0]?.id ?? "");
  const [substituidaId, setSubstituidaId] = useState("");
  const [numeroPosicao, setNumeroPosicao] = useState("");
  const [inicio, setInicio] = useState(hojeEmRecife());
  const [observacoes, setObservacoes] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();

  const conjunto = conjuntosDisponiveis.find((c) => c.id === conjuntoId);
  const ocupantes = (conjunto?.posicoes ?? []).filter((p) => p.bombaId !== bomba.id);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const dados: DadosInstalacao = {
      conjuntoId,
      // Vazio vira null: o @Pattern do servidor recusa "" e aceita ausência.
      numeroPosicao: textoOuNulo(numeroPosicao),
      inicio,
      bombaSubstituidaId: substituidaId || null,
      observacoes,
    };
    const resultado = await executar(() => bombas.instalar(bomba.id, dados));
    if (resultado.ok) aoSalvar(resultado.valor.aviso);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <Campo rotulo="Conjunto *" erro={campos.conjuntoId}>
        <select
          required
          value={conjuntoId}
          onChange={(e) => {
            setConjuntoId(e.target.value);
            setSubstituidaId("");
          }}
        >
          <option value="">Escolha…</option>
          {conjuntosDisponiveis.map((c) => (
            <option key={c.id} value={c.id}>
              {c.tipo} · {c.localizacao}
            </option>
          ))}
        </select>
      </Campo>
      {ocupantes.length > 0 && (
        <Campo
          rotulo="No lugar de"
          erro={campos.bombaSubstituidaId}
          dica={
            bomba.patrimonioManutec
              ? "A bomba do condomínio sai da posição (retirada) e esta entra no lugar."
              : "Se for uma emprestada, ela continua lá até você registrar a devolução."
          }
        >
          <select value={substituidaId} onChange={(e) => setSubstituidaId(e.target.value)}>
            <option value="">Nenhuma: nova posição</option>
            {ocupantes.map((p) => (
              <option key={p.ocupacaoId} value={p.bombaId}>
                Posição {p.numeroPosicao} · {p.codigoBomba} ({p.descricaoBomba})
              </option>
            ))}
          </select>
        </Campo>
      )}
      <div className="linha-campos">
        <Campo
          rotulo="Posição"
          erro={campos.numeroPosicao}
          dica={substituidaId ? "Vazio = a mesma da substituída." : "Vazio = próxima livre."}
        >
          <input inputMode="numeric" placeholder="01" value={numeroPosicao} onChange={(e) => setNumeroPosicao(e.target.value)} />
        </Campo>
        <Campo rotulo="Data de entrada *" erro={campos.inicio}>
          <input type="date" required value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </Campo>
      </div>
      <Campo rotulo="Observações" erro={campos.observacoes}>
        <textarea rows={2} value={observacoes} onChange={(e) => setObservacoes(e.target.value)} />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : bomba.patrimonioManutec ? "Registrar empréstimo" : "Registrar instalação"}
      </button>
    </form>
  );
}

type PropsFormEncerramento = { ocupacao: Ocupacao; aoSalvar: () => void };

/** Tirar a bomba da posição: devolução (empréstimo) ou retirada (instalação). */
export function FormEncerramento({ ocupacao, aoSalvar }: PropsFormEncerramento) {
  const [fim, setFim] = useState(hojeEmRecife());
  const [observacoes, setObservacoes] = useState("");
  const { enviando, erro, campos, executar } = useEnvio();
  const devolucao = ocupacao.natureza === "emprestimo";

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const resultado = await executar(() => bombas.encerrarOcupacao(ocupacao.id, fim, observacoes));
    if (resultado.ok) aoSalvar();
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <p className="secundario">
        {ocupacao.codigoBomba} está na posição {ocupacao.numeroPosicao} de {ocupacao.conjunto} ({ocupacao.nomeCondominio})
        desde {ocupacao.inicio.split("-").reverse().join("/")}.
      </p>
      <Campo rotulo={devolucao ? "Data da devolução *" : "Data da retirada *"} erro={campos.fim}>
        <input type="date" required value={fim} onChange={(e) => setFim(e.target.value)} />
      </Campo>
      <Campo rotulo="Observações" erro={campos.observacoes}>
        <textarea rows={2} value={observacoes} onChange={(e) => setObservacoes(e.target.value)} />
      </Campo>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : devolucao ? "Registrar devolução" : "Registrar retirada"}
      </button>
    </form>
  );
}
