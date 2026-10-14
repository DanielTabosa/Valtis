package br.com.manutec.valtis.comum;

import com.fasterxml.jackson.annotation.JsonValue;

/** Se o registro nasceu no sistema ou veio da importação da base legada (D-09). */
public enum OrigemRegistro implements ValorDeBanco {

    SISTEMA("sistema"),
    IMPORTACAO("importacao");

    private final String valor;

    OrigemRegistro(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<OrigemRegistro> {
        public Conversor() {
            super(OrigemRegistro.class);
        }
    }
}
