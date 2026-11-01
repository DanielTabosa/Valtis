package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.usuario.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um item lido do Manutec Clientes ou do Jiraya, gravado ANTES de ser aplicado
 * (D-30). É o Outbox da agenda ao contrário: mesma retentativa, mesmo erro visível.
 *
 * A chave (origem, tipo, id externo, versão) é única no banco: reler o mesmo lote
 * não duplica nada (RN-23).
 */
@Entity
@Table(name = "evento_recebido")
public class EventoRecebido {

    /** Teto da espera entre tentativas, como no evento da agenda. */
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(6);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Convert(converter = OrigemIntegracao.Conversor.class)
    @Column(nullable = false, updatable = false)
    private OrigemIntegracao origem;

    @Convert(converter = TipoEventoRecebido.Conversor.class)
    @Column(nullable = false, updatable = false)
    private TipoEventoRecebido tipo;

    @Column(name = "id_externo", nullable = false, updatable = false)
    private long idExterno;

    /** atualizado_em da fonte, texto exato como recebido. */
    @Column(nullable = false, updatable = false)
    private String versao;

    /** O item inteiro, como chegou. */
    @ColumnTransformer(write = "?::jsonb")
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String corpo;

    @Convert(converter = EstadoEventoRecebido.Conversor.class)
    @Column(nullable = false)
    private EstadoEventoRecebido estado = EstadoEventoRecebido.PENDENTE;

    /** Por que não foi aplicado: o que a tela de pendências mostra (RF-75). */
    private String motivo;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private OffsetDateTime proximaTentativaEm;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvido_por_id")
    private Usuario resolvidoPor;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;

    private String justificativa;

    @Column(name = "recebido_em", nullable = false, updatable = false)
    private OffsetDateTime recebidoEm;

    @Column(name = "aplicado_em")
    private OffsetDateTime aplicadoEm;

    protected EventoRecebido() {
    }

    public EventoRecebido(OrigemIntegracao origem, TipoEventoRecebido tipo, long idExterno, String versao,
                          String corpo, OffsetDateTime agora) {
        this.origem = origem;
        this.tipo = tipo;
        this.idExterno = idExterno;
        this.versao = versao;
        this.corpo = corpo;
        this.recebidoEm = agora;
        this.proximaTentativaEm = agora;
    }

    public void aplicado(OffsetDateTime agora) {
        this.estado = EstadoEventoRecebido.APLICADO;
        this.aplicadoEm = agora;
        this.motivo = null;
        this.ultimoErro = null;
    }

    /** Problema de negócio: só uma pessoa resolve (RF-75). Nada é descartado em silêncio. */
    public void virarPendencia(String motivo) {
        this.estado = EstadoEventoRecebido.PENDENCIA;
        this.motivo = motivo;
    }

    /**
     * Falta algo que deve chegar sozinho (o cliente ainda não espelhado, o equipamento
     * ainda na fila). Espera e tenta de novo; esgotadas as tentativas, vira pendência.
     */
    public void aguardar(String motivo, OffsetDateTime agora, int maxTentativas, long esperaInicialSegundos) {
        this.motivo = motivo;
        if (++tentativas >= maxTentativas) {
            virarPendencia(motivo);
            return;
        }
        this.proximaTentativaEm = agora.plus(espera(esperaInicialSegundos));
    }

    /** Erro técnico ao aplicar. Mesma política de retentativa da agenda. */
    public void falhar(String erro, OffsetDateTime agora, int maxTentativas, long esperaInicialSegundos) {
        this.ultimoErro = erro;
        if (++tentativas >= maxTentativas) {
            this.estado = EstadoEventoRecebido.FALHOU;
            return;
        }
        this.proximaTentativaEm = agora.plus(espera(esperaInicialSegundos));
    }

    /** Chegou uma versão mais nova do mesmo item: esta não é mais aplicada por cima dela. */
    public void superar() {
        if (estado.emAberto()) {
            this.estado = EstadoEventoRecebido.SUPERADO;
        }
    }

    /** RF-75: depois de corrigir a causa (no Jiraya, na configuração), aplicar de novo. */
    public void reprocessar(Usuario quem, OffsetDateTime agora) {
        exigirParado();
        this.estado = EstadoEventoRecebido.PENDENTE;
        this.tentativas = 0;
        this.proximaTentativaEm = agora;
        this.resolvidoPor = quem;
        this.resolvidoEm = agora;
    }

    /** RF-75: descartar exige justificativa (ck_evento_descarte). */
    public void descartar(Usuario quem, String justificativa, OffsetDateTime agora) {
        exigirParado();
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("Informe por que este item pode ser descartado.");
        }
        this.estado = EstadoEventoRecebido.DESCARTADO;
        this.justificativa = justificativa;
        this.resolvidoPor = quem;
        this.resolvidoEm = agora;
    }

    private void exigirParado() {
        if (estado != EstadoEventoRecebido.PENDENCIA && estado != EstadoEventoRecebido.FALHOU) {
            throw new RegraDeNegocioException("Só pendências e falhas podem ser reprocessadas ou descartadas.");
        }
    }

    private Duration espera(long esperaInicialSegundos) {
        Duration espera = Duration.ofSeconds(esperaInicialSegundos).multipliedBy(1L << Math.min(tentativas - 1, 20));
        return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
    }

    public UUID getId() { return id; }
    public OrigemIntegracao getOrigem() { return origem; }
    public TipoEventoRecebido getTipo() { return tipo; }
    public long getIdExterno() { return idExterno; }
    public String getVersao() { return versao; }
    public String getCorpo() { return corpo; }
    public EstadoEventoRecebido getEstado() { return estado; }
    public String getMotivo() { return motivo; }
    public int getTentativas() { return tentativas; }
    public OffsetDateTime getProximaTentativaEm() { return proximaTentativaEm; }
    public String getUltimoErro() { return ultimoErro; }
    public String getJustificativa() { return justificativa; }
    public OffsetDateTime getRecebidoEm() { return recebidoEm; }
    public OffsetDateTime getAplicadoEm() { return aplicadoEm; }
}
