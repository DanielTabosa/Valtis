package br.com.manutec.valtis.recorrencia;

/**
 * Situação de uma válvula frente ao vencimento (RN-04, RF-35).
 * Vocabulário da planilha da Manutec. Calculado a cada consulta, nunca gravado (D-15).
 *
 * A ordem das constantes é a ordem de prioridade na tela: o mais urgente primeiro.
 */
public enum StatusPainel {

    VENCIDO("VENCIDO"),
    PROXIMO_DO_VENCIMENTO("PRÓXIMO DO VENCIMENTO"),
    SEM_REGISTRO("SEM REGISTRO"),
    EM_DIA("EM DIA");

    private final String rotulo;

    StatusPainel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
