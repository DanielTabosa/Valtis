import { useEffect, useState } from "react";

/** Devolve o valor só depois de ele parar de mudar por `ms`. Evita uma busca por tecla digitada. */
export function useAtrasado<T>(valor: T, ms = 300): T {
  const [atrasado, setAtrasado] = useState(valor);
  useEffect(() => {
    const espera = setTimeout(() => setAtrasado(valor), ms);
    return () => clearTimeout(espera);
  }, [valor, ms]);
  return atrasado;
}
