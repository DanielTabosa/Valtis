import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import type { AnaliseImportacao, LinhaAnalisada, LinhaImportacao, ResultadoImportacao } from "../api/tipos";
import { importacao } from "../api/valtis";
import { Cabecalho, MensagemErro } from "../componentes/Basicos";
import { useEnvio } from "../util/useEnvio";

/**
 * Importação de clientes da base antiga (RF-51 a RF-58), em três passos:
 * enviar o arquivo, conferir a prévia (corrigindo na própria tela, RF-56) e confirmar.
 * Quem reconhece colunas e valida é o servidor; a tela só mostra e edita.
 */
export function Importacao() {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [analise, setAnalise] = useState<AnaliseImportacao | null>(null);
  const [linhas, setLinhas] = useState<LinhaAnalisada[]>([]);
  const [marcadas, setMarcadas] = useState<Set<number>>(new Set());
  const [editado, setEditado] = useState(false);
  const [resultado, setResultado] = useState<ResultadoImportacao | null>(null);
  const envio = useEnvio();

  function receber(novas: LinhaAnalisada[]) {
    setLinhas(novas);
    setMarcadas(new Set(novas.filter((l) => l.sugerida).map((l) => l.dados.linha)));
    setEditado(false);
  }

  async function analisar(evento: FormEvent) {
    evento.preventDefault();
    if (!arquivo) return;
    setResultado(null);
    const r = await envio.executar(() => importacao.analisar(arquivo));
    if (r.ok) {
      setAnalise(r.valor);
      receber(r.valor.linhas);
    }
  }

  async function revalidar() {
    const r = await envio.executar(() => importacao.revalidar(linhas.map((l) => l.dados)));
    if (r.ok) receber(r.valor);
  }

  async function importar() {
    const escolhidas = linhas.filter((l) => marcadas.has(l.dados.linha)).map((l) => l.dados);
    if (!confirm(`Importar ${escolhidas.length} cliente(s)? Eles entram como "base antiga".`)) return;
    const r = await envio.executar(() => importacao.importar(escolhidas));
    if (r.ok) {
      setResultado(r.valor);
      setAnalise(null);
      setLinhas([]);
      setArquivo(null);
    }
  }

  function editar(numero: number, campo: keyof LinhaImportacao, valor: string) {
    setLinhas((atuais) =>
      atuais.map((l) => (l.dados.linha === numero ? { ...l, dados: { ...l.dados, [campo]: valor } } : l)),
    );
    setEditado(true);
  }

  function alternar(numero: number) {
    setMarcadas((atuais) => {
      const novas = new Set(atuais);
      if (novas.has(numero)) novas.delete(numero);
      else novas.add(numero);
      return novas;
    });
  }

  const comErro = linhas.filter((l) => l.erros.length > 0).length;
  const comAviso = linhas.filter((l) => l.erros.length === 0 && l.avisos.length > 0).length;
  const selecionadas = linhas.filter((l) => marcadas.has(l.dados.linha));
  const selecionadaComErro = selecionadas.some((l) => l.erros.length > 0);
  const podeImportar = selecionadas.length > 0 && !selecionadaComErro && !editado && !envio.enviando;

  return (
    <>
      <Cabecalho titulo="Importar clientes" />

      {resultado && (
        <div className="aviso sucesso" role="status">
          <strong>{resultado.importados} cliente(s) importado(s).</strong> {resultado.naFila} entraram na fila de{" "}
          <Link to="/oportunidades">Oportunidades</Link>
          {resultado.semData > 0 && `; ${resultado.semData} sem data ficaram só no cadastro`}. Veja em{" "}
          <Link to="/clientes">Clientes</Link>.
        </div>
      )}

      <form className="formulario etapa" onSubmit={analisar}>
        <h2>1. Arquivo</h2>
        <p className="secundario">
          Planilha .xlsx, .xls ou .csv, com o cabeçalho na primeira linha. O Valtis reconhece as colunas pelo nome
          (condomínio ou cliente, endereço, bairro, cidade, síndico, telefone, e-mail, atendimento e data da última
          manutenção) e descarta as demais.
        </p>
        <input
          type="file"
          accept=".xlsx,.xls,.csv"
          aria-label="Planilha"
          onChange={(e) => setArquivo(e.target.files?.[0] ?? null)}
        />
        <button type="submit" className="botao primario" disabled={!arquivo || envio.enviando}>
          {envio.enviando && !analise ? "Lendo…" : "Analisar arquivo"}
        </button>
      </form>

      <MensagemErro erro={envio.erro} />

      {analise && (
        <section className="etapa">
          <h2>2. Conferir</h2>
          <dl className="ficha">
            <div>
              <dt>Colunas aproveitadas</dt>
              <dd>
                {analise.colunasUsadas.map((c) => (
                  <span key={c.campo} className="selo">
                    {c.cabecalho} → {c.rotulo}
                  </span>
                ))}
              </dd>
            </div>
            <div>
              <dt>Colunas descartadas</dt>
              <dd>
                {analise.colunasDescartadas.length === 0
                  ? "nenhuma"
                  : analise.colunasDescartadas.map((c) => (
                      <span key={c} className="selo">
                        {c}
                      </span>
                    ))}
              </dd>
            </div>
            <div>
              <dt>Linhas</dt>
              <dd>
                {linhas.length} lidas · {comErro} com erro · {comAviso} com aviso · {selecionadas.length}{" "}
                selecionadas
              </dd>
            </div>
          </dl>

          {comErro > 0 && (
            <div className="aviso alerta">
              Linhas com erro não podem ser importadas. Corrija na tabela e clique em "Conferir correções", ou deixe-as
              desmarcadas.
            </div>
          )}

          <table className="tabela tabela-cartoes">
            <thead>
              <tr>
                <th>Importar</th>
                <th>Linha</th>
                <th>Condomínio</th>
                <th>Bairro</th>
                <th>Última manutenção</th>
                <th>Atendimento</th>
                <th>Observações</th>
              </tr>
            </thead>
            <tbody>
              {linhas.map((l) => (
                <tr key={l.dados.linha} className={l.erros.length > 0 ? "linha-erro" : ""}>
                  <td data-rotulo="Importar">
                    <input
                      type="checkbox"
                      aria-label={`Importar linha ${l.dados.linha}`}
                      checked={marcadas.has(l.dados.linha)}
                      onChange={() => alternar(l.dados.linha)}
                    />
                  </td>
                  <td data-rotulo="Linha" className="mono">
                    {l.dados.linha}
                  </td>
                  <td data-rotulo="Condomínio">
                    <input
                      value={l.dados.nomeCondominio ?? ""}
                      aria-label={`Condomínio da linha ${l.dados.linha}`}
                      onChange={(e) => editar(l.dados.linha, "nomeCondominio", e.target.value)}
                    />
                  </td>
                  <td data-rotulo="Bairro">{l.dados.bairro ?? "—"}</td>
                  <td data-rotulo="Última manutenção">
                    <input
                      value={l.dados.ultimaManutencao ?? ""}
                      placeholder="DD/MM/AAAA"
                      aria-label={`Última manutenção da linha ${l.dados.linha}`}
                      onChange={(e) => editar(l.dados.linha, "ultimaManutencao", e.target.value)}
                    />
                  </td>
                  <td data-rotulo="Atendimento">{l.dados.tipoAtendimento ?? "Avulso"}</td>
                  <td data-rotulo="Observações">
                    {l.erros.map((e) => (
                      <div key={e} className="campo-erro">
                        {e}
                      </div>
                    ))}
                    {l.avisos.map((a) => (
                      <div key={a} className="secundario">
                        {a}
                      </div>
                    ))}
                    {l.erros.length === 0 && l.avisos.length === 0 && <span className="secundario">ok</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <h2>3. Confirmar</h2>
          <div className="acoes">
            {editado && (
              <button type="button" className="botao" disabled={envio.enviando} onClick={revalidar}>
                Conferir correções
              </button>
            )}
            <button type="button" className="botao primario" disabled={!podeImportar} onClick={importar}>
              Importar {selecionadas.length} cliente(s)
            </button>
          </div>
          {editado && <p className="secundario">Você editou linhas: confira as correções antes de importar.</p>}
          {!editado && selecionadaComErro && (
            <p className="secundario">Há linhas selecionadas com erro: corrija ou desmarque.</p>
          )}
        </section>
      )}
    </>
  );
}
