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
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "localizacao_instalacao", nullable = false)
    private String localizacaoInstalacao;

    /** Térreo = 0. Subsolo = negativo. Nulos só na estação derivada do Jiraya, até alguém completar (Q-2). */
    @Column(name = "andar_inicial")
    private Integer andarInicial;

    @Column(name = "andar_final")
    private Integer andarFinal;

    /**
     * Estação derivada do Jiraya (RF-73): a localização interna normalizada. Uma
     * estação por (cliente, chave). Nula na estação cadastrada à mão.
     */
    @Column(name = "chave_integracao", updatable = false)
    private String chaveIntegracao;

    /** Se o serviço exige cortar a água do prédio inteiro. Vai no evento da agenda (RF-29). */
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
        atualizar(localizacaoInstalacao, andarInicial, andarFinal, requerFechamentoGeral);
    }

    /** RF-73: a estação que agrupa as VRPs de uma localização interna do Jiraya. Sem andares (Q-2). */
    public static Estacao derivadaDoJiraya(Cliente cliente, String localizacao, String chaveIntegracao) {
        Estacao e = new Estacao();
        e.cliente = cliente;
        e.localizacaoInstalacao = localizacao;
        e.chaveIntegracao = chaveIntegracao;
        return e;
    }

    /**
     * RN-24 por campo: na estação derivada a localização vem do Jiraya; andares e
     * fechamento geral são do Valtis e se completam aqui.
     */
    public void completar(Integer andarInicial, Integer andarFinal, boolean requerFechamentoGeral) {
        this.andarInicial = andarInicial;
        this.andarFinal = andarFinal;
        this.requerFechamentoGeral = requerFechamentoGeral;
    }

    public boolean isDerivada() {
        return chaveIntegracao != null;
    }

    public boolean isACompletar() {
        return andarInicial == null || andarFinal == null;
    }

    public void atualizar(String localizacaoInstalacao, Integer andarInicial, Integer andarFinal,
                          boolean requerFechamentoGeral) {
        this.localizacaoInstalacao = localizacaoInstalacao;
        this.andarInicial = andarInicial;
        this.andarFinal = andarFinal;
        this.requerFechamentoGeral = requerFechamentoGeral;
    }

    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    public String descreverAndares() {
        return Andares.descrever(andarInicial, andarFinal);
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getLocalizacaoInstalacao() { return localizacaoInstalacao; }
    public Integer getAndarInicial() { return andarInicial; }
    public Integer getAndarFinal() { return andarFinal; }
    public boolean isRequerFechamentoGeral() { return requerFechamentoGeral; }
    public boolean isAtivo() { return ativo; }
}
