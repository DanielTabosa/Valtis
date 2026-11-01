package br.com.manutec.valtis.integracao;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Grava uma página lida e avança o cursor, numa transação só (D-30): se o banco
 * cair no meio, o cursor não andou e a página é relida na próxima rodada. Reler
 * não duplica, porque a chave (origem, tipo, id, versão) é única (RN-23).
 */
@Component
public class RecebimentoDeEventos {

    private static final Set<EstadoEventoRecebido> EM_ABERTO = EnumSet.of(EstadoEventoRecebido.PENDENTE,
            EstadoEventoRecebido.PENDENCIA, EstadoEventoRecebido.FALHOU);

    private final EventoRecebidoRepository eventos;
    private final CursorIntegracaoRepository cursores;
    private final Clock relogio;

    public RecebimentoDeEventos(EventoRecebidoRepository eventos, CursorIntegracaoRepository cursores, Clock relogio) {
        this.eventos = eventos;
        this.cursores = cursores;
        this.relogio = relogio;
    }

    /** Devolve quantos itens eram novos. */
    @Transactional
    public int gravar(RecursoLido recurso, FonteExterna.Pagina pagina) {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        int novos = 0;
        for (FonteExterna.Item item : pagina.itens()) {
            if (eventos.existsByOrigemAndTipoAndIdExternoAndVersao(recurso.origem(), recurso.tipo(), item.id(),
                    item.versao())) {
                continue;
            }
            // A versão nova manda: a antiga, se ainda não foi aplicada, não é aplicada por cima dela.
            eventos.emAbertoDoItem(recurso.origem(), recurso.tipo(), item.id(), EM_ABERTO)
                    .forEach(EventoRecebido::superar);
            eventos.save(new EventoRecebido(recurso.origem(), recurso.tipo(), item.id(), item.versao(), item.corpo(),
                    agora));
            novos++;
        }
        CursorIntegracao cursor = cursor(recurso);
        if (pagina.ultimoId() != null) {
            cursor.avancar(pagina.ultimoDesde(), pagina.ultimoId());
        }
        cursor.leituraBemSucedida(agora);
        return novos;
    }

    @Transactional
    public void registrarFalha(RecursoLido recurso, String erro) {
        cursor(recurso).leituraFalhou(erro, OffsetDateTime.now(relogio));
    }

    @Transactional(readOnly = true)
    public CursorIntegracao lerCursor(RecursoLido recurso) {
        return cursores.findById(recurso.chave()).orElseGet(() -> new CursorIntegracao(recurso));
    }

    private CursorIntegracao cursor(RecursoLido recurso) {
        return cursores.findById(recurso.chave()).orElseGet(() -> cursores.save(new CursorIntegracao(recurso)));
    }
}
