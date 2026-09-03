package br.com.manutec.valtis.especificacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EspecificacaoRepository extends JpaRepository<Especificacao, UUID> {

    List<Especificacao> findByAtivoTrueOrderByMarcaAscModeloAsc();
}
