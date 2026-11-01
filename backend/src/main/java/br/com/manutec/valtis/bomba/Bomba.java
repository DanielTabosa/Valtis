package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O equipamento físico, com identidade permanente (RN-15).
 *
 * Não sabe onde está: isso é a ocupação aberta (D-27). Sabe de quem é
 * (condomínio ou patrimônio da Manutec) e o que é (dados técnicos).
 */
@Entity
@Table(name = "bomba")
public class Bomba {

    /** Dados técnicos que podem ficar para depois (RF-63). */
    public record DadosTecnicos(String fabricante, String modelo, BigDecimal potenciaCv, Integer tensaoVolts,
                                BigDecimal correnteAmperes, String funcao, LocalDate dataInstalacao) {

        public boolean incompletos() {
            return fabricante == null || modelo == null || potenciaCv == null || tensaoVolts == null
                    || correnteAmperes == null;
        }
    }

    /** P-22: 12 meses, como a VRP, editável por bomba. */
    public static final int PERIODICIDADE_PADRAO_MESES = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_referencia", nullable = false, unique = true, updatable = false)
    private String codigoReferencia;

    @Column(name = "patrimonio_manutec", nullable = false, updatable = false)
    private boolean patrimonioManutec;

    /** Dono, quando é do condomínio. Nulo se e só se patrimônio (ck_bomba_dono). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", updatable = false)
    private Cliente cliente;

    private String fabricante;
    private String modelo;

    @Column(name = "potencia_cv")
    private BigDecimal potenciaCv;

    @Column(name = "tensao_volts")
    private Integer tensaoVolts;

    @Column(name = "corrente_amperes")
    private BigDecimal correnteAmperes;

    private String funcao;

    @Column(name = "data_instalacao")
    private LocalDate dataInstalacao;

    /** Padrão 12 meses, editável por bomba (P-22). */
    @Column(name = "periodicidade_meses", nullable = false)
    private Integer periodicidadeMeses;

    /** Informada no cadastro (P-16). A próxima é calculada (RN-16), nunca gravada. */
    @Column(name = "ultima_manutencao")
    private LocalDate ultimaManutencao;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Equipamento do Jiraya que esta bomba espelha (RF-72). Nulo no cadastro próprio. */
    @Column(name = "jiraya_equipamento_id", updatable = false)
    private Long jirayaEquipamentoId;

    @Column(name = "fonte_atualizado_em")
    private String fonteAtualizadoEm;

    @Column(name = "sincronizado_em")
    private OffsetDateTime sincronizadoEm;

    /** A identificação da equipe no Jiraya (BR-01). Só leitura (RN-24). */
    @Column(name = "identificacao_externa")
    private String identificacaoExterna;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Bomba() {
    }

    private Bomba(String codigoReferencia, boolean patrimonioManutec, Cliente cliente, DadosTecnicos tecnicos,
                  Integer periodicidadeMeses, LocalDate ultimaManutencao) {
        this.codigoReferencia = codigoReferencia;
        this.patrimonioManutec = patrimonioManutec;
        this.cliente = cliente;
        atualizar(tecnicos, periodicidadeMeses, ultimaManutencao);
    }

    public static Bomba doCondominio(Cliente cliente, String codigoReferencia, DadosTecnicos tecnicos,
                                     Integer periodicidadeMeses, LocalDate ultimaManutencao) {
        return new Bomba(codigoReferencia, false, cliente, tecnicos, periodicidadeMeses, ultimaManutencao);
    }

    public static Bomba patrimonial(String codigoReferencia, DadosTecnicos tecnicos, Integer periodicidadeMeses,
                                    LocalDate ultimaManutencao) {
        return new Bomba(codigoReferencia, true, null, tecnicos, periodicidadeMeses, ultimaManutencao);
    }

    /** O que o Jiraya manda para a bomba. Só leitura no Valtis (RN-24). */
    public record DadosDoJiraya(String fabricante, String modelo, String identificacao, String versao) {}

    /** RF-72: a bomba que nasce do equipamento do Jiraya. Periodicidade padrão 12 (P-22). */
    public static Bomba espelhoDoJiraya(Cliente cliente, String codigoReferencia, long jirayaEquipamentoId,
                                        DadosDoJiraya dados, int periodicidadeMeses, OffsetDateTime agora) {
        Bomba b = new Bomba(codigoReferencia, false, cliente,
                new DadosTecnicos(null, null, null, null, null, null, null), periodicidadeMeses, null);
        b.jirayaEquipamentoId = jirayaEquipamentoId;
        b.atualizarDoJiraya(dados, agora);
        return b;
    }

    /** Sobrescreve só fabricante, modelo e identificação. O resto é do Valtis (RN-24 por campo). */
    public void atualizarDoJiraya(DadosDoJiraya dados, OffsetDateTime agora) {
        this.fabricante = dados.fabricante();
        this.modelo = dados.modelo();
        this.identificacaoExterna = dados.identificacao();
        this.fonteAtualizadoEm = dados.versao();
        this.sincronizadoEm = agora;
    }

    /** Campos do Valtis na bomba espelhada: o que o Jiraya não tem estruturado (RN-24 por campo). */
    public void completar(DadosTecnicos tecnicos, Integer periodicidadeMeses, LocalDate ultimaManutencao) {
        atualizar(new DadosTecnicos(fabricante, modelo, tecnicos.potenciaCv(), tecnicos.tensaoVolts(),
                tecnicos.correnteAmperes(), tecnicos.funcao(), tecnicos.dataInstalacao()),
                periodicidadeMeses, ultimaManutencao);
    }

    public boolean isEspelhada() {
        return jirayaEquipamentoId != null;
    }

    public void atualizar(DadosTecnicos tecnicos, Integer periodicidadeMeses, LocalDate ultimaManutencao) {
        this.fabricante = tecnicos.fabricante();
        this.modelo = tecnicos.modelo();
        this.potenciaCv = tecnicos.potenciaCv();
        this.tensaoVolts = tecnicos.tensaoVolts();
        this.correnteAmperes = tecnicos.correnteAmperes();
        this.funcao = tecnicos.funcao();
        this.dataInstalacao = tecnicos.dataInstalacao();
        this.periodicidadeMeses = periodicidadeMeses;
        this.ultimaManutencao = ultimaManutencao;
    }

    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    public DadosTecnicos dadosTecnicos() {
        return new DadosTecnicos(fabricante, modelo, potenciaCv, tensaoVolts, correnteAmperes, funcao, dataInstalacao);
    }

    /** Ex.: "WEG BC-92 · 3 CV". Para listas; cada parte é opcional (RF-63). */
    public String descricao() {
        StringBuilder texto = new StringBuilder();
        if (fabricante != null) texto.append(fabricante);
        if (modelo != null) texto.append(texto.isEmpty() ? "" : " ").append(modelo);
        if (potenciaCv != null) texto.append(texto.isEmpty() ? "" : " · ").append(potenciaCv.stripTrailingZeros().toPlainString()).append(" CV");
        return texto.isEmpty() ? "sem dados técnicos" : texto.toString();
    }

    public UUID getId() { return id; }
    public String getCodigoReferencia() { return codigoReferencia; }
    public boolean isPatrimonioManutec() { return patrimonioManutec; }
    public Cliente getCliente() { return cliente; }
    public String getFabricante() { return fabricante; }
    public String getModelo() { return modelo; }
    public BigDecimal getPotenciaCv() { return potenciaCv; }
    public Integer getTensaoVolts() { return tensaoVolts; }
    public BigDecimal getCorrenteAmperes() { return correnteAmperes; }
    public String getFuncao() { return funcao; }
    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public Integer getPeriodicidadeMeses() { return periodicidadeMeses; }
    public LocalDate getUltimaManutencao() { return ultimaManutencao; }
    public boolean isAtivo() { return ativo; }
    public Long getJirayaEquipamentoId() { return jirayaEquipamentoId; }
    public String getFonteAtualizadoEm() { return fonteAtualizadoEm; }
    public String getIdentificacaoExterna() { return identificacaoExterna; }
}
