// Tipos que espelham as respostas da API. Datas chegam como texto ISO ("2026-10-13").

export type Perfil = "tecnico" | "admin";
export type TipoAtendimento = "Contrato" | "Avulso";
export type StatusContrato = "Ativo" | "Suspenso" | "Inativo";
export type TipoRegistro = "Esfera" | "Gaveta";
export type TipoManutencao = "Preventiva" | "Corretiva" | "Instalacao";
export type StatusPainel = "VENCIDO" | "PROXIMO_DO_VENCIMENTO" | "SEM_REGISTRO" | "EM_DIA";
/** "cancelado": o serviço de origem foi inativado sem outra data-base (RN-26, P-32). Não é desfecho manual. */
export type StatusOportunidade =
  | "agendado"
  | "contatado"
  | "aceito"
  | "recusado"
  | "executado"
  | "perdido"
  | "cancelado";
export type TipoEvento = "alerta_comercial" | "lembrete_final";
export type StatusEvento = "pendente" | "criado" | "falhou" | "descartado";

/** Quem é dono dos cadastros (D-30): o próprio Valtis ou os sistemas externos (Manutec Clientes e Jiraya). */
export type ModoCadastros = "propria" | "externa";

export type Sistema = { modoCadastros: ModoCadastros };

export type UsuarioSessao = {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
};

export type RespostaLogin = {
  token: string;
  expiraEm: string;
  usuario: UsuarioSessao;
};

export type LinhaPainel = {
  valvulaId: string;
  estacaoId: string;
  clienteId: string;
  codigoReferencia: string;
  nomeCondominio: string;
  bairro: string | null;
  tipoAtendimento: TipoAtendimento;
  localizacaoInstalacao: string;
  andaresAtendidos: string;
  especificacao: string;
  numeroSerie: string | null;
  requerFechamentoGeral: boolean;
  ultimaManutencao: string | null;
  /** Quem fez o último serviço ativo: usuário do Valtis ou técnico do Jiraya. */
  ultimoTecnico: string | null;
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: StatusPainel;
  statusRotulo: string;
};

export type ContagemStatus = { status: StatusPainel; rotulo: string; total: number };

export type OpcoesDeFiltroPainel = { bairros: string[]; tecnicos: string[] };

export type Painel = { resumo: ContagemStatus[]; linhas: LinhaPainel[]; opcoes: OpcoesDeFiltroPainel };

export type ClienteResumo = {
  id: string;
  codigoReferencia: string;
  nomeCondominio: string;
  bairro: string | null;
  cidade: string | null;
  tipoAtendimento: TipoAtendimento;
  statusContrato: StatusContrato;
  ativo: boolean;
  cadastroIncompleto: boolean;
};

export type Valvula = {
  id: string;
  estacaoId: string;
  codigoReferencia: string;
  numeroValvula: string;
  /** Nula na válvula espelhada ainda sem especificação do Valtis ("especificação a completar"). */
  especificacaoId: string | null;
  especificacao: string;
  numeroSerie: string | null;
  dataInstalacao: string | null;
  periodicidadeMeses: number;
  ativo: boolean;
  /** Veio do Jiraya: fabricante, modelo e identificação são só leitura (RN-24). */
  espelhada: boolean;
  fabricante: string | null;
  modelo: string | null;
  identificacaoExterna: string | null;
};

export type Estacao = {
  id: string;
  clienteId: string;
  localizacaoInstalacao: string;
  andarInicial: number | null;
  andarFinal: number | null;
  andares: string;
  requerFechamentoGeral: boolean;
  ativo: boolean;
  valvulas: Valvula[];
  /** Nasceu da localização do Jiraya (RF-73). */
  derivada: boolean;
  /** Faltam os andares, que só o Valtis conhece. */
  aCompletar: boolean;
};

export type ClienteDetalhe = {
  id: string;
  codigoReferencia: string;
  nomeCondominio: string;
  endereco: string | null;
  bairro: string | null;
  cidade: string | null;
  sindicoResponsavel: string | null;
  telefoneContato: string | null;
  emailContato: string | null;
  tipoAtendimento: TipoAtendimento;
  statusContrato: StatusContrato;
  ativo: boolean;
  origem: "sistema" | "importacao";
  ultimaManutencaoLegado: string | null;
  dataLegadoAproximada: boolean;
  cadastroIncompleto: boolean;
  estacoes: Estacao[];
  /** Veio do Manutec Clientes: nome, endereço e contato são só leitura (RN-24). */
  espelhado: boolean;
  manutecId: number | null;
};

export type DadosCliente = {
  nomeCondominio: string;
  endereco: string;
  bairro: string;
  cidade: string;
  sindicoResponsavel: string;
  telefoneContato: string;
  emailContato: string;
  tipoAtendimento: TipoAtendimento;
  statusContrato: StatusContrato;
};

export type DadosEstacao = {
  localizacaoInstalacao: string;
  andarInicial: number | null;
  andarFinal: number | null;
  requerFechamentoGeral: boolean;
};

export type DadosValvula = {
  numeroValvula: string;
  especificacaoId: string;
  numeroSerie: string;
  dataInstalacao: string | null;
  periodicidadeMeses: number | null;
};

// Complementos (RN-24 por campo, D-32): só os campos que o Valtis guarda, inclusive no registro espelhado.

export type ComplementoCliente = { tipoAtendimento: TipoAtendimento; statusContrato: StatusContrato };

/** Andares: os dois ou nenhum. */
export type ComplementoEstacao = {
  andarInicial: number | null;
  andarFinal: number | null;
  requerFechamentoGeral: boolean;
};

export type ComplementoValvula = {
  especificacaoId: string | null;
  dataInstalacao: string | null;
  periodicidadeMeses: number | null;
};

export type Especificacao = {
  id: string;
  marca: string;
  modelo: string;
  diametroPolegadas: string;
  tipoRegistro: TipoRegistro;
  descricaoCompleta: string;
  ativo: boolean;
};

export type DadosEspecificacao = {
  marca: string;
  modelo: string;
  diametroPolegadas: string;
  tipoRegistro: TipoRegistro;
};

/** Exatamente um entre `valvulaId` e `bombaId` (RF-80). Item de bomba não tem pressões. */
export type ItemLancamento = {
  valvulaId: string | null;
  bombaId: string | null;
  dataRealizada: string;
  tipoManutencao: TipoManutencao;
  tecnicoResponsavelId: string;
  pecasSubstituidas: string;
  pressaoEntrada: number | null;
  pressaoSaida: number | null;
  observacoes: string;
};

export type ResultadoLancamento = {
  servicoId: string;
  valvulaId: string | null;
  codigoValvula: string | null;
  bombaId: string | null;
  codigoBomba: string | null;
  dataRealizada: string;
  proximaManutencao: string | null;
};

export type OrigemServico = "manual" | "jiraya";

export type ServicoDoHistorico = {
  id: string;
  dataRealizada: string;
  tipoManutencao: TipoManutencao;
  tecnicoResponsavel: string;
  pecasSubstituidas: string | null;
  pressaoEntrada: number | null;
  pressaoSaida: number | null;
  observacoes: string | null;
  /** "Jiraya" quando o serviço veio de uma OS de lá. */
  lancadoPor: string;
  lancadoEm: string;
  origem: OrigemServico;
  jirayaOsId: number | null;
  /** Inativo continua no histórico, riscado, com o motivo (RN-26). */
  ativo: boolean;
  motivoInativacao: string | null;
};

export type OportunidadeDoHistorico = {
  id: string;
  status: StatusOportunidade;
  dataPrevista: string;
  responsavel: string | null;
  motivoRecusa: string | null;
  encerradaEm: string | null;
};

export type ValvulaDetalhe = {
  valvula: Valvula;
  clienteId: string;
  codigoCliente: string;
  nomeCondominio: string;
  clienteAtivo: boolean;
  estacaoId: string;
  localizacaoInstalacao: string;
  andares: string;
  requerFechamentoGeral: boolean;
  estacaoAtiva: boolean;
  ultimaManutencao: string | null;
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: StatusPainel;
  statusRotulo: string;
  historico: ServicoDoHistorico[];
  oportunidades: OportunidadeDoHistorico[];
};

export type Oportunidade = {
  id: string;
  escopo: "valvula" | "cliente" | "bomba";
  status: StatusOportunidade;
  aberta: boolean;
  origem: "sistema" | "importacao";
  dataPrevista: string;
  dataAlertaComercial: string;
  dataLembreteFinal: string;
  diasParaVencimento: number | null;
  semDonoUrgente: boolean;
  semLembreteNaAgenda: boolean;
  clienteId: string;
  codigoCliente: string;
  nomeCondominio: string;
  bairro: string | null;
  sindicoResponsavel: string | null;
  telefoneContato: string | null;
  valvulaId: string | null;
  codigoValvula: string | null;
  localizacaoInstalacao: string | null;
  andares: string | null;
  requerFechamentoGeral: boolean | null;
  bombaId: string | null;
  codigoBomba: string | null;
  descricaoBomba: string | null;
  responsavelId: string | null;
  responsavelNome: string | null;
  assumidaEm: string | null;
  notas: string | null;
  motivoRecusa: string | null;
  encerradaEm: string | null;
};

export type Desfecho = {
  status: StatusOportunidade;
  notas: string;
  motivoRecusa: string;
  lembrarNovamente: boolean | null;
  lembrarEm: string | null;
};

export type ResultadoDesfecho = { oportunidade: Oportunidade; retorno: Oportunidade | null };

export type UsuarioOpcao = { id: string; nome: string; perfil: Perfil };

export type Usuario = { id: string; nome: string; email: string; perfil: Perfil; ativo: boolean };

/** Uma linha da planilha, em texto, como a tela mostra e deixa corrigir (RF-56). */
export type LinhaImportacao = {
  linha: number;
  nomeCondominio: string | null;
  endereco: string | null;
  bairro: string | null;
  cidade: string | null;
  sindicoResponsavel: string | null;
  telefoneContato: string | null;
  emailContato: string | null;
  tipoAtendimento: string | null;
  ultimaManutencao: string | null;
};

export type LinhaAnalisada = { dados: LinhaImportacao; erros: string[]; avisos: string[]; sugerida: boolean };

export type AnaliseImportacao = {
  colunasUsadas: { cabecalho: string; campo: string; rotulo: string }[];
  colunasDescartadas: string[];
  linhas: LinhaAnalisada[];
};

export type ResultadoImportacao = { importados: number; naFila: number; semData: number };

export type EventoDaAgenda = {
  id: string;
  tipo: TipoEvento;
  dataEvento: string;
  status: StatusEvento;
  tentativas: number;
  proximaTentativaEm: string;
  ultimoErro: string | null;
  nomeCondominio: string;
  alvo: string;
};

export type SituacaoAgenda = {
  provedor: string;
  sincronizacaoAtiva: boolean;
  totais: Record<StatusEvento, number>;
  estado: {
    ultimaExecucao: string | null;
    ultimoSucesso: string | null;
    erroDeAutorizacao: string | null;
    erroDeAutorizacaoEm: string | null;
  };
  eventos: EventoDaAgenda[];
  listaCortada: boolean;
};

// ── Módulo de bombas (RF-60 a RF-68) ──────────────────────────────────────

export type NaturezaOcupacao = "instalacao" | "emprestimo";
export type MotivoEncerramento = "retirada" | "devolucao";

export type TipoBomba = { id: string; nome: string; ativo: boolean };

/** Onde a bomba está agora, calculado pelo servidor a partir das ocupações abertas (D-27). */
export type PosicaoDaBomba = {
  ocupacaoId: string;
  conjuntoId: string;
  conjunto: string;
  clienteId: string;
  nomeCondominio: string;
  numeroPosicao: string;
  natureza: NaturezaOcupacao;
  desde: string;
};

export type Bomba = {
  id: string;
  codigoReferencia: string;
  patrimonioManutec: boolean;
  clienteId: string | null;
  nomeCondominio: string | null;
  fabricante: string | null;
  modelo: string | null;
  potenciaCv: number | null;
  tensaoVolts: number | null;
  correnteAmperes: number | null;
  funcao: string | null;
  dataInstalacao: string | null;
  periodicidadeMeses: number;
  ultimaManutencao: string | null;
  descricao: string;
  ativo: boolean;
  /** A data-base do ciclo: a mais recente entre o cadastro e o último serviço ativo (RF-80). */
  ultimaManutencaoConsiderada: string | null;
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: StatusPainel;
  statusRotulo: string;
  dadosTecnicosIncompletos: boolean;
  posicoes: PosicaoDaBomba[];
  disponivel: boolean;
  /** Veio do Jiraya: fabricante, modelo e identificação são só leitura (RN-24). */
  espelhada: boolean;
  identificacaoExterna: string | null;
};

export type PosicaoOcupada = {
  ocupacaoId: string;
  numeroPosicao: string;
  bombaId: string;
  codigoBomba: string;
  descricaoBomba: string;
  patrimonioManutec: boolean;
  natureza: NaturezaOcupacao;
  desde: string;
};

export type Conjunto = {
  id: string;
  clienteId: string;
  tipoId: string;
  tipo: string;
  localizacao: string;
  observacoes: string | null;
  ativo: boolean;
  posicoes: PosicaoOcupada[];
  /** Nasceu da localização e do tipo das bombas no Jiraya (RF-73): só muda por lá. */
  derivado: boolean;
};

export type Ocupacao = {
  id: string;
  bombaId: string;
  codigoBomba: string;
  conjuntoId: string;
  conjunto: string;
  clienteId: string;
  nomeCondominio: string;
  numeroPosicao: string;
  natureza: NaturezaOcupacao;
  bombaSubstituidaId: string | null;
  codigoBombaSubstituida: string | null;
  inicio: string;
  fim: string | null;
  motivoEncerramento: MotivoEncerramento | null;
  observacoes: string | null;
  registradoPor: string;
  aberta: boolean;
};

/** `historico` são as posições (D-27); `servicos` e `oportunidades`, como na ficha da válvula (RF-80). */
export type BombaDetalhe = {
  bomba: Bomba;
  historico: Ocupacao[];
  servicos: ServicoDoHistorico[];
  oportunidades: OportunidadeDoHistorico[];
};

export type PainelBombas = { resumo: ContagemStatus[]; linhas: Bomba[] };

export type DadosConjunto = { tipoId: string; localizacao: string; observacoes: string };

export type DadosBomba = {
  fabricante: string;
  modelo: string;
  potenciaCv: number | null;
  tensaoVolts: number | null;
  correnteAmperes: number | null;
  funcao: string;
  dataInstalacao: string | null;
  periodicidadeMeses: number | null;
  ultimaManutencao: string | null;
  conjuntoId: string | null;
};

export type DadosInstalacao = {
  conjuntoId: string;
  /** Nulo = o servidor escolhe (a da substituída, ou a próxima livre). */
  numeroPosicao: string | null;
  inicio: string;
  bombaSubstituidaId: string | null;
  observacoes: string;
};

export type ResultadoMovimentacao = { ocupacao: Ocupacao; aviso: string | null };

// ── Modo integrado: telas de admin (RF-75, RF-78) ─────────────────────────

export type OrigemIntegracao = "manutec_clientes" | "jiraya";
export type TipoEventoRecebido = "cliente" | "equipamento" | "ordem_servico";
export type EstadoEventoRecebido = "pendente" | "aplicado" | "pendencia" | "falhou" | "descartado" | "superado";

export type ServicoDaPendencia = {
  id: string;
  codigoEquipamento: string;
  dataRealizada: string;
  ativo: boolean;
  valvulaId: string | null;
  bombaId: string | null;
};

export type Pendencia = {
  id: string;
  origem: OrigemIntegracao;
  tipo: TipoEventoRecebido;
  idExterno: number;
  versao: string;
  estado: "pendencia" | "falhou";
  motivo: string | null;
  ultimoErro: string | null;
  tentativas: number;
  recebidoEm: string;
  resumo: string;
  servicos: ServicoDaPendencia[];
};

export type CursorIntegracao = {
  origem: string;
  recurso: string;
  desde: string | null;
  ultimaLeituraEm: string | null;
  ultimaLeituraOkEm: string | null;
  ultimoErro: string | null;
};

export type EstadoIntegracao = {
  modoCadastros: ModoCadastros;
  leituraAtiva: boolean;
  cursores: CursorIntegracao[];
  eventos: Partial<Record<EstadoEventoRecebido, number>>;
};

export type ResumoDaLeitura = { recebidos: number; aplicados: number };
