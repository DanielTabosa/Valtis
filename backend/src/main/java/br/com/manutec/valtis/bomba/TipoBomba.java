package br.com.manutec.valtis.bomba;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Catálogo configurável de tipos de bombeamento (RF-60): recalque, incêndio, drenagem… */
@Entity
@Table(name = "tipo_bomba")
public class TipoBomba {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected TipoBomba() {
    }

    public TipoBomba(String nome) {
        this.nome = nome;
    }

    public void renomear(String nome) { this.nome = nome; }
    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
}
