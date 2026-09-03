package br.com.manutec.valtis.cliente;

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

    @Column(name = "codigo_referencia", nullable = false, unique = true)
    private String codigoReferencia;

    @Column(name = "nome_condominio", nullable = false)
    private String nomeCondominio;

    private String endereco;
    private String bairro;
    private String cidade;

    @Column(name = "sindico_responsavel")
    private String sindicoResponsavel;

    @Column(name = "telefone_contato")
    private String telefoneContato;

    @Column(name = "email_contato")
    private String emailContato;

    @Column(name = "tipo_atendimento", nullable = false)
    private String tipoAtendimento = "Avulso";

    @Column(name = "status_contrato", nullable = false)
    private String statusContrato = "Ativo";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private String origem = "sistema";

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

    public Cliente(String codigoReferencia, String nomeCondominio, String endereco, String bairro,
                   String cidade, String sindicoResponsavel, String telefoneContato,
                   String emailContato, String tipoAtendimento) {
        this.codigoReferencia = codigoReferencia;
        this.nomeCondominio = nomeCondominio;
        this.endereco = endereco;
        this.bairro = bairro;
        this.cidade = cidade;
        this.sindicoResponsavel = sindicoResponsavel;
        this.telefoneContato = telefoneContato;
        this.emailContato = emailContato;
        if (tipoAtendimento != null) {
            this.tipoAtendimento = tipoAtendimento;
        }
    }

    public UUID getId() { return id; }
    public String getCodigoReferencia() { return codigoReferencia; }
    public String getNomeCondominio() { return nomeCondominio; }
    public String getEndereco() { return endereco; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getSindicoResponsavel() { return sindicoResponsavel; }
    public String getTelefoneContato() { return telefoneContato; }
    public String getEmailContato() { return emailContato; }
    public String getTipoAtendimento() { return tipoAtendimento; }
    public String getStatusContrato() { return statusContrato; }
    public boolean isAtivo() { return ativo; }

    /** Inativação lógica — nada é excluído fisicamente (RN-11). */
    public void inativar() { this.ativo = false; }
}
