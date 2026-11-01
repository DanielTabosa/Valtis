package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Desfecho comercial de uma oportunidade (RF-41).
 *
 * agendado → contatado → aceito são estados ABERTOS: o ciclo ainda está em jogo.
 * executado, recusado e perdido ENCERRAM a oportunidade.
 * cancelado também encerra, mas não é desfecho comercial: o serviço que abriu o
 * ciclo foi inativado e não sobrou data-base (RN-26, P-32). Não conta como receita perdida.
 */
public enum StatusOportunidade implements ValorDeBanco {

    AGENDADO("agendado"),
    CONTATADO("contatado"),
    ACEITO("aceito"),
    RECUSADO("recusado"),
    EXECUTADO("executado"),
    PERDIDO("perdido"),
    CANCELADO("cancelado");

    public static final Set<StatusOportunidade> ABERTOS = EnumSet.of(AGENDADO, CONTATADO, ACEITO);

    private final String valor;

    StatusOportunidade(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    public boolean aberto() {
        return ABERTOS.contains(this);
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<StatusOportunidade> {
        public Conversor() {
            super(StatusOportunidade.class);
        }
    }
}
