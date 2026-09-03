package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.valvula.Valvula;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A intervenção realizada. Uma linha por VÁLVULA atendida (D-18),
 * mesmo quando a visita cobre a estação inteira.
 */
@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valvula_id", nullable = false)
    private Valvula valvula;

    /** Informada pelo técnico, nunca assumida como a data de preenchimento (RF-17, D-02). */
    @Column(name = "data_realizada", nullable = false)
    private LocalDate dataRealizada;

    @Column(name = "tipo_manutencao", nullable = false)
    private String tipoManutencao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tecnico_responsavel_id", nullable = false)
    private Usuario tecnicoResponsavel;

    @Column(name = "pecas_substituidas")
    private String pecasSubstituidas;

    /** Opcional (D-10). */
    @Column(name = "pressao_entrada")
    private BigDecimal pressaoEntrada;

    @Column(name = "pressao_saida")
    private BigDecimal pressaoSaida;

    private String observacoes;

    @Column(nullable = false)
    private String status = "concluido";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Servico() {
    }

    public Servico(Valvula valvula, LocalDate dataRealizada, String tipoManutencao,
                   Usuario tecnicoResponsavel, String pecasSubstituidas,
                   BigDecimal pressaoEntrada, BigDecimal pressaoSaida,
                   String observacoes, Usuario criadoPor) {
        this.valvula = valvula;
        this.dataRealizada = dataRealizada;
        this.tipoManutencao = tipoManutencao;
        this.tecnicoResponsavel = tecnicoResponsavel;
        this.pecasSubstituidas = pecasSubstituidas;
        this.pressaoEntrada = pressaoEntrada;
        this.pressaoSaida = pressaoSaida;
        this.observacoes = observacoes;
        this.criadoPor = criadoPor;
    }

    /**
     * RN-01: a próxima manutenção é calculada a partir da data REALIZADA,
     * somada à periodicidade da própria válvula.
     */
    public LocalDate calcularProximaManutencao() {
        return dataRealizada.plusMonths(valvula.getPeriodicidadeMeses());
    }

    public UUID getId() { return id; }
    public Valvula getValvula() { return valvula; }
    public LocalDate getDataRealizada() { return dataRealizada; }
    public String getTipoManutencao() { return tipoManutencao; }
    public Usuario getTecnicoResponsavel() { return tecnicoResponsavel; }
    public String getPecasSubstituidas() { return pecasSubstituidas; }
    public BigDecimal getPressaoEntrada() { return pressaoEntrada; }
    public BigDecimal getPressaoSaida() { return pressaoSaida; }
    public String getObservacoes() { return observacoes; }
    public String getStatus() { return status; }
}
