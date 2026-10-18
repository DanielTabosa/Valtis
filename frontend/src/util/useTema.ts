import { useCallback, useEffect, useState } from "react";

export type Tema = "claro" | "escuro";

const CHAVE = "valtis.tema";

/** Sem escolha salva, vale o tema do sistema. O index.html aplica o salvo antes do React, sem piscar. */
function temaInicial(): Tema {
  const marcado = document.documentElement.dataset.theme;
  if (marcado === "light") return "claro";
  if (marcado === "dark") return "escuro";
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "escuro" : "claro";
}

export function useTema() {
  const [tema, setTema] = useState<Tema>(temaInicial);

  useEffect(() => {
    document.documentElement.dataset.theme = tema === "escuro" ? "dark" : "light";
    document.querySelector('meta[name="theme-color"]')?.setAttribute("content", tema === "escuro" ? "#050d1f" : "#0e2a5c");
  }, [tema]);

  const alternar = useCallback(() => {
    setTema((atual) => {
      const novo = atual === "escuro" ? "claro" : "escuro";
      try {
        localStorage.setItem(CHAVE, novo);
      } catch {
        // Navegação privada: o tema vale só nesta aba.
      }
      return novo;
    });
  }, []);

  return { tema, alternar };
}
