package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O ciclo de vida de um item lido (D-30, RN-23, RF-75): retentativa, pendência, descarte. */
class EventoRecebidoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 10, 13, 15, 0, 0, 0, ZoneOffset.UTC);

    private EventoRecebido novo() {
        return new EventoRecebido(OrigemIntegracao.JIRAYA, TipoEventoRecebido.ORDEM_SERVICO, 1234,
                "2026-10-12T10:00:00-03:00", "{}", AGORA);
    }

    @Test
    void falhaTecnicaEsperaCadaVezMaisEDesisteNoLimite() {
        EventoRecebido e = novo();
        e.falhar("HTTP 503", AGORA, 3, 300);
        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.PENDENTE);
        assertThat(e.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(300));

        e.falhar("HTTP 503", AGORA, 3, 300);
        assertThat(e.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(600));

        e.falhar("HTTP 503", AGORA, 3, 300);
        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.FALHOU);
        assertThat(e.getUltimoErro()).isEqualTo("HTTP 503");
    }

    @Test
    void dependenciaQueNaoChegaViraPendenciaComOMotivo() {
        EventoRecebido e = novo();
        e.aguardar("Equipamento ainda não chegou", AGORA, 2, 300);
        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.PENDENTE);

        e.aguardar("Equipamento ainda não chegou", AGORA, 2, 300);
        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.PENDENCIA);
        assertThat(e.getMotivo()).isEqualTo("Equipamento ainda não chegou");
    }

    @Test
    void esperaTemTetoDeSeisHoras() {
        EventoRecebido e = novo();
        for (int i = 0; i < 10; i++) {
            e.falhar("erro", AGORA, 50, 300);
        }
        assertThat(e.getProximaTentativaEm()).isEqualTo(AGORA.plusHours(6));
    }

    @Test
    void descartarExigeJustificativaESoValeParaPendencia() {
        EventoRecebido e = novo();
        assertThatThrownBy(() -> e.descartar(null, "qualquer", AGORA)).isInstanceOf(RegraDeNegocioException.class);

        e.virarPendencia("OS reaberta");
        assertThatThrownBy(() -> e.descartar(null, " ", AGORA)).isInstanceOf(RegraDeNegocioException.class);

        e.descartar(null, "Reaberta só para anexar foto", AGORA);
        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.DESCARTADO);
    }

    @Test
    void reprocessarVoltaAFilaDoZero() {
        EventoRecebido e = novo();
        e.falhar("erro", AGORA, 1, 300);
        e.reprocessar(null, AGORA.plusDays(1));

        assertThat(e.getEstado()).isEqualTo(EstadoEventoRecebido.PENDENTE);
        assertThat(e.getTentativas()).isZero();
        assertThat(e.getProximaTentativaEm()).isEqualTo(AGORA.plusDays(1));
    }

    @Test
    void versaoNovaSuperaASoQuandoEstaEmAberto() {
        EventoRecebido pendente = novo();
        pendente.superar();
        assertThat(pendente.getEstado()).isEqualTo(EstadoEventoRecebido.SUPERADO);

        EventoRecebido aplicado = novo();
        aplicado.aplicado(AGORA);
        aplicado.superar();
        assertThat(aplicado.getEstado()).isEqualTo(EstadoEventoRecebido.APLICADO);
    }
}
