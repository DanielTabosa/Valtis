package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Painel de sincronização com a agenda (RF-33). */
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    /** `ultimoErro` guarda o erro do envio ou, no descartado, o motivo do descarte. */
    public record EventoDaAgenda(UUID id, TipoEvento tipo, LocalDate dataEvento, StatusEvento status, int tentativas,
                                 OffsetDateTime proximaTentativaEm, String ultimoErro, String nomeCondominio,
                                 String alvo) {}

    public record Situacao(String provedor, boolean sincronizacaoAtiva, Map<StatusEvento, Long> totais,
                           EstadoDaAgenda.Retrato estado, List<EventoDaAgenda> eventos, boolean listaCortada) {}

    /** Criados e descartados só acumulam; a tela mostra um recorte. */
    private static final int EVENTOS_NA_TELA = 200;

    /** O que precisa de atenção primeiro: falha, depois o que ainda vai sair, depois o histórico. */
    private static final List<StatusEvento> PRIORIDADE =
            List.of(StatusEvento.FALHOU, StatusEvento.PENDENTE, StatusEvento.CRIADO, StatusEvento.DESCARTADO);

    private final EventoAgendaRepository repository;
    private final EstadoDaAgenda estado;
    private final ObjectProvider<SincronizadorDaAgenda> sincronizador;
    private final String provedor;
    private final Clock relogio;

    public AgendaController(EventoAgendaRepository repository, EstadoDaAgenda estado,
                            ObjectProvider<SincronizadorDaAgenda> sincronizador, ValtisPropriedades propriedades,
                            Clock relogio) {
        this.repository = repository;
        this.estado = estado;
        this.sincronizador = sincronizador;
        this.provedor = propriedades.agenda().provedor();
        this.relogio = relogio;
    }

    @GetMapping("/situacao")
    @Transactional(readOnly = true)
    public Situacao situacao(@RequestParam(required = false) StatusEvento status) {
        // Como no painel: os cartões contam tudo, a lista obedece ao filtro.
        Map<StatusEvento, Long> totais = EnumSet.allOf(StatusEvento.class).stream()
                .collect(Collectors.toMap(Function.identity(), repository::countByStatus));

        List<EventoDaAgenda> eventos = listar(status == null ? EnumSet.allOf(StatusEvento.class) : EnumSet.of(status))
                .stream()
                .sorted(Comparator.comparing((EventoDaAgenda e) -> PRIORIDADE.indexOf(e.status()))
                        .thenComparing(EventoDaAgenda::dataEvento))
                .toList();

        return new Situacao(provedor, sincronizador.getIfAvailable() != null, totais, estado.retrato(),
                eventos.stream().limit(EVENTOS_NA_TELA).toList(), eventos.size() > EVENTOS_NA_TELA);
    }

    private List<EventoDaAgenda> listar(EnumSet<StatusEvento> status) {
        return repository.comStatus(status)
                .stream()
                .map(e -> new EventoDaAgenda(e.getId(), e.getTipo(), e.getDataEvento(), e.getStatus(),
                        e.getTentativas(), e.getProximaTentativaEm(), e.getUltimoErro(),
                        e.getOportunidade().getCliente().getNomeCondominio(),
                        e.getOportunidade().codigoDoAlvo()))
                .toList();
    }

    @PostMapping("/eventos/{id}/reprocessar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reprocessar(@PathVariable UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."))
                .reprocessar(OffsetDateTime.now(relogio));
    }

    /** Roda o worker agora, sem esperar o próximo ciclo. Útil depois de corrigir uma credencial. */
    @PostMapping("/sincronizar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void sincronizarAgora() {
        SincronizadorDaAgenda worker = sincronizador.getIfAvailable();
        if (worker == null) {
            throw new RegraDeNegocioException("A integração com a agenda está desativada neste ambiente.");
        }
        worker.sincronizar();
    }
}
