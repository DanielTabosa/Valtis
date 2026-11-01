package br.com.manutec.valtis.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Uma rodada do modo integrado (D-30): lê cada fonte a partir do cursor, grava
 * o que chegou e aplica a fila. Só existe em `externa` (architecture.md §9).
 *
 * A chamada HTTP acontece FORA de transação; só a gravação de cada página é
 * transacional (RecebimentoDeEventos). Uma fonte fora do ar não impede a outra.
 */
@Component
@ConditionalOnProperty(name = "valtis.cadastros.origem", havingValue = "externa")
public class LeitorDaIntegracao {

    public record ResumoDaRodada(int recebidos, int aplicados) {}

    private static final Logger log = LoggerFactory.getLogger(LeitorDaIntegracao.class);
    /** Trava contra página que nunca termina (fonte com defeito no cursor). */
    private static final int MAX_PAGINAS_POR_RODADA = 200;

    private final Map<OrigemIntegracao, FonteExterna> fontes;
    private final RecebimentoDeEventos recebimento;
    private final AplicadorDeEventos aplicador;
    private final TiposDoJiraya tiposDoJiraya;
    private final ReclassificacaoPorTipo reclassificacao;

    public LeitorDaIntegracao(List<FonteExterna> fontes, RecebimentoDeEventos recebimento,
                              AplicadorDeEventos aplicador, TiposDoJiraya tiposDoJiraya,
                              ReclassificacaoPorTipo reclassificacao) {
        this.fontes = fontes.stream().collect(Collectors.toMap(FonteExterna::origem, Function.identity(),
                (primeira, segunda) -> primeira));
        this.recebimento = recebimento;
        this.aplicador = aplicador;
        this.tiposDoJiraya = tiposDoJiraya;
        this.reclassificacao = reclassificacao;
    }

    /** RF-78 "ler agora" e o agendamento chamam o mesmo método; um de cada vez. */
    public synchronized ResumoDaRodada rodar() {
        // Antes de ler e aplicar os equipamentos: um tipo renomeado vale já nesta rodada.
        atualizarTiposDoJiraya();
        int recebidos = 0;
        // A ordem importa só para chegar tudo na mesma rodada; a aplicação respeita
        // as dependências sozinha (cliente, depois equipamento, depois OS).
        for (RecursoLido recurso : RecursoLido.values()) {
            recebidos += ler(recurso);
        }
        int aplicados = aplicador.aplicarPendentes();
        return new ResumoDaRodada(recebidos, aplicados);
    }

    /**
     * Antes de aplicar qualquer equipamento: o nome atual de cada tipo (jiraya.md,
     * rota 1). As pendências de um tipo que passou a ser VRP ou bomba voltam para a fila.
     */
    private void atualizarTiposDoJiraya() {
        FonteExterna jiraya = fontes.get(OrigemIntegracao.JIRAYA);
        if (jiraya == null || !jiraya.configurada()) {
            return;
        }
        try {
            List<FonteExterna.TipoDeEquipamento> tipos = jiraya.tiposDeEquipamento();
            if (tipos.isEmpty()) {
                return;
            }
            tiposDoJiraya.atualizar(tipos);
            int devolvidos = reclassificacao.devolverAFilaOQuePassouAContar();
            if (devolvidos > 0) {
                log.info("{} pendência(s) voltaram para a fila: o tipo do equipamento passou a ser VRP ou bomba.",
                        devolvidos);
            }
        } catch (FonteExterna.CredencialRecusadaException | FonteExterna.FonteIndisponivelException e) {
            // Segue com a lista da rodada anterior (ou com o nome do corpo). A mesma falha
            // aparece no cursor de equipamentos, lido logo depois, e é o que a tela mostra.
            log.warn("Tipos de equipamento do Jiraya não lidos nesta rodada: {}", e.getMessage());
        }
    }

    private int ler(RecursoLido recurso) {
        FonteExterna fonte = fontes.get(recurso.origem());
        if (fonte == null || !fonte.configurada()) {
            recebimento.registrarFalha(recurso, "Fonte sem URL ou sem token configurados.");
            return 0;
        }
        int recebidos = 0;
        try {
            CursorIntegracao cursor = recebimento.lerCursor(recurso);
            String desde = cursor.getDesde();
            Long aposId = cursor.getAposId();
            for (int pagina = 0; pagina < MAX_PAGINAS_POR_RODADA; pagina++) {
                FonteExterna.Pagina lida = fonte.alteracoes(recurso, desde, aposId);
                recebidos += recebimento.gravar(recurso, lida);
                if (!lida.temMais() || lida.ultimoId() == null) {
                    break;
                }
                desde = lida.ultimoDesde();
                aposId = lida.ultimoId();
            }
        } catch (FonteExterna.CredencialRecusadaException | FonteExterna.FonteIndisponivelException e) {
            // O que já foi gravado fica; o cursor parou na última página salva.
            log.warn("Leitura de {} interrompida: {}", recurso.nome(), e.getMessage());
            recebimento.registrarFalha(recurso, e.getMessage());
        }
        return recebidos;
    }
}
