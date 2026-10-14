import { useCallback, useEffect, useState } from "react";
import { mensagemDe } from "../api/http";

type Carga<T> = {
  dados: T | null;
  carregando: boolean;
  erro: string | null;
  recarregar: () => void;
};

/**
 * Busca dados ao montar a tela e sempre que `dependencias` mudarem.
 * Ignora respostas que chegam depois de a busca ter sido trocada por outra
 * (ex.: digitar rápido no campo de busca), para a tela não mostrar resultado velho.
 */
export function useCarga<T>(buscar: () => Promise<T>, dependencias: readonly unknown[]): Carga<T> {
  const [dados, setDados] = useState<T | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [versao, setVersao] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const buscarMemo = useCallback(buscar, dependencias);

  useEffect(() => {
    let valida = true;
    setCarregando(true);
    setErro(null);
    buscarMemo()
      .then((resultado) => {
        if (valida) setDados(resultado);
      })
      .catch((e: unknown) => {
        if (valida) setErro(mensagemDe(e));
      })
      .finally(() => {
        if (valida) setCarregando(false);
      });
    return () => {
      valida = false;
    };
  }, [buscarMemo, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { dados, carregando, erro, recarregar };
}
