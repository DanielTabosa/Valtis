package br.com.manutec.valtis.estacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstacaoRepository extends JpaRepository<Estacao, UUID> {

    List<Estacao> findByClienteIdOrderByLocalizacaoInstalacao(UUID clienteId);

    @Query("SELECT e FROM Estacao e JOIN FETCH e.cliente WHERE e.id = :id")
    Optional<Estacao> buscarComCliente(@Param("id") UUID id);
}
