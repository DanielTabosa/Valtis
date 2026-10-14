package br.com.manutec.valtis.agenda;

import java.time.LocalDate;
import java.util.List;

/**
 * A "porta" para a agenda externa (architecture.md §2: casos de uso falam com
 * interfaces; adaptadores implementam).
 *
 * O worker do Outbox só conhece esta interface. Trocar Google por Outlook,
 * ou por um simulador nos testes, não muda uma linha do worker.
 */
public interface AgendaGateway {

    /** Dados de um evento de dia inteiro. `idExterno` é escolhido por nós, para idempotência. */
    record NovoEvento(String idExterno, LocalDate data, String titulo, String descricao, List<String> convidados) {}

    /**
     * Cria o evento e devolve o id dele na agenda.
     * Se o evento com esse `idExterno` já existir, devolve o id sem duplicar.
     *
     * @throws AgendaIndisponivelException falha passageira: rede, 5xx, limite de requisições. Vale tentar de novo.
     * @throws AgendaNaoAutorizadaException credencial inválida ou revogada. Tentar de novo não resolve.
     */
    String criarEvento(NovoEvento evento);

    /** Nome do provedor, para a tela de sincronização. */
    String nome();

    class AgendaIndisponivelException extends RuntimeException {
        public AgendaIndisponivelException(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }

    class AgendaNaoAutorizadaException extends RuntimeException {
        public AgendaNaoAutorizadaException(String mensagem) {
            super(mensagem);
        }
    }
}
