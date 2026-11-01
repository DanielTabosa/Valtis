package br.com.manutec.valtis.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * RF-11: busca por nome, código, bairro ou cidade. Busca vazia ("") devolve todos.
     * Nunca passe null: o Postgres recebe texto nulo como bytea e a consulta quebra.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE (:incluirInativos = TRUE OR c.ativo = TRUE)
              AND (:busca = ''
                   OR LOWER(c.nomeCondominio)   LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(c.codigoReferencia) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(c.bairro)           LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(c.cidade)           LIKE LOWER(CONCAT('%', :busca, '%')))
            ORDER BY c.nomeCondominio
            """)
    List<Cliente> buscar(@Param("busca") String busca, @Param("incluirInativos") boolean incluirInativos);

    /**
     * Maior número já usado em códigos COND-NNN. Considera inativos também:
     * um código nunca é reaproveitado, nem de cliente inativado.
     */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_referencia FROM 6) AS INTEGER)), 0)
            FROM cliente
            WHERE codigo_referencia ~ '^COND-[0-9]+$'
            """, nativeQuery = true)
    int maiorNumeroDeCodigo();

    Optional<Cliente> findByManutecId(Long manutecId);
}
