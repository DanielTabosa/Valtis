// Endereço do backend. Em produção vem da variável VITE_API_URL.
const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export type LinhaPainel = {
  valvulaId: string;
  codigoReferencia: string;
  nomeCondominio: string;
  bairro: string | null;
  localizacaoInstalacao: string;
  andaresAtendidos: string;
  especificacao: string;
  requerFechamentoGeral: boolean;
  ultimaManutencao: string | null;
  proximaManutencao: string | null;
  diasRestantes: number | null;
  status: string;
};

export async function buscarPainel(): Promise<LinhaPainel[]> {
  const resposta = await fetch(`${BASE}/api/painel`);
  if (!resposta.ok) {
    throw new Error(`Falha ao carregar o painel (HTTP ${resposta.status})`);
  }
  return resposta.json();
}
