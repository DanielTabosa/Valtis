package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.OrigemRegistro;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.valvula.Valvula;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O retorno previsto e o seu desfecho comercial (módulos M5 e M10).
 *
 * É a entidade que transforma "dinheiro na mesa" em número: cada manutenção
 * concluída gera uma, e o desfecho dela diz se o cliente voltou.
 */
@Entity
@Table(name = "oportunidade")
public class Oportunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Convert(converter = EscopoOportunidade.Conversor.class)
    @Column(nullable = false, updatable = false)
    private EscopoOportunidade escopo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    /** Nulo quando o escopo é CLIENTE (importados, D-09). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valvula_id", updatable = false)
    private Valvula valvula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_origem_id", updatable = false)
    private Servico servicoOrigem;

    /** RN-01: data realizada + periodicidade. Os alertas derivam dela (RN-06). */
    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Convert(converter = StatusOportunidade.Conversor.class)
    @Column(nullable = false)
    private StatusOportunidade status = StatusOportunidade.AGENDADO;

    /** Nulo até alguém assumir (RN-07, D-04). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "assumida_em")
    private OffsetDateTime assumidaEm;

    @Column(name = "motivo_recusa")
    private String motivoRecusa;

    @Convert(converter = OrigemRegistro.Conversor.class)
    @Column(nullable = false, updatable = false)
    private OrigemRegistro origem = OrigemRegistro.SISTEMA;

    private String notas;

    /** O serviço que fechou este ciclo (RN-10). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_resultante_id")
    private Servico servicoResultante;

    /** A oportunidade recusada que gerou esta, quando o cliente pediu para lembrar depois (RN-09). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidade_anterior_id", updatable = false)
    private Oportunidade oportunidadeAnterior;

    @Column(name = "encerrada_em")
    private OffsetDateTime encerradaEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Oportunidade() {
    }

    /** RN-07: nasce de um serviço concluído, sem responsável. */
    public static Oportunidade doServico(Servico servico, LocalDate dataPrevista) {
        Oportunidade o = new Oportunidade();
        o.escopo = EscopoOportunidade.VALVULA;
        o.valvula = servico.getValvula();
        o.cliente = servico.getValvula().getEstacao().getCliente();
        o.servicoOrigem = servico;
        o.dataPrevista = dataPrevista;
        return o;
    }

    /** RN-09: o cliente recusou, mas pediu para ser lembrado numa data. Nova oportunidade, mesmo alvo. */
    public static Oportunidade retornoDe(Oportunidade recusada, LocalDate lembrarEm) {
        Oportunidade o = new Oportunidade();
        o.escopo = recusada.escopo;
        o.valvula = recusada.valvula;
        o.cliente = recusada.cliente;
        o.dataPrevista = lembrarEm;
        o.oportunidadeAnterior = recusada;
        return o;
    }

    /** RN-08: qualquer usuário assume; o nome fica visível. */
    public void assumir(Usuario usuario, OffsetDateTime agora) {
        exigirAberta();
        if (responsavel != null && !responsavel.getId().equals(usuario.getId())) {
            throw new ConflitoException("Esta oportunidade já foi assumida por " + responsavel.getNome() + ".");
        }
        this.responsavel = usuario;
        this.assumidaEm = agora;
    }

    /** Devolve a oportunidade à fila. Só quem assumiu ou um administrador. */
    public void liberar(Usuario usuario) {
        exigirAberta();
        if (responsavel == null) {
            return;
        }
        if (!responsavel.getId().equals(usuario.getId()) && !usuario.isAdmin()) {
            throw new RegraDeNegocioException("Só quem assumiu, ou um administrador, pode devolver à fila.");
        }
        this.responsavel = null;
        this.assumidaEm = null;
    }

    /**
     * RF-41: contatado, aceito, recusado ou perdido. EXECUTADO não entra aqui:
     * só um serviço lançado fecha o ciclo como executado (RN-10).
     */
    public void registrarDesfecho(StatusOportunidade novo, String notas, String motivoRecusa, OffsetDateTime agora) {
        exigirAberta();
        if (novo == StatusOportunidade.EXECUTADO || novo == StatusOportunidade.AGENDADO) {
            throw new RegraDeNegocioException("Esse desfecho não pode ser marcado à mão.");
        }
        this.status = novo;
        if (notas != null) {
            this.notas = notas;
        }
        this.motivoRecusa = novo == StatusOportunidade.RECUSADO ? motivoRecusa : null;
        if (!novo.aberto()) {
            this.encerradaEm = agora;
        }
    }

    /** RN-10: um novo serviço na válvula fecha o ciclo anterior como executado. */
    public void fecharComServico(Servico servico, OffsetDateTime agora) {
        this.status = StatusOportunidade.EXECUTADO;
        this.servicoResultante = servico;
        this.encerradaEm = agora;
    }

    /** A periodicidade da válvula mudou: a data prevista acompanha (RN-01). */
    public void reprogramar(LocalDate novaDataPrevista) {
        exigirAberta();
        this.dataPrevista = novaDataPrevista;
    }

    public boolean isAberta() {
        return status.aberto();
    }

    private void exigirAberta() {
        if (!isAberta()) {
            throw new RegraDeNegocioException("Esta oportunidade já está encerrada.");
        }
    }

    public UUID getId() { return id; }
    public EscopoOportunidade getEscopo() { return escopo; }
    public Cliente getCliente() { return cliente; }
    public Valvula getValvula() { return valvula; }
    public Servico getServicoOrigem() { return servicoOrigem; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public StatusOportunidade getStatus() { return status; }
    public Usuario getResponsavel() { return responsavel; }
    public OffsetDateTime getAssumidaEm() { return assumidaEm; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public OrigemRegistro getOrigem() { return origem; }
    public String getNotas() { return notas; }
    public Servico getServicoResultante() { return servicoResultante; }
    public Oportunidade getOportunidadeAnterior() { return oportunidadeAnterior; }
    public OffsetDateTime getEncerradaEm() { return encerradaEm; }
}
