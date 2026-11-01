package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O grupo de bombas de uma função no prédio (recalque, incêndio…).
 * Equivale à estação, para bombas (D-26). As posições (01, 02…) não são
 * entidade: existem só como número dentro de cada ocupação (D-27).
 */
@Entity
@Table(name = "conjunto_bombeamento")
public class ConjuntoBombeamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    /** O tipo é do conjunto, não de cada bomba (P-15). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_bomba_id", nullable = false)
    private TipoBomba tipo;

    @Column(nullable = false)
    private String localizacao;

    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * Conjunto derivado do Jiraya (RF-73, Q-4): a localização interna normalizada.
     * Um conjunto por (cliente, tipo, chave). Nula no conjunto cadastrado à mão.
     */
    @Column(name = "chave_integracao", updatable = false)
    private String chaveIntegracao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected ConjuntoBombeamento() {
    }

    public ConjuntoBombeamento(Cliente cliente, TipoBomba tipo, String localizacao, String observacoes) {
        this.cliente = cliente;
        atualizar(tipo, localizacao, observacoes);
    }

    public void atualizar(TipoBomba tipo, String localizacao, String observacoes) {
        this.tipo = tipo;
        this.localizacao = localizacao;
        this.observacoes = observacoes;
    }

    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    /** RF-73: o conjunto que agrupa as bombas de um tipo numa localização interna do Jiraya. */
    public static ConjuntoBombeamento derivadoDoJiraya(Cliente cliente, TipoBomba tipo, String localizacao,
                                                       String chaveIntegracao) {
        ConjuntoBombeamento c = new ConjuntoBombeamento(cliente, tipo, localizacao, null);
        c.chaveIntegracao = chaveIntegracao;
        return c;
    }

    public boolean isDerivado() {
        return chaveIntegracao != null;
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public TipoBomba getTipo() { return tipo; }
    public String getLocalizacao() { return localizacao; }
    public String getChaveIntegracao() { return chaveIntegracao; }
    public String getObservacoes() { return observacoes; }
    public boolean isAtivo() { return ativo; }
}
