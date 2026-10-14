package br.com.manutec.valtis.usuario;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt. A senha em si nunca é gravada (RNF-20). */
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Convert(converter = Perfil.Conversor.class)
    @Column(nullable = false)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public void alterar(String nome, Perfil perfil) {
        this.nome = nome;
        this.perfil = perfil;
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    public boolean isAdmin() { return perfil == Perfil.ADMIN; }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
