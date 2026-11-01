package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A aplicação sobe com a agenda Google ligada (D-23). Credenciais fictícias: nada
 * é chamado no Google aqui, o worker só rodaria depois do atraso inicial.
 *
 * Reproduz o bug de 01/11/2026: o adaptador pedia um RestClient.Builder que o
 * Spring Boot 4 só fornece com o starter de restclient, e a subida falhava.
 */
@TestPropertySource(properties = {
        "valtis.agenda.provedor=google",
        "valtis.agenda.atraso-inicial-ms=3600000",
        "valtis.agenda.google.client-id=cliente-ficticio",
        "valtis.agenda.google.client-secret=segredo-ficticio",
        "valtis.agenda.google.refresh-token=refresh-ficticio",
        "valtis.agenda.google.calendario-id=agenda-ficticia"})
class ContextoComAgendaGoogleTest extends TesteDeIntegracao {

    @Autowired
    private AgendaGateway gateway;

    @Test
    void sobeComOAdaptadorDoGoogle() {
        assertThat(gateway).isInstanceOf(GoogleCalendarGateway.class);
    }
}
