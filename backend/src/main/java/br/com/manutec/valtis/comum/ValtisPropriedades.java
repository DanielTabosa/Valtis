package br.com.manutec.valtis.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
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
        Agenda agenda,
        Cadastros cadastros,
        Integracao integracao
) {

    /** RF-70: quem escreve os cadastros. `propria` (padrão) ou `externa` (D-29). */
    public record Cadastros(String origem) {
    }

    /**
     * Modo integrado (D-30). Só é lido em `externa`. Cada fonte com URL e token
     * de serviço vindos de variável de ambiente (AGENTS.md §8).
     */
    public record Integracao(
            int maxTentativas,
            long esperaInicialSegundos,
            Fonte manutecClientes,
            Fonte jiraya,
            /** RF-72: nome do tipo do Jiraya contém um destes → válvula. Sem diferenciar caixa. */
            List<String> tiposValvula,
            /** RF-72: nome do tipo do Jiraya contém um destes → bomba. */
            List<String> tiposBomba
    ) {
    }

    public record Fonte(String url, String token, Duration timeout) {

        public boolean configurada() {
            return url != null && !url.isBlank() && token != null && !token.isBlank();
        }
    }

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
