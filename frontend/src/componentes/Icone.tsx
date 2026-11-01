import { useId } from "react";

/**
 * Ícones de traço único, desenhados em grade 24×24 (sem biblioteca: D-24).
 * Decorativos por padrão (aria-hidden): o texto ao lado é que dá o sentido.
 */
const TRACOS = {
  painel: "M4 4h7v9H4zM13 4h7v5h-7zM13 11h7v9h-7zM4 15h7v5H4z",
  lancar: "M12 5v14M5 12h14",
  oportunidades: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM12 12h.01",
  clientes: "M4 21V5l8-2v18M12 21h8V9l-8-2M8 8v.01M8 12v.01M8 16v.01M16 12v.01M16 16v.01M2 21h20",
  especificacoes: "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0M16 4v4M10 10v4M16 16v4",
  agenda: "M5 5h14a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM4 10h16M8 3v4M16 3v4M8 14h2M14 14h2M8 17h2",
  importar: "M12 15V3M7 8l5-5 5 5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4",
  usuarios: "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM2 21v-1a6 6 0 0 1 12 0v1M16 3.5a4 4 0 0 1 0 7M22 21v-1a6 6 0 0 0-4-5.6",
  conta: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0",
  sair: "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l5-5-5-5M15 12H3",
  sol: "M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
  lua: "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z",
  mais: "M5 12h.01M12 12h.01M19 12h.01",
  fechar: "M6 6l12 12M18 6L6 18",
  alerta: "M12 3l9.5 17H2.5zM12 10v4M12 17v.01",
  relogio: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2",
  confere: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM8 12l3 3 5-6",
  vazio: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM8 12h8",
  busca: "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4",
  seta: "M9 6l6 6-6 6",
  sincronia: "M20 12a8 8 0 0 1-14.3 4.9M4 12a8 8 0 0 1 14.3-4.9M18.5 3v4.2h-4.2M5.5 21v-4.2h4.2",
  bomba: "M12 15a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM12 11v-1M12 3v4M12 15v6M3 11h5M16 11h5M7 20h10",
} as const;

export type NomeIcone = keyof typeof TRACOS;

export function Icone({ nome, tamanho = 20, rotulo }: { nome: NomeIcone; tamanho?: number; rotulo?: string }) {
  return (
    <svg
      className="icone"
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={rotulo ? "img" : undefined}
      aria-label={rotulo}
      aria-hidden={rotulo ? undefined : true}
    >
      <path d={TRACOS[nome]} />
    </svg>
  );
}

/**
 * Marca do Valtis: o símbolo técnico de válvula (dois triângulos que se tocam)
 * com a haste do regulador por cima, dentro de um quadrado com o degradê Manutec.
 */
export function Marca({ tamanho = 32 }: { tamanho?: number }) {
  // Id único por instância: um degradê dentro de um SVG oculto (display: none) não pinta nos outros.
  const degrade = `valtis-degrade-${useId().replace(/[^a-zA-Z0-9]/g, "")}`;
  return (
    <svg className="marca-simbolo" width={tamanho} height={tamanho} viewBox="0 0 32 32" aria-hidden="true">
      <defs>
        <linearGradient id={degrade} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#3f7fd8" />
          <stop offset="1" stopColor="#0e2a5c" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill={`url(#${degrade})`} />
      <path d="M7 12.5v9l9-4.5zM25 12.5v9l-9-4.5z" fill="#ffffff" />
      <path d="M16 17V9.5" stroke="#7dd3fc" strokeWidth="2" strokeLinecap="round" />
      <circle cx="16" cy="8" r="2.4" fill="#7dd3fc" />
    </svg>
  );
}
