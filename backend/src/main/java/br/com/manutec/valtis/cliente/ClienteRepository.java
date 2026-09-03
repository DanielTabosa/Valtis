package br.com.manutec.valtis.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByAtivoTrueOrderByNomeCondominio();

    boolean existsByCodigoReferencia(String codigoReferencia);
}
