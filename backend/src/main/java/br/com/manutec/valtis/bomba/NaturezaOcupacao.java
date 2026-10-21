package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/** Por que a bomba está na posição: é do condomínio, ou é da Manutec e foi emprestada. */
public enum NaturezaOcupacao implements ValorDeBanco {

    INSTALACAO("instalacao"),
    EMPRESTIMO("emprestimo");

    private final String valor;

    NaturezaOcupacao(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    /** RN-18: empréstimo termina em devolução; instalação do condomínio termina em retirada. */
    public MotivoEncerramento encerramento() {
        return this == EMPRESTIMO ? MotivoEncerramento.DEVOLUCAO : MotivoEncerramento.RETIRADA;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<NaturezaOcupacao> {
        public Conversor() {
            super(NaturezaOcupacao.class);
        }
    }
}
