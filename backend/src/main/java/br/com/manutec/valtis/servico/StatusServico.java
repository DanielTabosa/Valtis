package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/** Só serviço CONCLUIDO conta para o vencimento. RASCUNHO fica para RF-20. */
public enum StatusServico implements ValorDeBanco {

    RASCUNHO("rascunho"),
    CONCLUIDO("concluido");

    private final String valor;

    StatusServico(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<StatusServico> {
        public Conversor() {
            super(StatusServico.class);
        }
    }
}
