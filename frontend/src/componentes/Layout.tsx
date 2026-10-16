import { useState } from "react";
import { NavLink, Outlet } from "react-router";
import { useSessao } from "../sessao/useSessao";

type Item = { para: string; rotulo: string; soAdmin?: boolean };

const ITENS: Item[] = [
  { para: "/", rotulo: "Painel" },
  { para: "/lancar", rotulo: "Lançar serviço" },
  { para: "/oportunidades", rotulo: "Oportunidades" },
  { para: "/clientes", rotulo: "Clientes" },
  { para: "/especificacoes", rotulo: "Especificações" },
  { para: "/agenda", rotulo: "Agenda" },
  { para: "/importar", rotulo: "Importar", soAdmin: true },
  { para: "/usuarios", rotulo: "Usuários", soAdmin: true },
];

export function Layout() {
  const { usuario, sair, ehAdmin } = useSessao();
  const [menuAberto, setMenuAberto] = useState(false);

  return (
    <div className="app">
      <header className="topo">
        <div className="topo-marca">
          <NavLink to="/" className="marca" onClick={() => setMenuAberto(false)}>
            Valtis
          </NavLink>
          <button
            type="button"
            className="botao-menu"
            aria-expanded={menuAberto}
            aria-controls="navegacao"
            onClick={() => setMenuAberto((aberto) => !aberto)}
          >
            {menuAberto ? "Fechar" : "Menu"}
          </button>
        </div>
        <nav id="navegacao" className={`navegacao ${menuAberto ? "aberta" : ""}`}>
          {ITENS.filter((item) => !item.soAdmin || ehAdmin).map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.para === "/"}
              className={({ isActive }) => `nav-item ${isActive ? "ativo" : ""}`}
              onClick={() => setMenuAberto(false)}
            >
              {item.rotulo}
            </NavLink>
          ))}
          <div className="nav-usuario">
            <NavLink to="/conta" className="nav-item" onClick={() => setMenuAberto(false)}>
              {usuario?.nome}
            </NavLink>
            <button type="button" className="botao-texto" onClick={sair}>
              Sair
            </button>
          </div>
        </nav>
      </header>
      <main className="conteudo">
        <Outlet />
      </main>
    </div>
  );
}
