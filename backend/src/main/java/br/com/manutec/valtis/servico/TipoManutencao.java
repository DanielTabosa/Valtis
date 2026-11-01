package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoManutencao implements ValorDeBanco {

    PREVENTIVA("Preventiva"),
    CORRETIVA("Corretiva"),
    /** RN-22: a instalação também inicia o ciclo. */
    INSTALACAO("Instalacao");

    private final String valor;

    TipoManutencao(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<TipoManutencao> {
        public Conversor() {
            super(TipoManutencao.class);
        }
    }
}
