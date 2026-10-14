package br.com.manutec.valtis.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Toda a configuração própria do Valtis, lida do bloco `valtis:` do application.yml.
 *
 * Um lugar só para os números que seriam "mágicos" espalhados pelo código
 * (AGENTS.md §4): janelas de alerta, validade do token, limite de tentativas.
 */
@ConfigurationProperties(prefix = "valtis")
public record ValtisPropriedades(
        String fusoHorario,
        String urlFrontend,
        Recorrencia recorrencia,
        Seguranca seguranca,
        Agenda agenda
) {

    /** Janelas de alerta (D-08, RF-28). */
    public record Recorrencia(int alertaComercialDias, int lembreteFinalDias) {
    }

    public record Seguranca(
            String jwtSegredo,
            long validadeTokenHoras,
            int maxTentativasLogin,
            long bloqueioLoginMinutos,
            AdminInicial adminInicial
    ) {
    }

    /** Primeiro administrador, criado só quando o banco ainda não tem nenhum. */
    public record AdminInicial(String nome, String email, String senha) {

        public boolean configurado() {
            return email != null && !email.isBlank() && senha != null && !senha.isBlank();
        }
    }

    public record Agenda(
            /** desativada | simulada | google */
            String provedor,
            int maxTentativas,
            long esperaInicialSegundos,
            Google google
    ) {
    }

    public record Google(
            String clientId,
            String clientSecret,
            String refreshToken,
            String calendarioId,
            List<String> convidados
    ) {
    }
}
