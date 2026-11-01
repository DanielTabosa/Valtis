package br.com.manutec.valtis.integracao;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Até onde a leitura de cada fonte chegou (D-30, RF-78). O cursor só avança
 * depois que o lote foi gravado em evento_recebido.
 */
@Entity
@Table(name = "cursor_integracao")
public class CursorIntegracao {

    @Embeddable
    public record Chave(String origem, String recurso) implements Serializable {
    }

    @EmbeddedId
    private Chave chave;

    /** atualizado_em do último item gravado, texto exato como a fonte mandou. */
    private String desde;

    @Column(name = "apos_id")
    private Long aposId;

    @Column(name = "ultima_leitura_em")
    private OffsetDateTime ultimaLeituraEm;

    @Column(name = "ultima_leitura_ok_em")
    private OffsetDateTime ultimaLeituraOkEm;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    protected CursorIntegracao() {
    }

    public CursorIntegracao(RecursoLido recurso) {
        this.chave = recurso.chave();
    }

    public void avancar(String desde, long aposId) {
        this.desde = desde;
        this.aposId = aposId;
    }

    public void leituraBemSucedida(OffsetDateTime agora) {
        this.ultimaLeituraEm = agora;
        this.ultimaLeituraOkEm = agora;
        this.ultimoErro = null;
    }

    public void leituraFalhou(String erro, OffsetDateTime agora) {
        this.ultimaLeituraEm = agora;
        this.ultimoErro = erro;
    }

    public Chave getChave() { return chave; }
    public String getDesde() { return desde; }
    public Long getAposId() { return aposId; }
    public OffsetDateTime getUltimaLeituraEm() { return ultimaLeituraEm; }
    public OffsetDateTime getUltimaLeituraOkEm() { return ultimaLeituraOkEm; }
    public String getUltimoErro() { return ultimoErro; }
}
