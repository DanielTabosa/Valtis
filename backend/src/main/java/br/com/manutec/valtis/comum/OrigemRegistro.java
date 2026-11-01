package br.com.manutec.valtis.comum;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Se o registro nasceu no sistema, veio da importação da base legada (D-09) ou é
 * espelho do Manutec Clientes no modo integrado (D-29).
 */
public enum OrigemRegistro implements ValorDeBanco {

    SISTEMA("sistema"),
    IMPORTACAO("importacao"),
    INTEGRACAO("integracao");

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
