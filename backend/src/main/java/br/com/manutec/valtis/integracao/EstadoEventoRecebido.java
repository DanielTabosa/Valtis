package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Ciclo de vida de um item lido (D-30, database.md §8a).
 *
 * PENDENCIA é problema de negócio: precisa de uma pessoa (RF-75).
 * FALHOU é problema técnico que esgotou as tentativas.
 * SUPERADO: chegou versão mais nova do mesmo item antes de esta ser aplicada.
 */
public enum EstadoEventoRecebido implements ValorDeBanco {

    PENDENTE("pendente"),
    APLICADO("aplicado"),
    PENDENCIA("pendencia"),
    FALHOU("falhou"),
    DESCARTADO("descartado"),
    SUPERADO("superado");

    private final String valor;

    EstadoEventoRecebido(String valor) {
        this.valor = valor;
    }

    /** Ainda pode ser aplicado: uma versão mais nova o substitui. */
    public boolean emAberto() {
        return this == PENDENTE || this == PENDENCIA || this == FALHOU;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<EstadoEventoRecebido> {
        public Conversor() {
            super(EstadoEventoRecebido.class);
        }
    }
}
