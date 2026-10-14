package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/** Os dois alertas de cada ciclo (RN-06, D-08). */
public enum TipoEvento implements ValorDeBanco {

    ALERTA_COMERCIAL("alerta_comercial", "Contato comercial"),
    LEMBRETE_FINAL("lembrete_final", "Lembrete final");

    private final String valor;
    private final String titulo;

    TipoEvento(String valor, String titulo) {
        this.valor = valor;
        this.titulo = titulo;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    public String titulo() {
        return titulo;
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<TipoEvento> {
        public Conversor() {
            super(TipoEvento.class);
        }
    }
}
