package br.com.manutec.valtis.servico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    /** RF-21: histórico da válvula, do mais recente para o mais antigo, já com os técnicos. */
    @Query("""
            SELECT s FROM Servico s
            JOIN FETCH s.tecnicoResponsavel JOIN FETCH s.criadoPor
            WHERE s.valvula.id = :valvulaId
            ORDER BY s.dataRealizada DESC, s.criadoEm DESC
            """)
    List<Servico> historicoDaValvula(@Param("valvulaId") UUID valvulaId);

    /** Data do último serviço concluído da válvula: a data-base do ciclo atual. */
    @Query("""
            SELECT MAX(s.dataRealizada) FROM Servico s
            WHERE s.valvula.id = :valvulaId
              AND s.status = br.com.manutec.valtis.servico.StatusServico.CONCLUIDO
            """)
    Optional<LocalDate> ultimaDataConcluida(@Param("valvulaId") UUID valvulaId);

    boolean existsByValvulaId(UUID valvulaId);
}
