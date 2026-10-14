package br.com.manutec.valtis.usuario;

import br.com.manutec.valtis.comum.ConversorDeEnum;
import br.com.manutec.valtis.comum.ValorDeBanco;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Perfis de acesso (RF-01 a RF-03).
 * O técnico lança serviços, cadastra equipamentos e trabalha a fila de oportunidades.
 * O administrador faz tudo isso e também inativa cadastros e gerencia usuários.
 */
public enum Perfil implements ValorDeBanco {

    TECNICO("tecnico"),
    ADMIN("admin");

    private final String valor;

    Perfil(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    /** Nome da autoridade no Spring Security: ROLE_ADMIN, ROLE_TECNICO. */
    public String papel() {
        return "ROLE_" + name();
    }

    @jakarta.persistence.Converter
    public static class Conversor extends ConversorDeEnum<Perfil> {
        public Conversor() {
            super(Perfil.class);
        }
    }
}
