package br.com.manutec.valtis.especificacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EspecificacaoRepository extends JpaRepository<Especificacao, UUID> {

    List<Especificacao> findAllByOrderByMarcaAscModeloAscDiametroPolegadasAsc();

    List<Especificacao> findByAtivoTrueOrderByMarcaAscModeloAscDiametroPolegadasAsc();

    /** A mesma combinação ignorando maiúsculas: "Bermad 720" e "bermad 720" são uma coisa só (D-17). */
    Optional<Especificacao> findByMarcaIgnoreCaseAndModeloIgnoreCaseAndDiametroPolegadasAndTipoRegistro(
            String marca, String modelo, String diametroPolegadas, TipoRegistro tipoRegistro);
}
