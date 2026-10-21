package br.com.manutec.valtis.bomba;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OcupacaoRepository extends JpaRepository<OcupacaoBomba, UUID> {

    /** Ocupações abertas de uma bomba. Normalmente zero ou uma. */
    @Query("""
            SELECT o FROM OcupacaoBomba o JOIN FETCH o.conjunto c JOIN FETCH c.cliente
            WHERE o.bomba.id = :bombaId AND o.fim IS NULL
            """)
    List<OcupacaoBomba> abertasDaBomba(@Param("bombaId") UUID bombaId);

    /** Ocupações abertas de várias bombas de uma vez, para listas (RF-68). */
    @Query("""
            SELECT o FROM OcupacaoBomba o JOIN FETCH o.conjunto c JOIN FETCH c.cliente
            WHERE o.bomba.id IN :bombas AND o.fim IS NULL
            """)
    List<OcupacaoBomba> abertasDasBombas(@Param("bombas") Collection<UUID> bombas);

    /** Quem está em cada posição dos conjuntos: abertas, com a bomba carregada. */
    @Query("""
            SELECT o FROM OcupacaoBomba o JOIN FETCH o.bomba
            WHERE o.conjunto.id IN :conjuntos AND o.fim IS NULL
            ORDER BY o.numeroPosicao, o.inicio
            """)
    List<OcupacaoBomba> abertasDosConjuntos(@Param("conjuntos") Collection<UUID> conjuntos);

    /** RF-67: histórico individual, do mais recente para o mais antigo. */
    @Query("""
            SELECT o FROM OcupacaoBomba o
            JOIN FETCH o.conjunto c JOIN FETCH c.cliente JOIN FETCH o.registradoPor
            LEFT JOIN FETCH o.bombaSubstituida
            WHERE o.bomba.id = :bombaId
            ORDER BY o.inicio DESC, o.criadoEm DESC
            """)
    List<OcupacaoBomba> historicoDaBomba(@Param("bombaId") UUID bombaId);

    @Query("SELECT o FROM OcupacaoBomba o JOIN FETCH o.bomba JOIN FETCH o.conjunto WHERE o.id = :id")
    Optional<OcupacaoBomba> buscarCompleta(@Param("id") UUID id);

    /** Maior número de posição já usado no conjunto, inclusive em ocupações encerradas. */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(numero_posicao AS INTEGER)), 0)
            FROM ocupacao_bomba
            WHERE conjunto_id = :conjuntoId
              AND numero_posicao ~ '^[0-9]+$'
            """, nativeQuery = true)
    int maiorPosicaoNoConjunto(@Param("conjuntoId") UUID conjuntoId);

    boolean existsByBombaId(UUID bombaId);
}
