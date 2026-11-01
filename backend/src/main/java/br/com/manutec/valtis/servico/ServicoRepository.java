package br.com.manutec.valtis.servico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    /**
     * RF-21: histórico da válvula, do mais recente para o mais antigo, já com os técnicos.
     * Inclui os inativos (RN-26): a tela os mostra riscados, com o motivo.
     * LEFT JOIN: o serviço vindo do Jiraya não tem técnico nem autor do Valtis.
     */
    @Query("""
            SELECT s FROM Servico s
            LEFT JOIN FETCH s.tecnicoResponsavel LEFT JOIN FETCH s.criadoPor
            WHERE s.valvula.id = :valvulaId
            ORDER BY s.dataRealizada DESC, s.criadoEm DESC
            """)
    List<Servico> historicoDaValvula(@Param("valvulaId") UUID valvulaId);

    /** RF-80: o mesmo histórico, para a bomba. */
    @Query("""
            SELECT s FROM Servico s
            LEFT JOIN FETCH s.tecnicoResponsavel LEFT JOIN FETCH s.criadoPor
            WHERE s.bomba.id = :bombaId
            ORDER BY s.dataRealizada DESC, s.criadoEm DESC
            """)
    List<Servico> historicoDaBomba(@Param("bombaId") UUID bombaId);

    /** Data do último serviço concluído e ativo da válvula: a data-base do ciclo atual (RN-26). */
    @Query("""
            SELECT MAX(s.dataRealizada) FROM Servico s
            WHERE s.valvula.id = :valvulaId
              AND s.ativo = TRUE
              AND s.status = br.com.manutec.valtis.servico.StatusServico.CONCLUIDO
            """)
    Optional<LocalDate> ultimaDataConcluida(@Param("valvulaId") UUID valvulaId);

    @Query("""
            SELECT MAX(s.dataRealizada) FROM Servico s
            WHERE s.bomba.id = :bombaId
              AND s.ativo = TRUE
              AND s.status = br.com.manutec.valtis.servico.StatusServico.CONCLUIDO
            """)
    Optional<LocalDate> ultimaDataConcluidaDaBomba(@Param("bombaId") UUID bombaId);

    /** Último serviço ativo de várias bombas de uma vez, para o painel de bombas. */
    @Query("""
            SELECT s.bomba.id AS bombaId, MAX(s.dataRealizada) AS ultima FROM Servico s
            WHERE s.bomba.id IN :bombas
              AND s.ativo = TRUE
              AND s.status = br.com.manutec.valtis.servico.StatusServico.CONCLUIDO
            GROUP BY s.bomba.id
            """)
    List<UltimoServicoDaBomba> ultimosDasBombas(@Param("bombas") Collection<UUID> bombas);

    interface UltimoServicoDaBomba {
        UUID getBombaId();
        LocalDate getUltima();
    }

    /** Serviços que uma OS do Jiraya já gerou: reaplicar a mesma OS não duplica (RN-23). */
    List<Servico> findByJirayaOsId(Long jirayaOsId);

    boolean existsByValvulaId(UUID valvulaId);

    boolean existsByBombaId(UUID bombaId);
}
