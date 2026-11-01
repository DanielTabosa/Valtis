package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * O que foi lido. A ordem das constantes é a ordem de aplicação: o equipamento
 * precisa do cliente, e a OS precisa do equipamento.
 */
public enum TipoEventoRecebido implements ValorDeBanco {

    CLIENTE("cliente"),
    EQUIPAMENTO("equipamento"),
    ORDEM_SERVICO("ordem_servico");

    private final String valor;

    TipoEventoRecebido(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<TipoEventoRecebido> {
        public Conversor() {
            super(TipoEventoRecebido.class);
        }
    }
}
