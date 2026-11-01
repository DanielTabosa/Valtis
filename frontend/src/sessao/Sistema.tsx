import { useEffect, useMemo, useState, type ReactNode } from "react";
import type { ModoCadastros } from "../api/tipos";
import { sistema } from "../api/valtis";
import { ContextoSistema, type EstadoSistema } from "./useSistema";
import { useSessao } from "./useSessao";

/**
 * Lê o modo de operação uma vez por sessão (D-30). O modo só muda com novo deploy,
 * então não há por que perguntar de novo a cada tela.
 */
export function ProvedorSistema({ children }: { children: ReactNode }) {
  const { usuario } = useSessao();
  const [lido, setLido] = useState<ModoCadastros | null>(null);
  const logado = usuario !== null;
  // Ao sair, o valor lido fica guardado: o modo é do deploy, não do usuário, e vale para o próximo login.
  const modo = logado ? lido : null;

  useEffect(() => {
    if (!logado) return;
    let valida = true;
    sistema
      .consultar()
      .then((r) => {
        if (valida) setLido(r.modoCadastros);
      })
      .catch(() => {
        // Sem a resposta, segue como cadastro próprio: o servidor recusa (409) o que
        // não couber no modo real e a mensagem dele diz onde o cadastro é gerido.
        if (valida) setLido("propria");
      });
    return () => {
      valida = false;
    };
  }, [logado]);

  const valor = useMemo<EstadoSistema>(() => ({ modo, externo: modo === "externa" }), [modo]);

  return <ContextoSistema.Provider value={valor}>{children}</ContextoSistema.Provider>;
}
