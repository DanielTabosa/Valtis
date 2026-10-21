package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.usuario.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um período em que uma bomba ocupou uma posição de um conjunto (D-27).
 *
 * É histórico: nasce, um dia se encerra, e nunca se reescreve (RN-20).
 * Enquanto `fim` é nulo, a ocupação está aberta e diz onde a bomba está.
 */
@Entity
@Table(name = "ocupacao_bomba")
public class OcupacaoBomba {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bomba_id", nullable = false, updatable = false)
    private Bomba bomba;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conjunto_id", nullable = false, updatable = false)
    private ConjuntoBombeamento conjunto;

    @Column(name = "numero_posicao", nullable = false, updatable = false)
    private String numeroPosicao;

    @Convert(converter = NaturezaOcupacao.Conversor.class)
    @Column(nullable = false, updatable = false)
    private NaturezaOcupacao natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bomba_substituida_id", updatable = false)
    private Bomba bombaSubstituida;

    @Column(nullable = false, updatable = false)
    private LocalDate inicio;

    private LocalDate fim;

    @Convert(converter = MotivoEncerramento.Conversor.class)
    @Column(name = "motivo_encerramento")
    private MotivoEncerramento motivoEncerramento;

    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false, updatable = false)
    private Usuario registradoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected OcupacaoBomba() {
    }

    public OcupacaoBomba(Bomba bomba, ConjuntoBombeamento conjunto, String numeroPosicao, LocalDate inicio,
                         Bomba bombaSubstituida, String observacoes, Usuario registradoPor) {
        this.bomba = bomba;
        this.conjunto = conjunto;
        this.numeroPosicao = numeroPosicao;
        // A natureza vem da bomba, não de quem registra: patrimônio da Manutec só entra emprestado.
        this.natureza = bomba.isPatrimonioManutec() ? NaturezaOcupacao.EMPRESTIMO : NaturezaOcupacao.INSTALACAO;
        this.inicio = inicio;
        this.bombaSubstituida = bombaSubstituida;
        this.observacoes = observacoes;
        this.registradoPor = registradoPor;
    }

    /**
     * RN-18 e RN-20: encerra uma vez só, com data igual ou posterior ao início.
     * O motivo é consequência da natureza (devolução para empréstimo, retirada para instalação).
     */
    public void encerrar(LocalDate fim, String observacoes) {
        if (!isAberta()) {
            throw new RegraDeNegocioException("Esta ocupação já foi encerrada em %s e não pode ser reescrita."
                    .formatted(this.fim));
        }
        if (fim.isBefore(inicio)) {
            throw new RegraDeNegocioException("A data de saída não pode ser anterior à entrada (%s).".formatted(inicio));
        }
        this.fim = fim;
        this.motivoEncerramento = natureza.encerramento();
        if (observacoes != null) {
            this.observacoes = this.observacoes == null ? observacoes : this.observacoes + "\n" + observacoes;
        }
    }

    public boolean isAberta() { return fim == null; }

    public UUID getId() { return id; }
    public Bomba getBomba() { return bomba; }
    public ConjuntoBombeamento getConjunto() { return conjunto; }
    public String getNumeroPosicao() { return numeroPosicao; }
    public NaturezaOcupacao getNatureza() { return natureza; }
    public Bomba getBombaSubstituida() { return bombaSubstituida; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFim() { return fim; }
    public MotivoEncerramento getMotivoEncerramento() { return motivoEncerramento; }
    public String getObservacoes() { return observacoes; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
