package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.oportunidade.StatusOportunidade;
import br.com.manutec.valtis.suporte.Cenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O worker do Outbox, inclusive os caminhos de falha (AGENTS.md §5):
 * API fora do ar, credencial revogada, retentativa e evento que perdeu o sentido.
 */
@ExtendWith(MockitoExtension.class)
class SincronizadorDaAgendaTest {

    private static final ZoneId RECIFE = ZoneId.of("America/Recife");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 13);
    private static final OffsetDateTime AGORA = HOJE.atTime(12, 0).atZone(RECIFE).toOffsetDateTime();

    @Mock
    private EventoAgendaRepository repository;
    @Mock
    private AgendaGateway gateway;

    private final EstadoDaAgenda estado = new EstadoDaAgenda();
    private SincronizadorDaAgenda sincronizador;

    @BeforeEach
    void preparar() {
        ValtisPropriedades propriedades = new ValtisPropriedades("America/Recife", "https://valtis.teste",
                new ValtisPropriedades.Recorrencia(30, 7), null,
                new ValtisPropriedades.Agenda("simulada", 3, 300, null), null, null);
        sincronizador = new SincronizadorDaAgenda(repository, gateway, new MontadorDeEvento(propriedades), estado,
                new TransactionTemplate(new SemTransacao()), Clock.fixed(AGORA.toInstant(), RECIFE), propriedades);
    }

    @Test
    void criaOEventoComIdDecididoPorNosEGravaOIdDoGoogle() {
        EventoAgenda evento = naFila(eventoPara(HOJE.plusDays(20)));
        when(gateway.criarEvento(any())).thenReturn("google-abc");

        sincronizador.sincronizar();

        ArgumentCaptor<AgendaGateway.NovoEvento> enviado = ArgumentCaptor.forClass(AgendaGateway.NovoEvento.class);
        verify(gateway).criarEvento(enviado.capture());
        assertThat(enviado.getValue().idExterno()).isEqualTo(evento.idExterno());
        assertThat(enviado.getValue().titulo()).contains("Contato comercial", "COND-001-VRP-01");
        // RF-29: a informação operacional crítica vai no texto.
        assertThat(enviado.getValue().descricao()).contains("Exige fechamento geral: SIM");

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.CRIADO);
        assertThat(evento.getGoogleEventId()).isEqualTo("google-abc");
        assertThat(estado.retrato().ultimoSucesso()).isEqualTo(AGORA);
    }

    @Test
    void googleForaDoArContaTentativaEAgendaANova() {
        EventoAgenda evento = naFila(eventoPara(HOJE.plusDays(20)));
        when(gateway.criarEvento(any()))
                .thenThrow(new AgendaGateway.AgendaIndisponivelException("HTTP 503", null));

        sincronizador.sincronizar();

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.PENDENTE);
        assertThat(evento.getTentativas()).isEqualTo(1);
        assertThat(evento.getProximaTentativaEm()).isEqualTo(AGORA.plusMinutes(5));
        assertThat(evento.getUltimoErro()).isEqualTo("HTTP 503");
    }

    @Test
    void credencialRevogadaParaARodadaSemQueimarTentativas() {
        EventoAgenda primeiro = eventoPara(HOJE.plusDays(20));
        EventoAgenda segundo = eventoPara(HOJE.plusDays(25));
        when(repository.prontosParaEnvio(any(), any())).thenReturn(List.of(primeiro.getId(), segundo.getId()));
        when(repository.buscarCompleto(primeiro.getId())).thenReturn(Optional.of(primeiro));
        when(gateway.criarEvento(any()))
                .thenThrow(new AgendaGateway.AgendaNaoAutorizadaException("invalid_grant"));

        sincronizador.sincronizar();

        // Token revogado é alerta, não retentativa: o segundo evento nem é tentado.
        verify(repository, never()).buscarCompleto(segundo.getId());
        assertThat(primeiro.getTentativas()).isZero();
        assertThat(primeiro.getStatus()).isEqualTo(StatusEvento.PENDENTE);
        assertThat(estado.retrato().erroDeAutorizacao()).isEqualTo("invalid_grant");
    }

    @Test
    void alertaComDataPassadaEhDescartadoSemChamarOGoogle() {
        EventoAgenda evento = naFila(eventoPara(HOJE.minusDays(1)));

        sincronizador.sincronizar();

        verify(gateway, never()).criarEvento(any());
        assertThat(evento.getStatus()).isEqualTo(StatusEvento.DESCARTADO);
    }

    @Test
    void cicloFechadoAntesDoEnvioDescartaOEvento() {
        EventoAgenda evento = naFila(eventoPara(HOJE.plusDays(20)));
        evento.getOportunidade().registrarDesfecho(StatusOportunidade.PERDIDO, null, null, AGORA);

        sincronizador.sincronizar();

        verify(gateway, never()).criarEvento(any());
        assertThat(evento.getStatus()).isEqualTo(StatusEvento.DESCARTADO);
    }

    @Test
    void valvulaInativadaNaoGeraAviso() {
        EventoAgenda evento = naFila(eventoPara(HOJE.plusDays(20)));
        evento.getOportunidade().getValvula().inativar();

        sincronizador.sincronizar();

        verify(gateway, never()).criarEvento(any());
        assertThat(evento.getStatus()).isEqualTo(StatusEvento.DESCARTADO);
    }

    private EventoAgenda eventoPara(LocalDate data) {
        Oportunidade oportunidade = Cenario.oportunidade(data.plusDays(30));
        return Cenario.comId(new EventoAgenda(oportunidade, TipoEvento.ALERTA_COMERCIAL, data, AGORA));
    }

    private EventoAgenda naFila(EventoAgenda evento) {
        when(repository.prontosParaEnvio(any(), any())).thenReturn(List.of(evento.getId()));
        when(repository.buscarCompleto(evento.getId())).thenReturn(Optional.of(evento));
        lenient().when(repository.findById(evento.getId())).thenReturn(Optional.of(evento));
        return evento;
    }

    /** Gerenciador de transação que não faz nada: aqui não há banco. */
    private static class SemTransacao implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
