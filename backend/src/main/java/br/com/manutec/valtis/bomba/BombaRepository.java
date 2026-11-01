package br.com.manutec.valtis.bomba;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BombaRepository extends JpaRepository<Bomba, UUID> {

    List<Bomba> findByClienteIdOrderByCodigoReferencia(UUID clienteId);

    List<Bomba> findByPatrimonioManutecTrueOrderByCodigoReferencia();

    Optional<Bomba> findByJirayaEquipamentoId(Long jirayaEquipamentoId);

    @Query("SELECT b FROM Bomba b LEFT JOIN FETCH b.cliente WHERE b.id = :id")
    Optional<Bomba> buscarCompleta(@Param("id") UUID id);

    /** Painel de bombas: as ativas de clientes ativos, mais o patrimônio ativo. */
    @Query("""
            SELECT b FROM Bomba b LEFT JOIN FETCH b.cliente c
            WHERE b.ativo = TRUE AND (c IS NULL OR c.ativo = TRUE)
            ORDER BY b.codigoReferencia
            """)
    List<Bomba> ativasParaOPainel();

    /** Maior sequencial dos códigos COND-001-BMB-NN do cliente (RN-15). Inclui inativas: código não volta. */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_referencia FROM '-BMB-([0-9]+)$') AS INTEGER)), 0)
            FROM bomba
            WHERE cliente_id = :clienteId
            """, nativeQuery = true)
    int maiorSequencialDoCliente(@Param("clienteId") UUID clienteId);

    /** Maior sequencial dos códigos MAN-BMB-NNN do patrimônio (RN-15). */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_referencia FROM '^MAN-BMB-([0-9]+)$') AS INTEGER)), 0)
            FROM bomba
            WHERE patrimonio_manutec = TRUE
            """, nativeQuery = true)
    int maiorSequencialDoPatrimonio();
}
