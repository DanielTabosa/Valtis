import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import App from "./App.tsx";
import "./index.css";
import { ProvedorSessao } from "./sessao/Sessao.tsx";
import { ProvedorSistema } from "./sessao/Sistema.tsx";

const raiz = document.getElementById("root");
if (!raiz) throw new Error("Elemento #root não encontrado no index.html.");

createRoot(raiz).render(
  <StrictMode>
    <BrowserRouter>
      <ProvedorSessao>
        <ProvedorSistema>
          <App />
        </ProvedorSistema>
      </ProvedorSessao>
    </BrowserRouter>
  </StrictMode>,
);
