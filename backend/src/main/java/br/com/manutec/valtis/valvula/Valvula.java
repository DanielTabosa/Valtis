package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.especificacao.Especificacao;
import br.com.manutec.valtis.estacao.Estacao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

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

    /** Opcional só na válvula espelhada do Jiraya (Q-1, ck_valvula_especificacao). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especificacao_id")
    private Especificacao especificacao;

    /** Equipamento do Jiraya que esta válvula espelha (RF-72). Nulo no cadastro próprio. */
    @Column(name = "jiraya_equipamento_id", updatable = false)
    private Long jirayaEquipamentoId;

    /** atualizado_em do Jiraya, texto exato como recebido. */
    @Column(name = "fonte_atualizado_em")
    private String fonteAtualizadoEm;

    @Column(name = "sincronizado_em")
    private OffsetDateTime sincronizadoEm;

    /** Vêm do Jiraya, em texto livre (Q-1). Só leitura no Valtis (RN-24). */
    private String fabricante;
    private String modelo;

    @Column(name = "identificacao_externa")
    private String identificacaoExterna;

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

    /** Dados que o Jiraya manda para a válvula. Só leitura no Valtis (RN-24). */
    public record DadosDoJiraya(String fabricante, String modelo, String numeroSerie, String identificacao,
                                String versao) {}

    /** RF-72: a VRP que nasce do equipamento do Jiraya. Especificação fica a completar (Q-1). */
    public static Valvula espelhoDoJiraya(Estacao estacao, String codigoReferencia, String numeroValvula,
                                          long jirayaEquipamentoId, DadosDoJiraya dados, OffsetDateTime agora) {
        Valvula v = new Valvula();
        v.estacao = estacao;
        v.codigoReferencia = codigoReferencia;
        v.numeroValvula = numeroValvula;
        v.jirayaEquipamentoId = jirayaEquipamentoId;
        v.atualizarDoJiraya(dados, agora);
        return v;
    }

    /**
     * Sobrescreve só o que vem da fonte. Especificação, periodicidade e data de
     * instalação são do Valtis e nunca são tocadas aqui (RN-24 por campo).
     */
    public void atualizarDoJiraya(DadosDoJiraya dados, OffsetDateTime agora) {
        this.fabricante = dados.fabricante();
        this.modelo = dados.modelo();
        this.numeroSerie = dados.numeroSerie();
        this.identificacaoExterna = dados.identificacao();
        this.fonteAtualizadoEm = dados.versao();
        this.sincronizadoEm = agora;
    }

    /** Campos do Valtis na válvula espelhada (RN-24 por campo). */
    public void completar(Especificacao especificacao, LocalDate dataInstalacao, Integer periodicidadeMeses) {
        this.especificacao = especificacao;
        this.dataInstalacao = dataInstalacao;
        this.periodicidadeMeses = periodicidadeMeses != null ? periodicidadeMeses : PERIODICIDADE_PADRAO_MESES;
    }

    public boolean isEspelhada() {
        return jirayaEquipamentoId != null;
    }

    /** O que a lista mostra: a especificação do catálogo ou, na espelhada, o que o Jiraya sabe (Q-1). */
    public String descricaoTecnica() {
        if (especificacao != null) {
            return especificacao.getDescricaoCompleta();
        }
        String texto = String.join(" ", Stream.of(fabricante, modelo).filter(Objects::nonNull).toList());
        return texto.isBlank() ? "especificação a completar" : texto;
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
    public Long getJirayaEquipamentoId() { return jirayaEquipamentoId; }
    public String getFonteAtualizadoEm() { return fonteAtualizadoEm; }
    public String getFabricante() { return fabricante; }
    public String getModelo() { return modelo; }
    public String getIdentificacaoExterna() { return identificacaoExterna; }
    public String getCodigoReferencia() { return codigoReferencia; }
    public String getNumeroValvula() { return numeroValvula; }
    public String getNumeroSerie() { return numeroSerie; }
    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public Integer getPeriodicidadeMeses() { return periodicidadeMeses; }
    public boolean isAtivo() { return ativo; }
}
