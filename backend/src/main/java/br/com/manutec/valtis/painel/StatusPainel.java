package br.com.manutec.valtis.painel;

/**
 * Vocabulário do painel (RN-04). Vem da planilha da Manutec.
 */
public enum StatusPainel {

    SEM_REGISTRO("SEM REGISTRO"),
    VENCIDO("VENCIDO"),
    PROXIMO_DO_VENCIMENTO("PRÓXIMO DO VENCIMENTO"),
    EM_DIA("EM DIA");

    private final String rotulo;

    StatusPainel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
