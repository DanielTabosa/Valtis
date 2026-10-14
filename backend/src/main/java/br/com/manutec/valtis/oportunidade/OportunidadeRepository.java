package br.com.manutec.valtis.oportunidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OportunidadeRepository extends JpaRepository<Oportunidade, UUID> {

    /** Oportunidades abertas da válvula: as que um novo serviço fecha (RN-10). */
    @Query("""
            SELECT o FROM Oportunidade o
            WHERE o.valvula.id = :valvulaId AND o.status IN :abertos
            """)
    List<Oportunidade> abertasDaValvula(@Param("valvulaId") UUID valvulaId,
                                        @Param("abertos") Collection<StatusOportunidade> abertos);

    /** Oportunidades abertas de escopo cliente (importados): o primeiro serviço real as fecha (RF-59). */
    @Query("""
            SELECT o FROM Oportunidade o
            WHERE o.cliente.id = :clienteId
              AND o.escopo = br.com.manutec.valtis.oportunidade.EscopoOportunidade.CLIENTE
              AND o.status IN :abertos
            """)
    List<Oportunidade> abertasDeEscopoCliente(@Param("clienteId") UUID clienteId,
                                              @Param("abertos") Collection<StatusOportunidade> abertos);

    /**
     * A fila (RF-42), com tudo que a tela mostra carregado de uma vez.
     * LEFT JOIN na válvula e na estação: oportunidades de escopo cliente não têm.
     */
    @Query("""
            SELECT o FROM Oportunidade o
            JOIN FETCH o.cliente c
            LEFT JOIN FETCH o.valvula v
            LEFT JOIN FETCH v.estacao
            LEFT JOIN FETCH o.responsavel
            WHERE o.status IN :status
            ORDER BY o.dataPrevista, c.nomeCondominio
            """)
    List<Oportunidade> fila(@Param("status") Collection<StatusOportunidade> status);

    @Query("""
            SELECT o FROM Oportunidade o
            JOIN FETCH o.cliente c
            LEFT JOIN FETCH o.valvula v
            LEFT JOIN FETCH v.estacao
            LEFT JOIN FETCH o.responsavel
            WHERE o.id = :id
            """)
    Optional<Oportunidade> buscarCompleta(@Param("id") UUID id);

    @Query("""
            SELECT o FROM Oportunidade o
            LEFT JOIN FETCH o.responsavel
            WHERE o.valvula.id = :valvulaId
            ORDER BY o.dataPrevista DESC
            """)
    List<Oportunidade> daValvula(@Param("valvulaId") UUID valvulaId);
}
