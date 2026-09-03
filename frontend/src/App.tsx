import { useEffect, useState } from "react";
import { buscarPainel, type LinhaPainel } from "./api";
import "./App.css";

const ORDEM_STATUS = [
  "VENCIDO",
  "PRÓXIMO DO VENCIMENTO",
  "SEM REGISTRO",
  "EM DIA",
];

/** 2026-07-03 -> 03/07/2026 */
function formatarData(iso: string | null): string {
  if (!iso) return "—";
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

function textoPrazo(dias: number | null): string {
  if (dias === null) return "—";
  if (dias < 0) return `${Math.abs(dias)} dias em atraso`;
  if (dias === 0) return "vence hoje";
  return `em ${dias} dias`;
}

/** VENCIDO -> "vencido", PRÓXIMO DO VENCIMENTO -> "proximo" */
function classeStatus(status: string): string {
  if (status === "VENCIDO") return "vencido";
  if (status === "PRÓXIMO DO VENCIMENTO") return "proximo";
  if (status === "SEM REGISTRO") return "sem-registro";
  return "em-dia";
}

export default function App() {
  const [linhas, setLinhas] = useState<LinhaPainel[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    buscarPainel()
      .then(setLinhas)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, []);

  // Contagem por situação, calculada a partir das linhas já carregadas.
  const resumo = ORDEM_STATUS.map((status) => ({
    status,
    total: linhas.filter((l) => l.status === status).length,
  }));

  return (
    <div className="pagina">
      <header className="cabecalho">
        <div>
          <h1>Valtis</h1>
          <p className="subtitulo">
            Controle de manutenção de válvulas redutoras de pressão
          </p>
        </div>
        <span className="empresa">Manutec Válvulas</span>
      </header>

      {carregando && <p className="aviso">Carregando painel…</p>}

      {erro && (
        <div className="aviso erro">
          <strong>Não foi possível carregar.</strong> {erro}
          <br />
          Verifique se o backend está rodando em localhost:8080.
        </div>
      )}

      {!carregando && !erro && (
        <>
          <section className="resumo">
            {resumo.map((r) => (
              <div key={r.status} className={`cartao ${classeStatus(r.status)}`}>
                <span className="numero">{r.total}</span>
                <span className="rotulo">{r.status}</span>
              </div>
            ))}
          </section>

          <table className="tabela">
            <thead>
              <tr>
                <th>Situação</th>
                <th>Código</th>
                <th>Condomínio</th>
                <th>Estação</th>
                <th>Andares</th>
                <th>Especificação</th>
                <th>Última</th>
                <th>Próxima</th>
                <th>Prazo</th>
              </tr>
            </thead>
            <tbody>
              {linhas.map((l) => (
                <tr key={l.valvulaId}>
                  <td>
                    <span className={`etiqueta ${classeStatus(l.status)}`}>
                      {l.status}
                    </span>
                  </td>
                  <td className="mono">{l.codigoReferencia}</td>
                  <td>
                    {l.nomeCondominio}
                    {l.bairro && <span className="secundario"> · {l.bairro}</span>}
                  </td>
                  <td>
                    {l.localizacaoInstalacao}
                    {l.requerFechamentoGeral && (
                      <span
                        className="fechamento"
                        title="Exige fechamento geral do prédio"
                      >
                        fechamento geral
                      </span>
                    )}
                  </td>
                  <td>{l.andaresAtendidos}</td>
                  <td className="secundario">{l.especificacao}</td>
                  <td>{formatarData(l.ultimaManutencao)}</td>
                  <td>{formatarData(l.proximaManutencao)}</td>
                  <td>{textoPrazo(l.diasRestantes)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {linhas.length === 0 && (
            <p className="aviso">Nenhuma válvula cadastrada ainda.</p>
          )}
        </>
      )}
    </div>
  );
}
