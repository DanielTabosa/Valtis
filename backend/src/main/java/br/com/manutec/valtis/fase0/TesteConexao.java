package br.com.manutec.valtis.fase0;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entidade espelhando a tabela criada em V1__teste_conexao.sql.
 *
 * Como o ddl-auto está em 'validate', o Hibernate NÃO cria esta tabela —
 * ele apenas confere se o que está aqui bate com o que o Flyway criou.
 * Se você errar um nome de coluna, a aplicação se recusa a subir.
 *
 * Código de Fase 0 — será removido antes da Fase 1.
 */
@Entity
@Table(name = "teste_conexao")
public class TesteConexao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mensagem;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    // O JPA exige um construtor sem argumentos para conseguir instanciar
    // a entidade quando lê do banco.
    protected TesteConexao() {
    }

    public Long getId() {
        return id;
    }

    public String getMensagem() {
        return mensagem;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
