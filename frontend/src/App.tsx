import { Link, Navigate, Outlet, Route, Routes } from "react-router";
import { Carregando } from "./componentes/Basicos";
import { Layout } from "./componentes/Layout";
import { Agenda } from "./paginas/Agenda";
import { BombaDetalhe } from "./paginas/BombaDetalhe";
import { ClienteDetalhe } from "./paginas/ClienteDetalhe";
import { Clientes } from "./paginas/Clientes";
import { Conta } from "./paginas/Conta";
import { Especificacoes } from "./paginas/Especificacoes";
import { Importacao } from "./paginas/Importacao";
import { Lancamento } from "./paginas/Lancamento";
import { Login } from "./paginas/Login";
import { Oportunidades } from "./paginas/Oportunidades";
import { Painel } from "./paginas/Painel";
import { PainelBombas } from "./paginas/PainelBombas";
import { Patrimonio } from "./paginas/Patrimonio";
import { TiposBomba } from "./paginas/TiposBomba";
import { Usuarios } from "./paginas/Usuarios";
import { ValvulaDetalhe } from "./paginas/ValvulaDetalhe";
import { useSessao } from "./sessao/useSessao";

/** Sem sessão, nenhuma tela além do login (RNF-23). */
function SoLogado() {
  const { usuario, verificando, erroConexao, tentarDeNovo, sair } = useSessao();
  if (verificando) return <Carregando texto="Abrindo…" />;
  if (!usuario && erroConexao) {
    return (
      <div className="tela-login">
        <div className="cartao-login" role="alert">
          <h1>Valtis</h1>
          <p>{erroConexao}</p>
          <button type="button" className="botao primario largo" onClick={tentarDeNovo}>
            Tentar de novo
          </button>
          <button type="button" className="botao-texto" onClick={sair}>
            Entrar com outra conta
          </button>
        </div>
      </div>
    );
  }
  if (!usuario) return <Navigate to="/entrar" replace />;
  return <Outlet />;
}

/** Esconder o menu não basta: a rota também confere. E o servidor confere de novo (403). */
function SoAdmin() {
  const { ehAdmin } = useSessao();
  return ehAdmin ? <Outlet /> : <Navigate to="/" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<Login />} />
      <Route element={<SoLogado />}>
        <Route element={<Layout />}>
          <Route index element={<Painel />} />
          <Route path="lancar" element={<Lancamento />} />
          <Route path="oportunidades" element={<Oportunidades />} />
          <Route path="clientes" element={<Clientes />} />
          <Route path="clientes/:id" element={<ClienteDetalhe />} />
          <Route path="valvulas/:id" element={<ValvulaDetalhe />} />
          <Route path="bombas" element={<PainelBombas />} />
          <Route path="bombas/:id" element={<BombaDetalhe />} />
          <Route path="patrimonio" element={<Patrimonio />} />
          <Route path="especificacoes" element={<Especificacoes />} />
          <Route path="agenda" element={<Agenda />} />
          <Route path="conta" element={<Conta />} />
          <Route element={<SoAdmin />}>
            <Route path="usuarios" element={<Usuarios />} />
            <Route path="importar" element={<Importacao />} />
            <Route path="tipos-bomba" element={<TiposBomba />} />
          </Route>
          <Route
            path="*"
            element={
              <p className="aviso">
                Página não encontrada. <Link to="/">Voltar ao painel</Link>
              </p>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
