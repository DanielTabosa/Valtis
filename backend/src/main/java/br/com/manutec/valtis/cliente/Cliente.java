package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.OrigemRegistro;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O condomínio atendido. Cliente e prédio são a mesma coisa neste domínio (D-17).
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** COND-001. Gerado pelo sistema, nunca muda (RF-09). */
    @Column(name = "codigo_referencia", nullable = false, unique = true, updatable = false)
    private String codigoReferencia;

    @Column(name = "nome_condominio", nullable = false)
    private String nomeCondominio;

    private String endereco;
    private String bairro;
    private String cidade;

    // Dados pessoais (LGPD, RNF-22): nunca em log, URL ou mensagem de erro.
    @Column(name = "sindico_responsavel")
    private String sindicoResponsavel;

    @Column(name = "telefone_contato")
    private String telefoneContato;

    @Column(name = "email_contato")
    private String emailContato;

    @Convert(converter = TipoAtendimento.Conversor.class)
    @Column(name = "tipo_atendimento", nullable = false)
    private TipoAtendimento tipoAtendimento = TipoAtendimento.AVULSO;

    @Convert(converter = StatusContrato.Conversor.class)
    @Column(name = "status_contrato", nullable = false)
    private StatusContrato statusContrato = StatusContrato.ATIVO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Convert(converter = OrigemRegistro.Conversor.class)
    @Column(nullable = false)
    private OrigemRegistro origem = OrigemRegistro.SISTEMA;

    @Column(name = "ultima_manutencao_legado")
    private LocalDate ultimaManutencaoLegado;

    @Column(name = "data_legado_aproximada", nullable = false)
    private boolean dataLegadoAproximada = false;

    @Column(name = "cadastro_incompleto", nullable = false)
    private boolean cadastroIncompleto = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Cliente() {
    }

    public Cliente(String codigoReferencia, DadosCliente dados) {
        this.codigoReferencia = codigoReferencia;
        atualizar(dados);
    }

    /**
     * RF-51, RF-53, RF-58: cliente da base antiga. Entra sem estação e sem válvula,
     * marcado como incompleto, com a data da última manutenção sempre aproximada
     * (vem da nota fiscal, não do registro do serviço).
     */
    public static Cliente importado(String codigoReferencia, DadosCliente dados, LocalDate ultimaManutencao) {
        Cliente cliente = new Cliente(codigoReferencia, dados);
        cliente.origem = OrigemRegistro.IMPORTACAO;
        cliente.cadastroIncompleto = true;
        cliente.ultimaManutencaoLegado = ultimaManutencao;
        cliente.dataLegadoAproximada = ultimaManutencao != null;
        return cliente;
    }

    public void atualizar(DadosCliente dados) {
        this.nomeCondominio = dados.nomeCondominio();
        this.endereco = dados.endereco();
        this.bairro = dados.bairro();
        this.cidade = dados.cidade();
        this.sindicoResponsavel = dados.sindicoResponsavel();
        this.telefoneContato = dados.telefoneContato();
        this.emailContato = dados.emailContato();
        if (dados.tipoAtendimento() != null) {
            this.tipoAtendimento = dados.tipoAtendimento();
        }
        if (dados.statusContrato() != null) {
            this.statusContrato = dados.statusContrato();
        }
    }

    /** Inativação lógica: nada é excluído fisicamente (RN-11). */
    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }

    /** RF-59: a primeira manutenção real de um cliente importado completa o cadastro. */
    public void completarCadastro() { this.cadastroIncompleto = false; }

    public UUID getId() { return id; }
    public String getCodigoReferencia() { return codigoReferencia; }
    public String getNomeCondominio() { return nomeCondominio; }
    public String getEndereco() { return endereco; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getSindicoResponsavel() { return sindicoResponsavel; }
    public String getTelefoneContato() { return telefoneContato; }
    public String getEmailContato() { return emailContato; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public StatusContrato getStatusContrato() { return statusContrato; }
    public boolean isAtivo() { return ativo; }
    public OrigemRegistro getOrigem() { return origem; }
    public LocalDate getUltimaManutencaoLegado() { return ultimaManutencaoLegado; }
    public boolean isDataLegadoAproximada() { return dataLegadoAproximada; }
    public boolean isCadastroIncompleto() { return cadastroIncompleto; }
}
