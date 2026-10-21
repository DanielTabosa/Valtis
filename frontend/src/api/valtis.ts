// Uma função por rota da API. As telas só falam com este arquivo.
import { http } from "./http";
import type {
  AnaliseImportacao,
  Bomba,
  BombaDetalhe,
  Conjunto,
  DadosBomba,
  DadosConjunto,
  DadosInstalacao,
  Ocupacao,
  PainelBombas,
  ResultadoMovimentacao,
  TipoBomba,
  ClienteDetalhe,
  ClienteResumo,
  DadosCliente,
  DadosEspecificacao,
  DadosEstacao,
  DadosValvula,
  Desfecho,
  Especificacao,
  Estacao,
  ItemLancamento,
  LinhaAnalisada,
  LinhaImportacao,
  Oportunidade,
  Painel,
  Perfil,
  RespostaLogin,
  ResultadoDesfecho,
  ResultadoImportacao,
  ResultadoLancamento,
  SituacaoAgenda,
  StatusEvento,
  StatusPainel,
  Usuario,
  UsuarioOpcao,
  UsuarioSessao,
  Valvula,
  ValvulaDetalhe,
} from "./tipos";

function consulta(parametros: Record<string, string | boolean | undefined>): string {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== "") busca.set(chave, String(valor));
  }
  const texto = busca.toString();
  return texto ? `?${texto}` : "";
}

export const autenticacao = {
  entrar: (email: string, senha: string) => http.post<RespostaLogin>("/api/auth/login", { email, senha }),
  eu: () => http.get<UsuarioSessao>("/api/auth/eu"),
  trocarSenha: (senhaAtual: string, novaSenha: string) => http.post<void>("/api/auth/senha", { senhaAtual, novaSenha }),
};

export type FiltroPainel = {
  busca?: string;
  status?: StatusPainel;
  bairro?: string;
  tipoAtendimento?: string;
  clienteId?: string;
};

export const painel = {
  montar: (filtro: FiltroPainel) => http.get<Painel>(`/api/painel${consulta(filtro)}`),
};

export const clientes = {
  listar: (busca: string, incluirInativos: boolean) =>
    http.get<ClienteResumo[]>(`/api/clientes${consulta({ busca, incluirInativos })}`),
  detalhar: (id: string) => http.get<ClienteDetalhe>(`/api/clientes/${id}`),
  criar: (dados: DadosCliente) => http.post<ClienteDetalhe>("/api/clientes", dados),
  atualizar: (id: string, dados: DadosCliente) => http.put<ClienteDetalhe>(`/api/clientes/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/clientes/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/clientes/${id}/reativar`),
};

export const estacoes = {
  criar: (clienteId: string, dados: DadosEstacao) => http.post<Estacao>(`/api/clientes/${clienteId}/estacoes`, dados),
  atualizar: (id: string, dados: DadosEstacao) => http.put<Estacao>(`/api/estacoes/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/estacoes/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/estacoes/${id}/reativar`),
};

export const valvulas = {
  criar: (estacaoId: string, dados: DadosValvula) => http.post<Valvula>(`/api/estacoes/${estacaoId}/valvulas`, dados),
  detalhar: (id: string) => http.get<ValvulaDetalhe>(`/api/valvulas/${id}`),
  atualizar: (id: string, dados: DadosValvula) => http.put<Valvula>(`/api/valvulas/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/valvulas/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/valvulas/${id}/reativar`),
  excluir: (id: string) => http.delete<void>(`/api/valvulas/${id}`),
};

export const importacao = {
  analisar: (arquivo: File) =>
    http.enviarArquivo<AnaliseImportacao>("/api/importacao/clientes/analise", "arquivo", arquivo),
  revalidar: (linhas: LinhaImportacao[]) =>
    http.post<LinhaAnalisada[]>("/api/importacao/clientes/revalidacao", { linhas }),
  importar: (linhas: LinhaImportacao[]) => http.post<ResultadoImportacao>("/api/importacao/clientes", { linhas }),
};

export const especificacoes = {
  listar: (incluirInativas: boolean) =>
    http.get<Especificacao[]>(`/api/especificacoes${consulta({ incluirInativas })}`),
  criar: (dados: DadosEspecificacao) => http.post<Especificacao>("/api/especificacoes", dados),
  atualizar: (id: string, dados: DadosEspecificacao) => http.put<Especificacao>(`/api/especificacoes/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/especificacoes/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/especificacoes/${id}/reativar`),
};

export const servicos = {
  lancar: (itens: ItemLancamento[]) => http.post<ResultadoLancamento[]>("/api/servicos/lancamentos", { itens }),
};

export type Situacao = "ABERTAS" | "ENCERRADAS" | "TODAS";
export type FiltroResponsavel = "QUALQUER" | "EU" | "NINGUEM";

export const oportunidades = {
  fila: (situacao: Situacao, responsavel: FiltroResponsavel) =>
    http.get<Oportunidade[]>(`/api/oportunidades${consulta({ situacao, responsavel })}`),
  assumir: (id: string) => http.post<Oportunidade>(`/api/oportunidades/${id}/assumir`),
  liberar: (id: string) => http.post<Oportunidade>(`/api/oportunidades/${id}/liberar`),
  desfecho: (id: string, desfecho: Desfecho) =>
    http.post<ResultadoDesfecho>(`/api/oportunidades/${id}/desfecho`, desfecho),
};

export const usuarios = {
  ativos: () => http.get<UsuarioOpcao[]>("/api/usuarios/ativos"),
  listar: () => http.get<Usuario[]>("/api/usuarios"),
  criar: (nome: string, email: string, perfil: Perfil, senha: string) =>
    http.post<Usuario>("/api/usuarios", { nome, email, perfil, senha }),
  atualizar: (id: string, nome: string, perfil: Perfil) => http.put<Usuario>(`/api/usuarios/${id}`, { nome, perfil }),
  redefinirSenha: (id: string, senha: string) => http.post<void>(`/api/usuarios/${id}/senha`, { senha }),
  inativar: (id: string) => http.post<void>(`/api/usuarios/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/usuarios/${id}/reativar`),
};

export const agenda = {
  situacao: (status?: StatusEvento) =>
    http.get<SituacaoAgenda>(`/api/agenda/situacao${consulta({ status: status?.toUpperCase() })}`),
  reprocessar: (id: string) => http.post<void>(`/api/agenda/eventos/${id}/reprocessar`),
  sincronizarAgora: () => http.post<void>("/api/agenda/sincronizar"),
};

export const tiposBomba = {
  listar: (incluirInativos: boolean) => http.get<TipoBomba[]>(`/api/tipos-bomba${consulta({ incluirInativos })}`),
  criar: (nome: string) => http.post<TipoBomba>("/api/tipos-bomba", { nome }),
  renomear: (id: string, nome: string) => http.put<TipoBomba>(`/api/tipos-bomba/${id}`, { nome }),
  inativar: (id: string) => http.post<void>(`/api/tipos-bomba/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/tipos-bomba/${id}/reativar`),
};

export const conjuntos = {
  doCliente: (clienteId: string) => http.get<Conjunto[]>(`/api/clientes/${clienteId}/conjuntos`),
  criar: (clienteId: string, dados: DadosConjunto) => http.post<Conjunto>(`/api/clientes/${clienteId}/conjuntos`, dados),
  atualizar: (id: string, dados: DadosConjunto) => http.put<Conjunto>(`/api/conjuntos/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/conjuntos/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/conjuntos/${id}/reativar`),
};

export const bombas = {
  painel: (status: StatusPainel | "", busca: string) =>
    http.get<PainelBombas>(`/api/bombas/painel${consulta({ status: status || undefined, busca })}`),
  doCliente: (clienteId: string) => http.get<Bomba[]>(`/api/clientes/${clienteId}/bombas`),
  patrimonio: () => http.get<Bomba[]>("/api/patrimonio/bombas"),
  detalhar: (id: string) => http.get<BombaDetalhe>(`/api/bombas/${id}`),
  criarDoCondominio: (clienteId: string, dados: DadosBomba) =>
    http.post<Bomba>(`/api/clientes/${clienteId}/bombas`, dados),
  criarPatrimonial: (dados: DadosBomba) => http.post<Bomba>("/api/patrimonio/bombas", dados),
  atualizar: (id: string, dados: DadosBomba) => http.put<Bomba>(`/api/bombas/${id}`, dados),
  inativar: (id: string) => http.post<void>(`/api/bombas/${id}/inativar`),
  reativar: (id: string) => http.post<void>(`/api/bombas/${id}/reativar`),
  excluir: (id: string) => http.delete<void>(`/api/bombas/${id}`),
  instalar: (id: string, dados: DadosInstalacao) =>
    http.post<ResultadoMovimentacao>(`/api/bombas/${id}/instalacao`, dados),
  encerrarOcupacao: (ocupacaoId: string, fim: string, observacoes: string) =>
    http.post<Ocupacao>(`/api/ocupacoes/${ocupacaoId}/encerramento`, { fim, observacoes }),
};
