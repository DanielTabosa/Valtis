package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.suporte.Cenario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de retentativa do Outbox (architecture.md §3). */
class EventoAgendaTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-13T12:00:00-03:00");
    private static final int MAX_TENTATIVAS = 4;
    private static final long ESPERA_INICIAL = 300;

    private EventoAgenda novoEvento() {
        return Cenario.comId(new EventoAgenda(Cenario.oportunidade(LocalDate.of(2027, 3, 10)),
                TipoEvento.ALERTA_COMERCIAL, LocalDate.of(2027, 2, 8), AGORA));
    }

    @Test
    void esperaDobraACadaFalha() {
        EventoAgenda evento = novoEvento();

        evento.registrarFalha("HTTP 503", AGORA, MAX_TENTATIVAS, ESPERA_INICIAL);
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusMinutes(5));

        evento.registrarFalha("HTTP 503", AGORA, MAX_TENTATIVAS, ESPERA_INICIAL);
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusMinutes(10));

        evento.registrarFalha("HTTP 503", AGORA, MAX_TENTATIVAS, ESPERA_INICIAL);
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusMinutes(20));
        assertThat(evento.getStatus()).isEqualTo(StatusEvento.PENDENTE);
    }

    @Test
    void esperaTemTeto() {
        EventoAgenda evento = novoEvento();
        for (int i = 0; i < 15; i++) {
            evento.registrarFalha("HTTP 503", AGORA, 100, ESPERA_INICIAL);
        }
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusHours(6));
    }

    @Test
    void esgotadoOLimiteParaEFicaVisivelComoFalhou() {
        EventoAgenda evento = novoEvento();
        for (int i = 0; i < MAX_TENTATIVAS; i++) {
            evento.registrarFalha("HTTP 500", AGORA, MAX_TENTATIVAS, ESPERA_INICIAL);
        }

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.FALHOU);
        assertThat(evento.getUltimoErro()).isEqualTo("HTTP 500");
    }

    @Test
    void reprocessarZeraAsTentativas() {
        EventoAgenda evento = novoEvento();
        for (int i = 0; i < MAX_TENTATIVAS; i++) {
            evento.registrarFalha("HTTP 500", AGORA, MAX_TENTATIVAS, ESPERA_INICIAL);
        }

        evento.reprocessar(AGORA.plusDays(1));

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.PENDENTE);
        assertThat(evento.getTentativas()).isZero();
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusDays(1));
    }

    @Test
    void soReprocessaOQueFalhou() {
        assertThatThrownBy(() -> novoEvento().reprocessar(AGORA)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void eventoJaCriadoNaoEDescartado() {
        EventoAgenda evento = novoEvento();
        evento.marcarCriado("google-123", AGORA);

        evento.descartar("ciclo fechou");

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.CRIADO);
    }

    @Test
    void idExternoEhEstavelEValidoParaOGoogle() {
        EventoAgenda evento = novoEvento();

        // O Google aceita só a-v e 0-9, de 5 a 1024 caracteres.
        assertThat(evento.idExterno()).matches("[a-v0-9]{5,1024}");
        // Mesmo evento, mesmo id: é o que impede duplicar numa retentativa.
        assertThat(evento.idExterno()).isEqualTo(evento.idExterno());
    }
}
