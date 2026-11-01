package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * MANUAL: lançado na tela do Valtis ("lançado no Valtis", RF-76).
 * JIRAYA: nasceu de OS concluída no Jiraya (RF-74).
 */
public enum OrigemServico implements ValorDeBanco {

    MANUAL("manual"),
    JIRAYA("jiraya");

    private final String valor;

    OrigemServico(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<OrigemServico> {
        public Conversor() {
            super(OrigemServico.class);
        }
    }
}
