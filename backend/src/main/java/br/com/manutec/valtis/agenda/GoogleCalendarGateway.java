package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Adaptador do Google Calendar (D-01, D-23), falando direto com a API REST.
 *
 * Autentica como a conta Gmail institucional dona da agenda, trocando o
 * refresh token por um token de acesso de curta duração.
 *
 * ⚠️ Ainda não foi exercitado contra o Google de verdade: exige as credenciais
 * do app OAuth publicado em produção, que só o Daniel pode criar.
 */
@Component
@ConditionalOnProperty(name = "valtis.agenda.provedor", havingValue = "google")
public class GoogleCalendarGateway implements AgendaGateway {

    static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    static final String URL_CALENDAR = "https://www.googleapis.com/calendar/v3";

    /** Folga para renovar o token antes de ele expirar no meio de uma chamada. */
    private static final long MARGEM_RENOVACAO_SEGUNDOS = 60;

    private record RespostaToken(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn) {}

    private record RespostaEvento(String id) {}

    private final RestClient http;
    private final ValtisPropriedades.Google config;
    private final Clock relogio;

    private String tokenDeAcesso;
    private Instant tokenExpiraEm = Instant.EPOCH;

    @Autowired
    public GoogleCalendarGateway(RestClient.Builder builder, ValtisPropriedades propriedades, Clock relogio) {
        this(builder.build(), propriedades.agenda().google(), relogio);
    }

    GoogleCalendarGateway(RestClient http, ValtisPropriedades.Google config, Clock relogio) {
        if (config == null || vazio(config.clientId()) || vazio(config.clientSecret())
                || vazio(config.refreshToken()) || vazio(config.calendarioId())) {
            throw new IllegalStateException(
                    "Agenda Google ativada sem credenciais. Defina VALTIS_GOOGLE_CLIENT_ID, VALTIS_GOOGLE_CLIENT_SECRET, "
                            + "VALTIS_GOOGLE_REFRESH_TOKEN e VALTIS_GOOGLE_CALENDARIO_ID.");
        }
        this.http = http;
        this.config = config;
        this.relogio = relogio;
    }

    @Override
    public synchronized String criarEvento(NovoEvento evento) {
        Map<String, Object> corpo = Map.of(
                "id", evento.idExterno(),
                "summary", evento.titulo(),
                "description", evento.descricao(),
                // Evento de dia inteiro: o fim é exclusivo, por isso o dia seguinte.
                "start", Map.of("date", evento.data().toString()),
                "end", Map.of("date", evento.data().plusDays(1).toString()),
                "attendees", evento.convidados().stream().map(email -> Map.of("email", email)).toList());

        try {
            RespostaEvento resposta = http.post()
                    .uri(URL_CALENDAR + "/calendars/{agenda}/events?sendUpdates=all", config.calendarioId())
                    .headers(h -> h.setBearerAuth(tokenDeAcesso()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaEvento.class);
            return resposta == null || resposta.id() == null ? evento.idExterno() : resposta.id();
        } catch (HttpStatusCodeException e) {
            return tratarErroDoCalendar(e, evento);
        } catch (ResourceAccessException e) {
            throw new AgendaIndisponivelException("Sem conexão com o Google Calendar: " + e.getMessage(), e);
        }
    }

    private String tratarErroDoCalendar(HttpStatusCodeException e, NovoEvento evento) {
        HttpStatusCode status = e.getStatusCode();
        // 409: um evento com este id já existe. Uma tentativa anterior chegou lá: sucesso, sem duplicar.
        if (status.value() == 409) {
            return evento.idExterno();
        }
        if (status.value() == 401) {
            tokenExpiraEm = Instant.EPOCH;
            throw new AgendaIndisponivelException("Token de acesso recusado; será renovado na próxima tentativa.", e);
        }
        String corpo = e.getResponseBodyAsString();
        // 403 também é usado para limite de requisições (rateLimitExceeded, userRateLimitExceeded): esse é passageiro.
        if (status.value() == 403 && !corpo.contains("ateLimitExceeded")) {
            throw new AgendaNaoAutorizadaException("Sem permissão na agenda (HTTP 403). Confira o compartilhamento.");
        }
        throw new AgendaIndisponivelException("Google Calendar respondeu HTTP " + status.value(), e);
    }

    private String tokenDeAcesso() {
        Instant agora = relogio.instant();
        if (tokenDeAcesso != null && agora.isBefore(tokenExpiraEm)) {
            return tokenDeAcesso;
        }
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("client_id", config.clientId());
        formulario.add("client_secret", config.clientSecret());
        formulario.add("refresh_token", config.refreshToken());
        formulario.add("grant_type", "refresh_token");

        try {
            RespostaToken resposta = http.post()
                    .uri(URL_TOKEN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(RespostaToken.class);
            if (resposta == null || resposta.accessToken() == null) {
                throw new AgendaIndisponivelException("Google não devolveu token de acesso.", null);
            }
            tokenDeAcesso = resposta.accessToken();
            tokenExpiraEm = agora.plusSeconds(Math.max(0, resposta.expiresIn() - MARGEM_RENOVACAO_SEGUNDOS));
            return tokenDeAcesso;
        } catch (HttpStatusCodeException e) {
            // invalid_grant: refresh token revogado ou expirado. Alerta, não retentativa (architecture.md §3).
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401) {
                throw new AgendaNaoAutorizadaException(
                        "O Google recusou o refresh token (revogado ou expirado). É preciso autorizar de novo.");
            }
            throw new AgendaIndisponivelException("Falha ao renovar token: HTTP " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new AgendaIndisponivelException("Sem conexão com o Google: " + e.getMessage(), e);
        }
    }

    @Override
    public String nome() {
        return "google";
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
