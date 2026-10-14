package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Ciclo de vida de um evento na fila de saída (Outbox, D-13).
 *
 * PENDENTE   → ainda não chegou ao Google; o worker vai tentar.
 * CRIADO     → existe na agenda; o id do Google está gravado.
 * FALHOU     → esgotou as tentativas. Precisa de alguém olhar (RF-33).
 * DESCARTADO → não faz mais sentido criar: a data passou ou o ciclo fechou antes.
 */
public enum StatusEvento implements ValorDeBanco {

    PENDENTE("pendente"),
    CRIADO("criado"),
    FALHOU("falhou"),
    DESCARTADO("descartado");

    private final String valor;

    StatusEvento(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<StatusEvento> {
        public Conversor() {
            super(StatusEvento.class);
        }
    }
}
