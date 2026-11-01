package br.com.manutec.valtis.integracao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoRecebidoRepository extends JpaRepository<EventoRecebido, UUID> {

    boolean existsByOrigemAndTipoAndIdExternoAndVersao(OrigemIntegracao origem, TipoEventoRecebido tipo,
                                                       long idExterno, String versao);

    /** As versões anteriores do mesmo item que ainda estão em aberto: a nova as supera. */
    @Query("""
            SELECT e FROM EventoRecebido e
            WHERE e.origem = :origem AND e.tipo = :tipo AND e.idExterno = :idExterno AND e.estado IN :abertos
            """)
    List<EventoRecebido> emAbertoDoItem(@Param("origem") OrigemIntegracao origem,
                                        @Param("tipo") TipoEventoRecebido tipo,
                                        @Param("idExterno") long idExterno,
                                        @Param("abertos") Collection<EstadoEventoRecebido> abertos);

    /**
     * Prontos para aplicar, na ordem que respeita as dependências: cliente, depois
     * equipamento, depois OS; dentro de cada tipo, na ordem em que chegaram.
     */
    @Query(value = """
            SELECT id FROM evento_recebido
            WHERE estado = 'pendente' AND proxima_tentativa_em <= :agora
            ORDER BY CASE tipo WHEN 'cliente' THEN 0 WHEN 'equipamento' THEN 1 ELSE 2 END, recebido_em, id
            LIMIT :limite
            """, nativeQuery = true)
    List<UUID> prontos(@Param("agora") OffsetDateTime agora, @Param("limite") int limite);

    /** O evento mais recente de um item: diz, por exemplo, se o equipamento foi ignorado por tipo. */
    Optional<EventoRecebido> findFirstByOrigemAndTipoAndIdExternoOrderByRecebidoEmDesc(
            OrigemIntegracao origem, TipoEventoRecebido tipo, long idExterno);

    /** RF-75: o que precisa de uma pessoa. */
    @Query("""
            SELECT e FROM EventoRecebido e
            WHERE e.estado IN :estados
            ORDER BY e.recebidoEm DESC
            """)
    List<EventoRecebido> comEstado(@Param("estados") Collection<EstadoEventoRecebido> estados);

    long countByEstado(EstadoEventoRecebido estado);
}
