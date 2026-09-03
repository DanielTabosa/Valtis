package br.com.manutec.valtis.estacao;

import br.com.manutec.valtis.cliente.Cliente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ponto de instalação que agrupa N válvulas (D-07, D-19).
 * É a unidade que a empresa fatura.
 */
@Entity
@Table(name = "estacao")
public class Estacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // LAZY: só busca o cliente no banco quando alguém pedir de fato.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "localizacao_instalacao", nullable = false)
    private String localizacaoInstalacao;

    /** Térreo = 0. Subsolo = negativo. */
    @Column(name = "andar_inicial", nullable = false)
    private Integer andarInicial;

    @Column(name = "andar_final", nullable = false)
    private Integer andarFinal;

    @Column(name = "requer_fechamento_geral", nullable = false)
    private boolean requerFechamentoGeral = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Estacao() {
    }

    public Estacao(Cliente cliente, String localizacaoInstalacao, Integer andarInicial,
                   Integer andarFinal, boolean requerFechamentoGeral) {
        this.cliente = cliente;
        this.localizacaoInstalacao = localizacaoInstalacao;
        this.andarInicial = andarInicial;
        this.andarFinal = andarFinal;
        this.requerFechamentoGeral = requerFechamentoGeral;
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getLocalizacaoInstalacao() { return localizacaoInstalacao; }
    public Integer getAndarInicial() { return andarInicial; }
    public Integer getAndarFinal() { return andarFinal; }
    public boolean isRequerFechamentoGeral() { return requerFechamentoGeral; }
    public boolean isAtivo() { return ativo; }

    public void inativar() { this.ativo = false; }
}
