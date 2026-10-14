package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O adaptador do Google contra um servidor HTTP de mentira (MockRestServiceServer).
 * Nunca toca o Google de verdade (AGENTS.md §5).
 */
class GoogleCalendarGatewayTest {

    private static final String URL_EVENTOS =
            GoogleCalendarGateway.URL_CALENDAR + "/calendars/agenda-teste/events?sendUpdates=all";
    private static final String TOKEN_OK = """
            {"access_token":"token-de-acesso","expires_in":3600,"token_type":"Bearer"}""";

    private MockRestServiceServer google;
    private GoogleCalendarGateway gateway;

    private final AgendaGateway.NovoEvento evento = new AgendaGateway.NovoEvento("valtisabc123",
            LocalDate.of(2027, 2, 8), "[Valtis] Contato comercial", "texto", List.of("a@teste.local"));

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        google = MockRestServiceServer.bindTo(builder).build();
        ValtisPropriedades.Google config = new ValtisPropriedades.Google("cliente", "segredo", "refresh",
                "agenda-teste", List.of("a@teste.local"));
        gateway = new GoogleCalendarGateway(builder.build(), config,
                Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void trocaORefreshTokenECriaEventoDeDiaInteiroComNossoId() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("grant_type", "refresh_token")))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS))
                .andExpect(header("Authorization", "Bearer token-de-acesso"))
                .andExpect(jsonPath("$.id").value("valtisabc123"))
                .andExpect(jsonPath("$.start.date").value("2027-02-08"))
                .andExpect(jsonPath("$.end.date").value("2027-02-09"))
                .andExpect(jsonPath("$.attendees[0].email").value("a@teste.local"))
                .andRespond(withSuccess("{\"id\":\"valtisabc123\"}", MediaType.APPLICATION_JSON));

        assertThat(gateway.criarEvento(evento)).isEqualTo("valtisabc123");
        google.verify();
    }

    @Test
    void eventoQueJaExisteEhSucessoSemDuplicar() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS)).andRespond(withStatus(HttpStatus.CONFLICT));

        // 409: uma tentativa anterior chegou ao Google antes de a resposta se perder.
        assertThat(gateway.criarEvento(evento)).isEqualTo("valtisabc123");
    }

    @Test
    void refreshTokenRevogadoViraAlertaDeAutorizacao() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        assertThatThrownBy(() -> gateway.criarEvento(evento))
                .isInstanceOf(AgendaGateway.AgendaNaoAutorizadaException.class);
    }

    @Test
    void erroDoServidorDoGoogleEhPassageiro() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> gateway.criarEvento(evento))
                .isInstanceOf(AgendaGateway.AgendaIndisponivelException.class);
    }

    @Test
    void limiteDeRequisicoesNoFormatoDo403EhPassageiro() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"errors\":[{\"reason\":\"rateLimitExceeded\"}]}}"));

        assertThatThrownBy(() -> gateway.criarEvento(evento))
                .isInstanceOf(AgendaGateway.AgendaIndisponivelException.class);
    }

    @Test
    void reaproveitaOTokenEnquantoEleValer() {
        google.expect(requestTo(GoogleCalendarGateway.URL_TOKEN))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS)).andRespond(withSuccess("{\"id\":\"a\"}", MediaType.APPLICATION_JSON));
        google.expect(requestTo(URL_EVENTOS)).andRespond(withSuccess("{\"id\":\"b\"}", MediaType.APPLICATION_JSON));

        gateway.criarEvento(evento);
        gateway.criarEvento(evento);

        google.verify(); // um token só para as duas chamadas
    }
}
