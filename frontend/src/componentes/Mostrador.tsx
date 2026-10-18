/**
 * Mostrador decorativo da tela de entrada: um ciclo de 12 meses como um manômetro.
 * Do verde (serviço recente) ao vermelho (vencido), com o ponteiro varrendo devagar.
 * Só ilustra a ideia do produto; não mostra dado nenhum.
 */
const CENTRO = 300;
const RAIO = 250;
const INICIO = -225; // graus: 0 meses no canto inferior esquerdo
const VARREDURA = 270; // até 12 meses no canto inferior direito

function ponto(raio: number, graus: number) {
  const rad = (graus * Math.PI) / 180;
  return { x: CENTRO + raio * Math.cos(rad), y: CENTRO + raio * Math.sin(rad) };
}

const MARCAS = Array.from({ length: 49 }, (_, i) => {
  const graus = INICIO + (i * VARREDURA) / 48;
  const mes = i % 4 === 0 ? i / 4 : null;
  return { i, graus, mes };
});

const ARCO = (() => {
  const a = ponto(RAIO, INICIO);
  const b = ponto(RAIO, INICIO + VARREDURA);
  return `M ${a.x} ${a.y} A ${RAIO} ${RAIO} 0 1 1 ${b.x} ${b.y}`;
})();

export function Mostrador() {
  return (
    <svg className="mostrador" viewBox="0 0 600 600" aria-hidden="true">
      <defs>
        <linearGradient id="mostrador-arco" x1="0" y1="1" x2="1" y2="0">
          <stop offset="0" stopColor="#22c55e" />
          <stop offset="0.6" stopColor="#f59e0b" />
          <stop offset="1" stopColor="#ef4444" />
        </linearGradient>
        <radialGradient id="mostrador-miolo">
          <stop offset="0" stopColor="#12306a" />
          <stop offset="1" stopColor="#071634" />
        </radialGradient>
        <filter id="mostrador-brilho">
          <feGaussianBlur stdDeviation="6" />
        </filter>
      </defs>

      <g className="mostrador-giro">
        <circle cx={CENTRO} cy={CENTRO} r="282" fill="none" stroke="#5e9ce9" strokeOpacity=".25" strokeDasharray="2 10" />
      </g>
      <circle cx={CENTRO} cy={CENTRO} r={RAIO} fill="none" stroke="#7da5e1" strokeOpacity=".12" />

      {MARCAS.map(({ i, graus, mes }) => {
        const maior = mes !== null;
        const a = ponto(maior ? 214 : 224, graus);
        const b = ponto(238, graus);
        const t = ponto(196, graus);
        return (
          <g key={i}>
            <line
              x1={a.x}
              y1={a.y}
              x2={b.x}
              y2={b.y}
              stroke="#9fc2f0"
              strokeOpacity={maior ? 0.7 : 0.25}
              strokeWidth={maior ? 2 : 1}
            />
            {maior && (
              <text x={t.x} y={t.y + 4} textAnchor="middle" fill="#8496b0" fontSize="12">
                {mes}
              </text>
            )}
          </g>
        );
      })}

      <path d={ARCO} fill="none" stroke="url(#mostrador-arco)" strokeWidth="6" strokeLinecap="round" opacity=".9" />
      <path d={ARCO} fill="none" stroke="url(#mostrador-arco)" strokeWidth="10" opacity=".35" filter="url(#mostrador-brilho)" />
      <circle cx={CENTRO} cy={CENTRO} r="170" fill="url(#mostrador-miolo)" stroke="#7da5e1" strokeOpacity=".18" />

      <g className="mostrador-ponteiro">
        <path d={`M${CENTRO} ${CENTRO} L${CENTRO} 78`} stroke="#7dd3fc" strokeWidth="3" strokeLinecap="round" />
        <path d={`M${CENTRO} ${CENTRO} L${CENTRO} 78`} stroke="#38bdf8" strokeWidth="8" opacity=".4" filter="url(#mostrador-brilho)" />
      </g>
      <circle cx={CENTRO} cy={CENTRO} r="16" fill="#0b1f4a" stroke="#7dd3fc" strokeWidth="2" />
      <text x={CENTRO} y="372" textAnchor="middle" fill="#a3b3cb" fontSize="13" letterSpacing="3">
        CICLO · 12 MESES
      </text>
    </svg>
  );
}
