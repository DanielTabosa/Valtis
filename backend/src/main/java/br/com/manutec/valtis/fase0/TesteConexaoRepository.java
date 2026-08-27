package br.com.manutec.valtis.fase0;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do Spring Data.
 *
 * Note que é uma INTERFACE e não tem implementação. O Spring Data gera a
 * implementação em tempo de execução: herdando de JpaRepository você já
 * ganha findAll, findById, save, delete e outros, sem escrever SQL.
 *
 * Código de Fase 0 — será removido antes da Fase 1.
 */
public interface TesteConexaoRepository extends JpaRepository<TesteConexao, Long> {
}
