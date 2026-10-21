// Tipos que espelham as respostas da API. Datas chegam como texto ISO ("2026-10-13").

export type Perfil = "tecnico" | "admin";
export type TipoAtendimento = "Contrato" | "Avulso";
export type StatusContrato = "Ativo" | "Suspenso" | "Inativo";
export type TipoRegistro = "Esfera" | "Gaveta";
export type TipoManutencao = "Preventiva" | "Corretiva";
export type StatusPainel = "VENCIDO" | "PROXIMO_DO_VENCIMENTO" | "SEM_REGISTRO" | "EM_DIA";
export type StatusOportunidade = "agendado" | "contatado" | "aceito" | "recusado" | "executado" | "perdido";
export type TipoEvento = "alerta_comercial" | "lembrete_final";
export type StatusEvento = "pendente" | "criado" | "falhou" | "descartado";

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
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: StatusPainel;
  statusRotulo: string;
};

export type ContagemStatus = { status: StatusPainel; rotulo: string; total: number };

export type Painel = { resumo: ContagemStatus[]; linhas: LinhaPainel[] };

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
  especificacaoId: string;
  especificacao: string;
  numeroSerie: string | null;
  dataInstalacao: string | null;
  periodicidadeMeses: number;
  ativo: boolean;
};

export type Estacao = {
  id: string;
  clienteId: string;
  localizacaoInstalacao: string;
  andarInicial: number;
  andarFinal: number;
  andares: string;
  requerFechamentoGeral: boolean;
  ativo: boolean;
  valvulas: Valvula[];
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

export type ItemLancamento = {
  valvulaId: string;
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
  valvulaId: string;
  codigoValvula: string;
  dataRealizada: string;
  proximaManutencao: string | null;
};

export type ServicoDoHistorico = {
  id: string;
  dataRealizada: string;
  tipoManutencao: TipoManutencao;
  tecnicoResponsavel: string;
  pecasSubstituidas: string | null;
  pressaoEntrada: number | null;
  pressaoSaida: number | null;
  observacoes: string | null;
  lancadoPor: string;
  lancadoEm: string;
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
  escopo: "valvula" | "cliente";
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
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: StatusPainel;
  statusRotulo: string;
  dadosTecnicosIncompletos: boolean;
  posicoes: PosicaoDaBomba[];
  disponivel: boolean;
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

export type BombaDetalhe = { bomba: Bomba; historico: Ocupacao[] };

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
