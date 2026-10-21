package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MotivoEncerramento implements ValorDeBanco {

    RETIRADA("retirada"),
    DEVOLUCAO("devolucao");

    private final String valor;

    MotivoEncerramento(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<MotivoEncerramento> {
        public Conversor() {
            super(MotivoEncerramento.class);
        }
    }
}
