package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VALVULA: nasceu de um serviço lançado na válvula (RN-07).
 * BOMBA: nasceu de um serviço lançado na bomba (D-31).
 * CLIENTE: veio da base legada, sem válvula conhecida (D-09).
 */
public enum EscopoOportunidade implements ValorDeBanco {

    VALVULA("valvula"),
    BOMBA("bomba"),
    CLIENTE("cliente");

    private final String valor;

    EscopoOportunidade(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<EscopoOportunidade> {
        public Conversor() {
            super(EscopoOportunidade.class);
        }
    }
}
