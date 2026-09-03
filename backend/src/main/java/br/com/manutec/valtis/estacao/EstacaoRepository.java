package br.com.manutec.valtis.estacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstacaoRepository extends JpaRepository<Estacao, UUID> {

    List<Estacao> findByClienteIdAndAtivoTrue(UUID clienteId);
}
