package br.com.manutec.valtis.bomba;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConjuntoRepository extends JpaRepository<ConjuntoBombeamento, UUID> {

    @Query("""
            SELECT c FROM ConjuntoBombeamento c JOIN FETCH c.tipo
            WHERE c.cliente.id = :clienteId
            ORDER BY c.localizacao
            """)
    List<ConjuntoBombeamento> doCliente(@Param("clienteId") UUID clienteId);

    @Query("SELECT c FROM ConjuntoBombeamento c JOIN FETCH c.cliente JOIN FETCH c.tipo WHERE c.id = :id")
    Optional<ConjuntoBombeamento> buscarCompleto(@Param("id") UUID id);
}
