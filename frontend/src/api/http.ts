// Cliente HTTP do Valtis. Toda chamada passa por aqui: anexa o token e
// traduz a resposta de erro do backend (formato "problem details") num erro tipado.

const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const CHAVE_TOKEN = "valtis.token";

export class ErroApi extends Error {
  readonly status: number;
  /** Mensagem por campo, quando a validação do servidor recusou o formulário. */
  readonly campos: Record<string, string>;

  constructor(status: number, mensagem: string, campos: Record<string, string> = {}) {
    super(mensagem);
    this.status = status;
    this.campos = campos;
  }
}

let aoPerderSessao: () => void = () => {};

/** A sessão registra aqui o que fazer quando o servidor recusar o token. */
export function registrarAoPerderSessao(acao: () => void): void {
  aoPerderSessao = acao;
}

export function lerToken(): string | null {
  try {
    return localStorage.getItem(CHAVE_TOKEN);
  } catch {
    // Modo privado ou armazenamento bloqueado: segue sem sessão salva.
    return null;
  }
}

export function guardarToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(CHAVE_TOKEN, token);
    } else {
      localStorage.removeItem(CHAVE_TOKEN);
    }
  } catch {
    // Sem armazenamento, a sessão dura até fechar a aba.
  }
}

type Metodo = "GET" | "POST" | "PUT";

async function requisitar<T>(metodo: Metodo, caminho: string, corpo?: unknown): Promise<T> {
  const cabecalhos: Record<string, string> = {};
  const token = lerToken();
  if (token) cabecalhos.Authorization = `Bearer ${token}`;
  if (corpo !== undefined) cabecalhos["Content-Type"] = "application/json";

  let resposta: Response;
  try {
    resposta = await fetch(`${BASE}${caminho}`, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi(0, "Sem conexão com o servidor. Confira a internet e tente de novo.");
  }

  if (resposta.status === 401 && caminho !== "/api/auth/login") {
    aoPerderSessao();
    throw new ErroApi(401, "Sua sessão expirou. Entre de novo.");
  }
  if (!resposta.ok) {
    throw await erroDa(resposta);
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

async function erroDa(resposta: Response): Promise<ErroApi> {
  try {
    const corpo: unknown = await resposta.json();
    if (typeof corpo === "object" && corpo !== null) {
      const problema = corpo as { detail?: unknown; campos?: unknown };
      const mensagem = typeof problema.detail === "string" ? problema.detail : mensagemPadrao(resposta.status);
      const campos =
        typeof problema.campos === "object" && problema.campos !== null
          ? (problema.campos as Record<string, string>)
          : {};
      return new ErroApi(resposta.status, mensagem, campos);
    }
  } catch {
    // Corpo vazio ou não-JSON: usa a mensagem padrão do status.
  }
  return new ErroApi(resposta.status, mensagemPadrao(resposta.status));
}

function mensagemPadrao(status: number): string {
  if (status === 403) return "Seu perfil não tem permissão para isso.";
  if (status === 404) return "Registro não encontrado.";
  if (status >= 500) return "O servidor teve um problema. Tente de novo em instantes.";
  return "Não foi possível concluir a operação.";
}

export const http = {
  get: <T>(caminho: string) => requisitar<T>("GET", caminho),
  post: <T>(caminho: string, corpo?: unknown) => requisitar<T>("POST", caminho, corpo ?? {}),
  put: <T>(caminho: string, corpo: unknown) => requisitar<T>("PUT", caminho, corpo),
};

/** Mensagem legível de qualquer erro capturado. */
export function mensagemDe(erro: unknown): string {
  if (erro instanceof Error) return erro.message;
  return "Erro inesperado.";
}
