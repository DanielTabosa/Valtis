package br.com.manutec.valtis.especificacao;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoRegistro implements ValorDeBanco {

    ESFERA("Esfera"),
    GAVETA("Gaveta");

    private final String valor;

    TipoRegistro(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<TipoRegistro> {
        public Conversor() {
            super(TipoRegistro.class);
        }
    }
}
