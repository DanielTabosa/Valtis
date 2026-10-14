package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O worker do Outbox (D-13, RF-31): lê a fila de eventos pendentes e os cria
 * na agenda, em segundo plano.
 *
 * Regras (architecture.md §3):
 * - Idempotente: o id do evento é decidido por nós antes da chamada. Rodar
 *   duas vezes não duplica.
 * - Retentativa com espera crescente e limite de tentativas.
 * - Token revogado é alerta, não retentativa: a rodada para na hora.
 * - A chamada ao Google acontece FORA de transação: uma resposta lenta do
 *   Google não segura conexão do banco.
 */
@Component
// Com a agenda desativada não há adaptador, e portanto não há worker: os eventos ficam pendentes.
@ConditionalOnExpression("'${valtis.agenda.provedor}' != 'desativada'")
public class SincronizadorDaAgenda {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorDaAgenda.class);
    private static final int LOTE = 50;

    private final EventoAgendaRepository repository;
    private final AgendaGateway gateway;
    private final MontadorDeEvento montador;
    private final EstadoDaAgenda estado;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final int maxTentativas;
    private final long esperaInicialSegundos;

    public SincronizadorDaAgenda(EventoAgendaRepository repository, AgendaGateway gateway, MontadorDeEvento montador,
                                 EstadoDaAgenda estado, TransactionTemplate transacao, Clock relogio,
                                 ValtisPropriedades propriedades) {
        this.repository = repository;
        this.gateway = gateway;
        this.montador = montador;
        this.estado = estado;
        this.transacao = transacao;
        this.relogio = relogio;
        this.maxTentativas = propriedades.agenda().maxTentativas();
        this.esperaInicialSegundos = propriedades.agenda().esperaInicialSegundos();
    }

    @Scheduled(fixedDelayString = "${valtis.agenda.intervalo-ms}", initialDelayString = "${valtis.agenda.atraso-inicial-ms}")
    public void sincronizar() {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        estado.registrarExecucao(agora);

        List<UUID> prontos = repository.prontosParaEnvio(agora, Limit.of(LOTE));
        for (UUID id : prontos) {
            if (!enviar(id)) {
                return;
            }
        }
    }

    /** @return false quando a rodada deve parar (credencial recusada). */
    boolean enviar(UUID id) {
        AgendaGateway.NovoEvento novo = transacao.execute(status -> prepararOuDescartar(id));
        if (novo == null) {
            return true;
        }

        try {
            String googleId = gateway.criarEvento(novo);
            transacao.executeWithoutResult(status ->
                    repository.findById(id).ifPresent(e -> e.marcarCriado(googleId, OffsetDateTime.now(relogio))));
            estado.registrarSucesso(OffsetDateTime.now(relogio));
            return true;
        } catch (AgendaGateway.AgendaNaoAutorizadaException e) {
            // Não conta como tentativa do evento: o problema é a credencial, não ele.
            log.error("Agenda recusou a credencial. Sincronização suspensa até nova autorização: {}", e.getMessage());
            estado.registrarErroDeAutorizacao(e.getMessage(), OffsetDateTime.now(relogio));
            return false;
        } catch (RuntimeException e) {
            log.warn("Falha ao criar evento {} na agenda: {}", id, e.getMessage());
            transacao.executeWithoutResult(status -> repository.findById(id).ifPresent(evento ->
                    evento.registrarFalha(e.getMessage(), OffsetDateTime.now(relogio), maxTentativas,
                            esperaInicialSegundos)));
            return true;
        }
    }

    /**
     * Confere, imediatamente antes do envio, se o evento ainda faz sentido.
     * Entre o enfileiramento e agora o ciclo pode ter fechado, a válvula
     * pode ter sido inativada ou a data pode ter passado.
     */
    private AgendaGateway.NovoEvento prepararOuDescartar(UUID id) {
        EventoAgenda evento = repository.buscarCompleto(id).orElse(null);
        if (evento == null || !evento.isPendente()) {
            return null;
        }
        Oportunidade o = evento.getOportunidade();
        if (!o.isAberta()) {
            evento.descartar("Oportunidade encerrada antes do envio");
            return null;
        }
        // RN-12: cliente ou válvula inativos não geram aviso.
        if (!o.getCliente().isAtivo() || (o.getValvula() != null && !o.getValvula().isOperante())) {
            evento.descartar("Cliente ou válvula inativos");
            return null;
        }
        // Não cria evento no passado (RN-13): só polui a agenda.
        if (evento.getDataEvento().isBefore(LocalDate.now(relogio))) {
            evento.descartar("A data do alerta já passou");
            return null;
        }
        return montador.montar(evento);
    }
}
