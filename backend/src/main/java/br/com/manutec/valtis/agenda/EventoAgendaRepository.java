package br.com.manutec.valtis.agenda;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EventoAgendaRepository extends JpaRepository<EventoAgenda, UUID> {

    /** O que o worker deve tentar agora: pendentes cuja espera já venceu. */
    @Query("""
            SELECT e.id FROM EventoAgenda e
            WHERE e.status = br.com.manutec.valtis.agenda.StatusEvento.PENDENTE
              AND e.proximaTentativaEm <= :agora
            ORDER BY e.proximaTentativaEm
            """)
    List<UUID> prontosParaEnvio(@Param("agora") OffsetDateTime agora, Limit limite);

    List<EventoAgenda> findByOportunidadeId(UUID oportunidadeId);

    /** Evento com tudo que vai no texto da agenda (RF-29), numa consulta só. */
    @Query("""
            SELECT e FROM EventoAgenda e
            JOIN FETCH e.oportunidade o
            JOIN FETCH o.cliente
            LEFT JOIN FETCH o.valvula v
            LEFT JOIN FETCH v.estacao
            WHERE e.id = :id
            """)
    Optional<EventoAgenda> buscarCompleto(@Param("id") UUID id);

    /** RF-33: o que ainda não chegou à agenda, para o painel de sincronização. */
    @Query("""
            SELECT e FROM EventoAgenda e
            JOIN FETCH e.oportunidade o
            JOIN FETCH o.cliente
            LEFT JOIN FETCH o.valvula
            WHERE e.status IN :status
            ORDER BY e.dataEvento
            """)
    List<EventoAgenda> comStatus(@Param("status") Collection<StatusEvento> status);

    long countByStatus(StatusEvento status);

    /** Oportunidades que têm ao menos um evento com um destes status. */
    @Query("""
            SELECT DISTINCT e.oportunidade.id FROM EventoAgenda e
            WHERE e.status IN :status
            """)
    Set<UUID> oportunidadesComEventoEm(@Param("status") Collection<StatusEvento> status);

    boolean existsByOportunidadeIdAndStatusIn(UUID oportunidadeId, Collection<StatusEvento> status);
}
