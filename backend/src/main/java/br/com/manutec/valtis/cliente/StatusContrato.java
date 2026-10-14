package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusContrato implements ValorDeBanco {

    ATIVO("Ativo"),
    SUSPENSO("Suspenso"),
    INATIVO("Inativo");

    private final String valor;

    StatusContrato(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<StatusContrato> {
        public Conversor() {
            super(StatusContrato.class);
        }
    }
}
