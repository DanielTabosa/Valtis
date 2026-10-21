package br.com.manutec.valtis.bomba;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipoBombaRepository extends JpaRepository<TipoBomba, UUID> {

    List<TipoBomba> findAllByOrderByNome();

    List<TipoBomba> findByAtivoTrueOrderByNome();

    Optional<TipoBomba> findByNomeIgnoreCase(String nome);
}
