package br.com.manutec.valtis.integracao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CursorIntegracaoRepository extends JpaRepository<CursorIntegracao, CursorIntegracao.Chave> {
}
