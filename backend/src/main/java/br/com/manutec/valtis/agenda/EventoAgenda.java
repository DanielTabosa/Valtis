package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um evento a criar no Google Calendar: a linha da tabela de saída (Outbox, D-13).
 *
 * É gravado na MESMA transação que o serviço e a oportunidade. Se o Google
 * estiver fora do ar, nada se perde: a linha fica PENDENTE e o worker tenta
 * depois. A fonte da verdade é o banco; a agenda é só o aviso.
 */
@Entity
@Table(name = "evento_agenda")
public class EventoAgenda {

    /** Teto da espera entre tentativas: não adianta esperar mais que isso por um alerta de agenda. */
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(6);
    private static final int TAMANHO_MAXIMO_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidade_id", nullable = false, updatable = false)
    private Oportunidade oportunidade;

    @Convert(converter = TipoEvento.Conversor.class)
    @Column(nullable = false, updatable = false)
    private TipoEvento tipo;

    @Column(name = "data_evento", nullable = false)
    private LocalDate dataEvento;

    @Convert(converter = StatusEvento.Conversor.class)
    @Column(nullable = false)
    private StatusEvento status = StatusEvento.PENDENTE;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private OffsetDateTime proximaTentativaEm;

    /** Mensagem técnica da última falha. Nunca leva dado pessoal (AGENTS.md §8). */
    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @Column(name = "sincronizado_em")
    private OffsetDateTime sincronizadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected EventoAgenda() {
    }

    public EventoAgenda(Oportunidade oportunidade, TipoEvento tipo, LocalDate dataEvento, OffsetDateTime agora) {
        this.oportunidade = oportunidade;
        this.tipo = tipo;
        this.dataEvento = dataEvento;
        this.proximaTentativaEm = agora;
    }

    /**
     * O id que o Google vai usar para o evento, decidido POR NÓS antes da chamada.
     *
     * É isso que torna o worker idempotente: se o Google criar o evento e a
     * aplicação cair antes de gravar a resposta, a próxima tentativa manda o
     * mesmo id, o Google responde "já existe" e nada é duplicado.
     * O Google aceita de 5 a 1024 caracteres entre a-v e 0-9; o UUID em hexadecimal serve.
     */
    public String idExterno() {
        return "valtis" + id.toString().replace("-", "");
    }

    public void marcarCriado(String googleEventId, OffsetDateTime agora) {
        this.status = StatusEvento.CRIADO;
        this.googleEventId = googleEventId;
        this.sincronizadoEm = agora;
        this.ultimoErro = null;
    }

    /**
     * Retentativa com espera crescente: 1x, 2x, 4x, 8x a espera inicial, até o teto.
     * Esgotado o limite, para e fica visível como FALHOU: falha silenciosa
     * infinita é pior que falha visível (architecture.md §3).
     */
    public void registrarFalha(String erro, OffsetDateTime agora, int maxTentativas, long esperaInicialSegundos) {
        this.tentativas++;
        this.ultimoErro = resumir(erro);
        if (tentativas >= maxTentativas) {
            this.status = StatusEvento.FALHOU;
            return;
        }
        Duration espera = Duration.ofSeconds(esperaInicialSegundos).multipliedBy(1L << Math.min(tentativas - 1, 20));
        this.proximaTentativaEm = agora.plus(espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera);
    }

    public void descartar(String motivo) {
        if (status == StatusEvento.CRIADO) {
            return;
        }
        this.status = StatusEvento.DESCARTADO;
        this.ultimoErro = motivo;
    }

    /** Volta para a fila um evento que esgotou as tentativas (RF-33). */
    public void reprocessar(OffsetDateTime agora) {
        if (status != StatusEvento.FALHOU) {
            throw new RegraDeNegocioException("Só eventos que falharam podem ser reprocessados.");
        }
        this.status = StatusEvento.PENDENTE;
        this.tentativas = 0;
        this.proximaTentativaEm = agora;
        this.ultimoErro = null;
    }

    /** A data prevista mudou antes do evento chegar ao Google: ajusta a data e tenta de novo. */
    public void reprogramar(LocalDate novaData, OffsetDateTime agora) {
        if (status == StatusEvento.CRIADO || status == StatusEvento.DESCARTADO) {
            return;
        }
        this.dataEvento = novaData;
        this.status = StatusEvento.PENDENTE;
        this.tentativas = 0;
        this.proximaTentativaEm = agora;
    }

    public boolean isPendente() {
        return status == StatusEvento.PENDENTE;
    }

    private static String resumir(String erro) {
        if (erro == null) {
            return null;
        }
        return erro.length() <= TAMANHO_MAXIMO_ERRO ? erro : erro.substring(0, TAMANHO_MAXIMO_ERRO);
    }

    public UUID getId() { return id; }
    public Oportunidade getOportunidade() { return oportunidade; }
    public TipoEvento getTipo() { return tipo; }
    public LocalDate getDataEvento() { return dataEvento; }
    public StatusEvento getStatus() { return status; }
    public String getGoogleEventId() { return googleEventId; }
    public int getTentativas() { return tentativas; }
    public OffsetDateTime getProximaTentativaEm() { return proximaTentativaEm; }
    public String getUltimoErro() { return ultimoErro; }
    public OffsetDateTime getSincronizadoEm() { return sincronizadoEm; }
}
