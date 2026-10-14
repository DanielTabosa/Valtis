// Formulários de cadastro. Os mesmos componentes servem as telas de cadastro e
// a criação "de dentro do lançamento" (RF-10), sem trocar de tela.
import { useState, type FormEvent } from "react";
import type {
  ClienteDetalhe,
  DadosCliente,
  DadosEspecificacao,
  DadosEstacao,
  DadosValvula,
  Especificacao,
  Estacao,
  Valvula,
} from "../api/tipos";
import { clientes, especificacoes, estacoes, valvulas } from "../api/valtis";
import { numeroOuNulo } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { useEnvio } from "../util/useEnvio";
import { Campo, MensagemErro } from "./Basicos";

type PropsFormCliente = { cliente?: ClienteDetalhe; aoSalvar: (cliente: ClienteDetalhe) => void };

export function FormCliente({ cliente, aoSalvar }: PropsFormCliente) {
  const [dados, setDados] = useState<DadosCliente>({
    nomeCondominio: cliente?.nomeCondominio ?? "",
    endereco: cliente?.endereco ?? "",
    bairro: cliente?.bairro ?? "",
    cidade: cliente?.cidade ?? "Recife",
    sindicoResponsavel: cliente?.sindicoResponsavel ?? "",
    telefoneContato: cliente?.telefoneContato ?? "",
    emailContato: cliente?.emailContato ?? "",
    tipoAtendimento: cliente?.tipoAtendimento ?? "Avulso",
    statusContrato: cliente?.statusContrato ?? "Ativo",
  });
  const { enviando, erro, campos, executar } = useEnvio();
  const mudar = (campo: keyof DadosCliente, valor: string) => setDados((d) => ({ ...d, [campo]: valor }));

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const resultado = await executar(() => (cliente ? clientes.atualizar(cliente.id, dados) : clientes.criar(dados)));
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <Campo rotulo="Nome do condomínio *" erro={campos.nomeCondominio}>
        <input required value={dados.nomeCondominio} onChange={(e) => mudar("nomeCondominio", e.target.value)} />
      </Campo>
      <Campo rotulo="Endereço" erro={campos.endereco}>
        <input value={dados.endereco} onChange={(e) => mudar("endereco", e.target.value)} />
      </Campo>
      <div className="linha-campos">
        <Campo rotulo="Bairro" erro={campos.bairro}>
          <input value={dados.bairro} onChange={(e) => mudar("bairro", e.target.value)} />
        </Campo>
        <Campo rotulo="Cidade" erro={campos.cidade}>
          <input value={dados.cidade} onChange={(e) => mudar("cidade", e.target.value)} />
        </Campo>
      </div>
      <Campo rotulo="Síndico ou responsável" erro={campos.sindicoResponsavel}>
        <input value={dados.sindicoResponsavel} onChange={(e) => mudar("sindicoResponsavel", e.target.value)} />
      </Campo>
      <div className="linha-campos">
        <Campo rotulo="Telefone" erro={campos.telefoneContato}>
          <input
            type="tel"
            inputMode="tel"
            value={dados.telefoneContato}
            onChange={(e) => mudar("telefoneContato", e.target.value)}
          />
        </Campo>
        <Campo rotulo="E-mail" erro={campos.emailContato}>
          <input
            type="email"
            inputMode="email"
            value={dados.emailContato}
            onChange={(e) => mudar("emailContato", e.target.value)}
          />
        </Campo>
      </div>
      <div className="linha-campos">
        <Campo rotulo="Atendimento" erro={campos.tipoAtendimento} dica="Não muda o prazo de manutenção.">
          <select value={dados.tipoAtendimento} onChange={(e) => mudar("tipoAtendimento", e.target.value)}>
            <option value="Avulso">Avulso</option>
            <option value="Contrato">Contrato</option>
          </select>
        </Campo>
        <Campo rotulo="Situação do contrato" erro={campos.statusContrato}>
          <select value={dados.statusContrato} onChange={(e) => mudar("statusContrato", e.target.value)}>
            <option value="Ativo">Ativo</option>
            <option value="Suspenso">Suspenso</option>
            <option value="Inativo">Inativo</option>
          </select>
        </Campo>
      </div>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : cliente ? "Salvar alterações" : "Cadastrar cliente"}
      </button>
    </form>
  );
}

type PropsFormEstacao = { clienteId: string; estacao?: Estacao; aoSalvar: (estacao: Estacao) => void };

export function FormEstacao({ clienteId, estacao, aoSalvar }: PropsFormEstacao) {
  const [localizacao, setLocalizacao] = useState(estacao?.localizacaoInstalacao ?? "");
  const [andarInicial, setAndarInicial] = useState(estacao ? String(estacao.andarInicial) : "");
  const [andarFinal, setAndarFinal] = useState(estacao ? String(estacao.andarFinal) : "");
  const [fechamento, setFechamento] = useState(estacao?.requerFechamentoGeral ?? false);
  const { enviando, erro, campos, executar } = useEnvio();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const dados: DadosEstacao = {
      localizacaoInstalacao: localizacao,
      andarInicial: numeroOuNulo(andarInicial),
      andarFinal: numeroOuNulo(andarFinal),
      requerFechamentoGeral: fechamento,
    };
    const resultado = await executar(() =>
      estacao ? estacoes.atualizar(estacao.id, dados) : estacoes.criar(clienteId, dados),
    );
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <Campo rotulo="Localização *" erro={campos.localizacaoInstalacao} dica="Ex.: Barrilete - Casa de bombas">
        <input required value={localizacao} onChange={(e) => setLocalizacao(e.target.value)} />
      </Campo>
      <div className="linha-campos">
        <Campo rotulo="Andar inicial *" erro={campos.andarInicial} dica="Térreo = 0, subsolo negativo">
          <input
            type="number"
            inputMode="numeric"
            required
            value={andarInicial}
            onChange={(e) => setAndarInicial(e.target.value)}
          />
        </Campo>
        <Campo rotulo="Andar final *" erro={campos.andarFinal ?? campos.faixaDeAndaresValida}>
          <input
            type="number"
            inputMode="numeric"
            required
            value={andarFinal}
            onChange={(e) => setAndarFinal(e.target.value)}
          />
        </Campo>
      </div>
      <label className="caixa">
        <input type="checkbox" checked={fechamento} onChange={(e) => setFechamento(e.target.checked)} />
        Exige fechamento geral do prédio
      </label>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : estacao ? "Salvar alterações" : "Cadastrar estação"}
      </button>
    </form>
  );
}

type PropsFormValvula = { estacaoId: string; valvula?: Valvula; aoSalvar: (valvula: Valvula) => void };

export function FormValvula({ estacaoId, valvula, aoSalvar }: PropsFormValvula) {
  const catalogo = useCarga(() => especificacoes.listar(false), []);
  const [numero, setNumero] = useState(valvula?.numeroValvula ?? "");
  const [especificacaoId, setEspecificacaoId] = useState(valvula?.especificacaoId ?? "");
  const [numeroSerie, setNumeroSerie] = useState(valvula?.numeroSerie ?? "");
  const [dataInstalacao, setDataInstalacao] = useState(valvula?.dataInstalacao ?? "");
  const [periodicidade, setPeriodicidade] = useState(valvula ? String(valvula.periodicidadeMeses) : "12");
  const [novaEspecificacao, setNovaEspecificacao] = useState(false);
  const { enviando, erro, campos, executar } = useEnvio();

  const opcoes: Especificacao[] = catalogo.dados ?? [];

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const dados: DadosValvula = {
      numeroValvula: numero,
      especificacaoId,
      numeroSerie,
      dataInstalacao: dataInstalacao || null,
      periodicidadeMeses: numeroOuNulo(periodicidade),
    };
    const resultado = await executar(() =>
      valvula ? valvulas.atualizar(valvula.id, dados) : valvulas.criar(estacaoId, dados),
    );
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  if (novaEspecificacao) {
    return (
      <div>
        <p className="secundario">Nova especificação no catálogo. Depois de salvar, ela já fica escolhida.</p>
        <FormEspecificacao
          aoSalvar={(e) => {
            catalogo.recarregar();
            setEspecificacaoId(e.id);
            setNovaEspecificacao(false);
          }}
        />
        <button type="button" className="botao-texto" onClick={() => setNovaEspecificacao(false)}>
          Voltar para a válvula
        </button>
      </div>
    );
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro ?? catalogo.erro} />
      <div className="linha-campos">
        <Campo
          rotulo="Número na estação"
          erro={campos.numeroValvula}
          dica={valvula ? undefined : "Vazio = próximo livre. 01 = esquerda ou de cima."}
        >
          <input inputMode="numeric" placeholder="01" value={numero} onChange={(e) => setNumero(e.target.value)} />
        </Campo>
        <Campo rotulo="Periodicidade (meses)" erro={campos.periodicidadeMeses} dica="Padrão 12. Mude só em exceção.">
          <input
            type="number"
            inputMode="numeric"
            min={1}
            max={60}
            value={periodicidade}
            onChange={(e) => setPeriodicidade(e.target.value)}
          />
        </Campo>
      </div>
      <Campo rotulo="Especificação *" erro={campos.especificacaoId}>
        <select required value={especificacaoId} onChange={(e) => setEspecificacaoId(e.target.value)}>
          <option value="">Escolha…</option>
          {opcoes.map((e) => (
            <option key={e.id} value={e.id}>
              {e.descricaoCompleta}
            </option>
          ))}
        </select>
      </Campo>
      <button type="button" className="botao-texto" onClick={() => setNovaEspecificacao(true)}>
        + Especificação que não está na lista
      </button>
      <div className="linha-campos">
        <Campo rotulo="Número de série" erro={campos.numeroSerie}>
          <input value={numeroSerie} onChange={(e) => setNumeroSerie(e.target.value)} />
        </Campo>
        <Campo rotulo="Data de instalação" erro={campos.dataInstalacao}>
          <input type="date" value={dataInstalacao} onChange={(e) => setDataInstalacao(e.target.value)} />
        </Campo>
      </div>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : valvula ? "Salvar alterações" : "Cadastrar válvula"}
      </button>
    </form>
  );
}

type PropsFormEspecificacao = { especificacao?: Especificacao; aoSalvar: (e: Especificacao) => void };

export function FormEspecificacao({ especificacao, aoSalvar }: PropsFormEspecificacao) {
  const [dados, setDados] = useState<DadosEspecificacao>({
    marca: especificacao?.marca ?? "",
    modelo: especificacao?.modelo ?? "",
    diametroPolegadas: especificacao?.diametroPolegadas ?? "",
    tipoRegistro: especificacao?.tipoRegistro ?? "Esfera",
  });
  const { enviando, erro, campos, executar } = useEnvio();
  const mudar = (campo: keyof DadosEspecificacao, valor: string) => setDados((d) => ({ ...d, [campo]: valor }));

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const resultado = await executar(() =>
      especificacao ? especificacoes.atualizar(especificacao.id, dados) : especificacoes.criar(dados),
    );
    if (resultado.ok) aoSalvar(resultado.valor);
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <MensagemErro erro={erro} />
      <div className="linha-campos">
        <Campo rotulo="Marca *" erro={campos.marca}>
          <input required placeholder="Bermad" value={dados.marca} onChange={(e) => mudar("marca", e.target.value)} />
        </Campo>
        <Campo rotulo="Modelo *" erro={campos.modelo}>
          <input required placeholder="720" value={dados.modelo} onChange={(e) => mudar("modelo", e.target.value)} />
        </Campo>
      </div>
      <div className="linha-campos">
        <Campo rotulo="Diâmetro (polegadas) *" erro={campos.diametroPolegadas} dica="Ex.: 2, 1.1/2, 3/4">
          <input
            required
            value={dados.diametroPolegadas}
            onChange={(e) => mudar("diametroPolegadas", e.target.value)}
          />
        </Campo>
        <Campo rotulo="Registro *" erro={campos.tipoRegistro}>
          <select value={dados.tipoRegistro} onChange={(e) => mudar("tipoRegistro", e.target.value)}>
            <option value="Esfera">Esfera</option>
            <option value="Gaveta">Gaveta</option>
          </select>
        </Campo>
      </div>
      <button type="submit" className="botao primario" disabled={enviando}>
        {enviando ? "Salvando…" : especificacao ? "Salvar alterações" : "Cadastrar especificação"}
      </button>
    </form>
  );
}
