import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router";
import { useSessao } from "../sessao/useSessao";
import { useSistema } from "../sessao/useSistema";
import { useTema } from "../util/useTema";
import { Icone, Marca, type NomeIcone } from "./Icone";

type Item = {
  para: string;
  rotulo: string;
  icone: NomeIcone;
  soAdmin?: boolean;
  /** true: só no modo integrado; false: só com cadastro próprio; ausente: nos dois (D-30). */
  soExterno?: boolean;
};
type Grupo = { titulo: string; itens: Item[] };

/** Agrupado por tarefa: o técnico vive em "Operação"; o resto é apoio. */
const GRUPOS: Grupo[] = [
  {
    titulo: "Operação",
    itens: [
      { para: "/", rotulo: "Painel", icone: "painel" },
      { para: "/lancar", rotulo: "Lançar serviço", icone: "lancar" },
      { para: "/oportunidades", rotulo: "Oportunidades", icone: "oportunidades" },
      { para: "/bombas", rotulo: "Bombas", icone: "bomba" },
      { para: "/agenda", rotulo: "Agenda", icone: "agenda" },
    ],
  },
  {
    titulo: "Cadastros",
    itens: [
      { para: "/clientes", rotulo: "Clientes", icone: "clientes" },
      { para: "/especificacoes", rotulo: "Especificações", icone: "especificacoes" },
      // RF-77: em externa o patrimônio emprestado é do Jiraya (P-24); a tela fica só para propria.
      { para: "/patrimonio", rotulo: "Patrimônio", icone: "bomba", soExterno: false },
    ],
  },
  {
    titulo: "Administração",
    itens: [
      { para: "/importar", rotulo: "Importar", icone: "importar", soAdmin: true, soExterno: false },
      // No modo integrado o catálogo vem do Jiraya: a tela de escrita sai do menu.
      { para: "/tipos-bomba", rotulo: "Tipos de bomba", icone: "bomba", soAdmin: true, soExterno: false },
      { para: "/integracao", rotulo: "Integração", icone: "sincronia", soAdmin: true, soExterno: true },
      { para: "/integracao/pendencias", rotulo: "Pendências", icone: "alerta", soAdmin: true, soExterno: true },
      { para: "/usuarios", rotulo: "Usuários", icone: "usuarios", soAdmin: true },
    ],
  },
];

/** Rotas que têm filhas no menu: só ficam marcadas na própria página, não nas de baixo. */
const ROTAS_EXATAS = new Set(["/", "/integracao"]);

/** Barra inferior do celular: no máximo cinco destinos, com "Lançar" no centro. */
const BARRA: Item[] = [
  { para: "/", rotulo: "Painel", icone: "painel" },
  { para: "/oportunidades", rotulo: "Fila", icone: "oportunidades" },
  { para: "/lancar", rotulo: "Lançar", icone: "lancar" },
  { para: "/clientes", rotulo: "Clientes", icone: "clientes" },
];

function iniciais(nome: string | undefined): string {
  if (!nome) return "?";
  const partes = nome.trim().split(/\s+/);
  return ((partes[0]?.[0] ?? "") + (partes.length > 1 ? (partes.at(-1)?.[0] ?? "") : "")).toUpperCase();
}

export function Layout() {
  const { usuario, sair, ehAdmin } = useSessao();
  const { externo } = useSistema();
  const { tema, alternar } = useTema();
  const [maisAberto, setMaisAberto] = useState(false);
  const local = useLocation();

  useEffect(() => {
    if (!maisAberto) return;
    const fecharComEsc = (e: KeyboardEvent) => e.key === "Escape" && setMaisAberto(false);
    window.addEventListener("keydown", fecharComEsc);
    return () => window.removeEventListener("keydown", fecharComEsc);
  }, [maisAberto]);

  const visivel = (i: Item) => (!i.soAdmin || ehAdmin) && (i.soExterno === undefined || i.soExterno === externo);
  const grupos = GRUPOS.map((g) => ({ ...g, itens: g.itens.filter(visivel) })).filter(
    (g) => g.itens.length > 0,
  );
  const rotuloTema = tema === "escuro" ? "Usar tema claro" : "Usar tema escuro";

  return (
    <div className="app">
      <aside className="lateral" aria-label="Navegação principal">
        <NavLink to="/" className="marca">
          <Marca />
          <span className="marca-texto">
            Valtis
            <small>Manutec Válvulas</small>
          </span>
        </NavLink>

        <nav className="lateral-nav">
          {grupos.map((g) => (
            <div key={g.titulo} className="nav-grupo">
              <span className="nav-grupo-titulo">{g.titulo}</span>
              {g.itens.map((item) => (
                <NavLink
                  key={item.para}
                  to={item.para}
                  end={ROTAS_EXATAS.has(item.para)}
                  className={({ isActive }) => `nav-item ${isActive ? "ativo" : ""}`}
                >
                  <Icone nome={item.icone} />
                  {item.rotulo}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="nav-usuario">
          <NavLink to="/conta" className={({ isActive }) => `usuario-cartao ${isActive ? "ativo" : ""}`}>
            <span className="avatar" aria-hidden="true">
              {iniciais(usuario?.nome)}
            </span>
            <span className="usuario-texto">
              {usuario?.nome}
              <small>{ehAdmin ? "Administrador" : "Técnico"}</small>
            </span>
          </NavLink>
          <div className="usuario-acoes">
            <button type="button" className="botao-icone" onClick={alternar} aria-label={rotuloTema} title={rotuloTema}>
              <Icone nome={tema === "escuro" ? "sol" : "lua"} />
            </button>
            <button type="button" className="botao-icone" onClick={sair} aria-label="Sair" title="Sair">
              <Icone nome="sair" />
            </button>
          </div>
        </div>
      </aside>

      <header className="topo-movel">
        <NavLink to="/" className="marca">
          <Marca tamanho={28} />
          <span className="marca-texto">Valtis</span>
        </NavLink>
        <button type="button" className="botao-icone" onClick={alternar} aria-label={rotuloTema}>
          <Icone nome={tema === "escuro" ? "sol" : "lua"} />
        </button>
      </header>

      <main className="conteudo">
        <div className="conteudo-interno" key={local.pathname}>
          <Outlet />
        </div>
      </main>

      <nav className="barra-inferior" aria-label="Atalhos">
        {BARRA.map((item) => (
          <NavLink
            key={item.para}
            to={item.para}
            end={ROTAS_EXATAS.has(item.para)}
            className={({ isActive }) =>
              `barra-item ${item.para === "/lancar" ? "destaque" : ""} ${isActive ? "ativo" : ""}`
            }
            onClick={() => setMaisAberto(false)}
          >
            <span className="barra-icone">
              <Icone nome={item.icone} tamanho={item.para === "/lancar" ? 26 : 22} />
            </span>
            {item.rotulo}
          </NavLink>
        ))}
        <button
          type="button"
          className={`barra-item ${maisAberto ? "ativo" : ""}`}
          aria-expanded={maisAberto}
          aria-controls="folha-mais"
          onClick={() => setMaisAberto((aberto) => !aberto)}
        >
          <span className="barra-icone">
            <Icone nome={maisAberto ? "fechar" : "mais"} tamanho={22} />
          </span>
          Mais
        </button>
      </nav>

      {maisAberto && (
        <>
          <div className="folha-fundo" onClick={() => setMaisAberto(false)} aria-hidden="true" />
          <div id="folha-mais" className="folha" role="dialog" aria-label="Mais opções">
            <div className="folha-alca" aria-hidden="true" />
            {grupos.map((g) => (
              <div key={g.titulo} className="nav-grupo">
                <span className="nav-grupo-titulo">{g.titulo}</span>
                <div className="folha-grade">
                  {g.itens.map((item) => (
                    <NavLink
                      key={item.para}
                      to={item.para}
                      end={ROTAS_EXATAS.has(item.para)}
                      className={({ isActive }) => `folha-item ${isActive ? "ativo" : ""}`}
                      onClick={() => setMaisAberto(false)}
                    >
                      <Icone nome={item.icone} tamanho={22} />
                      {item.rotulo}
                    </NavLink>
                  ))}
                </div>
              </div>
            ))}
            <div className="folha-rodape">
              <NavLink to="/conta" className="usuario-cartao" onClick={() => setMaisAberto(false)}>
                <span className="avatar" aria-hidden="true">
                  {iniciais(usuario?.nome)}
                </span>
                <span className="usuario-texto">
                  {usuario?.nome}
                  <small>Minha conta</small>
                </span>
              </NavLink>
              <button type="button" className="botao" onClick={sair}>
                <Icone nome="sair" />
                Sair
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
