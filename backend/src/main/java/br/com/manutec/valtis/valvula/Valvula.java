package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.especificacao.Especificacao;
import br.com.manutec.valtis.estacao.Estacao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O equipamento físico. É quem tem ciclo de manutenção.
 */
@Entity
@Table(name = "valvula")
public class Valvula {

    /** RN-02: 12 meses para toda VRP, salvo exceção registrada na própria válvula. */
    public static final int PERIODICIDADE_PADRAO_MESES = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estacao_id", nullable = false, updatable = false)
    private Estacao estacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especificacao_id", nullable = false)
    private Especificacao especificacao;

    /** COND-001-VRP-01 (RN-14). Gerado pelo sistema, nunca muda. */
    @Column(name = "codigo_referencia", nullable = false, unique = true, updatable = false)
    private String codigoReferencia;

    /** 01, 02, 03... 01 = esquerda (paralelo) ou de cima (sobrepostas). */
    @Column(name = "numero_valvula", nullable = false)
    private String numeroValvula;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @Column(name = "data_instalacao")
    private LocalDate dataInstalacao;

    /** Padrão 12 meses. Editável só em exceções (RN-02). */
    @Column(name = "periodicidade_meses", nullable = false)
    private Integer periodicidadeMeses = PERIODICIDADE_PADRAO_MESES;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Valvula() {
    }

    public Valvula(Estacao estacao, String codigoReferencia, String numeroValvula, Especificacao especificacao,
                   String numeroSerie, LocalDate dataInstalacao, Integer periodicidadeMeses) {
        this.estacao = estacao;
        this.codigoReferencia = codigoReferencia;
        atualizar(numeroValvula, especificacao, numeroSerie, dataInstalacao, periodicidadeMeses);
    }

    public void atualizar(String numeroValvula, Especificacao especificacao, String numeroSerie,
                          LocalDate dataInstalacao, Integer periodicidadeMeses) {
        this.numeroValvula = numeroValvula;
        this.especificacao = especificacao;
        this.numeroSerie = numeroSerie;
        this.dataInstalacao = dataInstalacao;
        this.periodicidadeMeses = periodicidadeMeses != null ? periodicidadeMeses : PERIODICIDADE_PADRAO_MESES;
    }

    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    /** Uma válvula só entra em operação se ela, a estação e o cliente estiverem ativos (RN-12). */
    public boolean isOperante() {
        return ativo && estacao.isAtivo() && estacao.getCliente().isAtivo();
    }

    public UUID getId() { return id; }
    public Estacao getEstacao() { return estacao; }
    public Especificacao getEspecificacao() { return especificacao; }
    public String getCodigoReferencia() { return codigoReferencia; }
    public String getNumeroValvula() { return numeroValvula; }
    public String getNumeroSerie() { return numeroSerie; }
    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public Integer getPeriodicidadeMeses() { return periodicidadeMeses; }
    public boolean isAtivo() { return ativo; }
}
