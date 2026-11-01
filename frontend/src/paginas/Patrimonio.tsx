import { useState } from "react";
import { Link } from "react-router";
import type { Bomba } from "../api/tipos";
import { bombas } from "../api/valtis";
import { Cabecalho, Carregando, MensagemErro, Modal, NotaGerido } from "../componentes/Basicos";
import { FormBomba } from "../componentes/FormulariosBombas";
import { useSistema } from "../sessao/useSistema";
import { formatarData } from "../util/formato";
import { useCarga } from "../util/useCarga";
import { textoPosicao } from "../util/bomba";

/** Bombas próprias da Manutec (RF-65): o que está disponível e o que está emprestado a quem. */
export function Patrimonio() {
  const { externo } = useSistema();
  const [nova, setNova] = useState(false);
  const { dados, carregando, erro, recarregar } = useCarga(() => bombas.patrimonio(), []);

  const ativas = (dados ?? []).filter((b) => b.ativo);
  const disponiveis = ativas.filter((b) => b.disponivel).length;

  return (
    <>
      <Cabecalho titulo="Patrimônio da Manutec">
        {externo ? (
          <NotaGerido oQue="Novas bombas" fonte="jiraya" />
        ) : (
          <button type="button" className="botao primario" onClick={() => setNova(true)}>
            + Nova bomba
          </button>
        )}
      </Cabecalho>
      <p className="secundario">
        Bombas da Manutec emprestadas aos condomínios enquanto a deles está em conserto. A bomba só volta a ficar
        disponível depois da devolução registrada.
      </p>

      <MensagemErro erro={erro} />
      {carregando && !dados && <Carregando />}
      {dados && dados.length === 0 && <p className="aviso">Nenhuma bomba no patrimônio.</p>}

      {dados && dados.length > 0 && (
        <>
          <p>
            <strong>{disponiveis}</strong> {disponiveis === 1 ? "disponível" : "disponíveis"} de {ativas.length}{" "}
            {ativas.length === 1 ? "ativa" : "ativas"}.
          </p>
          <table className="tabela tabela-cartoes">
            <thead>
              <tr>
                <th>Código</th>
                <th>Bomba</th>
                <th>Situação</th>
                <th>Última manutenção</th>
              </tr>
            </thead>
            <tbody>
              {dados.map((b: Bomba) => (
                <tr key={b.id} className={b.ativo ? "" : "inativo"}>
                  <td data-rotulo="Código" className="mono">
                    <Link to={`/bombas/${b.id}`}>{b.codigoReferencia}</Link>
                  </td>
                  <td data-rotulo="Bomba">
                    {b.descricao}
                    {b.dadosTecnicosIncompletos && <span className="selo">dados incompletos</span>}
                    {!b.ativo && <span className="selo">inativa</span>}
                  </td>
                  <td data-rotulo="Situação">
                    {b.disponivel ? (
                      <span className="etiqueta em-dia">Disponível</span>
                    ) : b.posicoes.length > 0 ? (
                      <>
                        <span className="etiqueta proximo">Emprestada</span>{" "}
                        <Link to={`/clientes/${b.posicoes[0].clienteId}`}>{textoPosicao(b)}</Link>
                        <span className="secundario"> · desde {formatarData(b.posicoes[0].desde)}</span>
                      </>
                    ) : (
                      <span className="secundario">—</span>
                    )}
                  </td>
                  <td data-rotulo="Última manutenção" className="sem-quebra">
                    {formatarData(b.ultimaManutencaoConsiderada)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      <Modal titulo="Nova bomba do patrimônio" aberto={nova} aoFechar={() => setNova(false)}>
        <FormBomba
          aoSalvar={() => {
            setNova(false);
            recarregar();
          }}
        />
      </Modal>
    </>
  );
}
