package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.bomba.Bomba;
import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
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
 * A intervenção realizada. Uma linha por EQUIPAMENTO atendido (D-18): válvula
 * ou bomba, nunca os dois (RF-80, ck_servico_equipamento).
 *
 * Nunca é sobrescrita: toda intervenção é uma linha nova (RF-18). A única
 * mudança possível é a inativação, com motivo (RN-26).
 */
@Entity
@Table(name = "servico")
public class Servico {

    /** Quando o Jiraya não manda executor nem quem concluiu, o serviço não pode ficar sem técnico. */
    static final String TECNICO_NAO_INFORMADO = "Não informado pelo Jiraya";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nula quando o serviço é de bomba. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valvula_id", updatable = false)
    private Valvula valvula;

    /** Nula quando o serviço é de válvula (RF-80). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bomba_id", updatable = false)
    private Bomba bomba;

    /** Informada pelo técnico, nunca assumida como a data de preenchimento (RF-17, D-02). */
    @Column(name = "data_realizada", nullable = false)
    private LocalDate dataRealizada;

    @Convert(converter = TipoManutencao.Conversor.class)
    @Column(name = "tipo_manutencao", nullable = false)
    private TipoManutencao tipoManutencao;

    /** Nulo quando o técnico veio do Jiraya e não é usuário do Valtis (P-29). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_responsavel_id")
    private Usuario tecnicoResponsavel;

    @Column(name = "tecnico_externo")
    private String tecnicoExterno;

    @Column(name = "pecas_substituidas")
    private String pecasSubstituidas;

    /** Opcional (D-10). */
    @Column(name = "pressao_entrada")
    private BigDecimal pressaoEntrada;

    @Column(name = "pressao_saida")
    private BigDecimal pressaoSaida;

    private String observacoes;

    @Convert(converter = StatusServico.Conversor.class)
    @Column(nullable = false)
    private StatusServico status = StatusServico.CONCLUIDO;

    /** Quem digitou. Pode ser diferente de quem executou (database.md). Nulo no serviço vindo do Jiraya. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    @Convert(converter = OrigemServico.Conversor.class)
    @Column(nullable = false, updatable = false)
    private OrigemServico origem = OrigemServico.MANUAL;

    /** OS do Jiraya que originou o serviço (RF-74). Uma OS vira um serviço por equipamento. */
    @Column(name = "jiraya_os_id", updatable = false)
    private Long jirayaOsId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "motivo_inativacao")
    private String motivoInativacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inativado_por_id")
    private Usuario inativadoPor;

    @Column(name = "inativado_em")
    private OffsetDateTime inativadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Servico() {
    }

    /** Lançamento manual de válvula (RF-14). */
    public Servico(Valvula valvula, LocalDate dataRealizada, TipoManutencao tipoManutencao,
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

    /** Lançamento manual de bomba (RF-80). Pressões são da VRP: não se aplicam. */
    public static Servico daBomba(Bomba bomba, LocalDate dataRealizada, TipoManutencao tipoManutencao,
                                  Usuario tecnicoResponsavel, String pecasSubstituidas, String observacoes,
                                  Usuario criadoPor) {
        Servico s = new Servico();
        s.bomba = bomba;
        s.dataRealizada = dataRealizada;
        s.tipoManutencao = tipoManutencao;
        s.tecnicoResponsavel = tecnicoResponsavel;
        s.pecasSubstituidas = pecasSubstituidas;
        s.observacoes = observacoes;
        s.criadoPor = criadoPor;
        return s;
    }

    /**
     * Serviço nascido de OS concluída no Jiraya (RF-74). Sem técnico do Valtis nem
     * quem digitou: o técnico vem como texto (P-29).
     */
    public static Servico daOrdemDeServico(Valvula valvula, Bomba bomba, long jirayaOsId, LocalDate dataExecucao,
                                           TipoManutencao tipoManutencao, String tecnicoExterno,
                                           String observacoes) {
        if ((valvula == null) == (bomba == null)) {
            throw new IllegalArgumentException("Serviço é de uma válvula ou de uma bomba, nunca dos dois.");
        }
        Servico s = new Servico();
        s.valvula = valvula;
        s.bomba = bomba;
        s.origem = OrigemServico.JIRAYA;
        s.jirayaOsId = jirayaOsId;
        s.dataRealizada = dataExecucao;
        s.tipoManutencao = tipoManutencao;
        s.tecnicoExterno = tecnicoExterno == null || tecnicoExterno.isBlank() ? TECNICO_NAO_INFORMADO : tecnicoExterno;
        s.observacoes = observacoes;
        return s;
    }

    /** RN-26: sai do histórico ativo e do cálculo do ciclo. Nunca é apagado (RN-11). */
    public void inativar(Usuario quem, String motivo, OffsetDateTime agora) {
        if (!ativo) {
            throw new RegraDeNegocioException("Este serviço já está inativo.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("Informe o motivo da inativação.");
        }
        this.ativo = false;
        this.motivoInativacao = motivo;
        this.inativadoPor = quem;
        this.inativadoEm = agora;
    }

    public boolean isDaBomba() {
        return bomba != null;
    }

    /** O dono do equipamento atendido: quem recebe a oportunidade (RN-07). Nulo para bomba patrimonial. */
    public Cliente getCliente() {
        return isDaBomba() ? bomba.getCliente() : valvula.getEstacao().getCliente();
    }

    public int getPeriodicidadeMeses() {
        return isDaBomba() ? bomba.getPeriodicidadeMeses() : valvula.getPeriodicidadeMeses();
    }

    /** Nome do técnico para telas e agenda: o usuário do Valtis ou o texto do Jiraya. */
    public String nomeDoTecnico() {
        return tecnicoResponsavel != null ? tecnicoResponsavel.getNome() : tecnicoExterno;
    }

    /** Quem lançou: o usuário, ou "Jiraya" quando veio da OS (RF-76). */
    public String nomeDeQuemLancou() {
        return criadoPor != null ? criadoPor.getNome() : "Jiraya";
    }

    public UUID getId() { return id; }
    public Valvula getValvula() { return valvula; }
    public Bomba getBomba() { return bomba; }
    public LocalDate getDataRealizada() { return dataRealizada; }
    public TipoManutencao getTipoManutencao() { return tipoManutencao; }
    public Usuario getTecnicoResponsavel() { return tecnicoResponsavel; }
    public String getTecnicoExterno() { return tecnicoExterno; }
    public String getPecasSubstituidas() { return pecasSubstituidas; }
    public BigDecimal getPressaoEntrada() { return pressaoEntrada; }
    public BigDecimal getPressaoSaida() { return pressaoSaida; }
    public String getObservacoes() { return observacoes; }
    public StatusServico getStatus() { return status; }
    public Usuario getCriadoPor() { return criadoPor; }
    public OrigemServico getOrigem() { return origem; }
    public Long getJirayaOsId() { return jirayaOsId; }
    public boolean isAtivo() { return ativo; }
    public String getMotivoInativacao() { return motivoInativacao; }
    public OffsetDateTime getInativadoEm() { return inativadoEm; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
