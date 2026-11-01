package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/** As duas fontes do modo integrado (D-29). */
public enum OrigemIntegracao implements ValorDeBanco {

    MANUTEC_CLIENTES("manutec_clientes"),
    JIRAYA("jiraya");

    private final String valor;

    OrigemIntegracao(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<OrigemIntegracao> {
        public Conversor() {
            super(OrigemIntegracao.class);
        }
    }
}
