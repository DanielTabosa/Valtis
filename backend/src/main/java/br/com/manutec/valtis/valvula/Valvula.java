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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estacao_id", nullable = false)
    private Estacao estacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especificacao_id", nullable = false)
    private Especificacao especificacao;

    @Column(name = "codigo_referencia", nullable = false, unique = true)
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
    private Integer periodicidadeMeses = 12;

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

    public Valvula(Estacao estacao, Especificacao especificacao, String codigoReferencia,
                   String numeroValvula, String numeroSerie, LocalDate dataInstalacao,
                   Integer periodicidadeMeses) {
        this.estacao = estacao;
        this.especificacao = especificacao;
        this.codigoReferencia = codigoReferencia;
        this.numeroValvula = numeroValvula;
        this.numeroSerie = numeroSerie;
        this.dataInstalacao = dataInstalacao;
        if (periodicidadeMeses != null) {
            this.periodicidadeMeses = periodicidadeMeses;
        }
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

    public void inativar() { this.ativo = false; }
}
