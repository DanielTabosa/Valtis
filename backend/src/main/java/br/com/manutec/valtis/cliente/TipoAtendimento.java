package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/** Modalidade comercial. Não altera o cálculo de prazo (RN-03). */
public enum TipoAtendimento implements ValorDeBanco {

    CONTRATO("Contrato"),
    AVULSO("Avulso");

    private final String valor;

    TipoAtendimento(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<TipoAtendimento> {
        public Conversor() {
            super(TipoAtendimento.class);
        }
    }
}
