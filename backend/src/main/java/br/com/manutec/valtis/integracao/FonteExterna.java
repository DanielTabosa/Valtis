package br.com.manutec.valtis.integracao;

import java.util.List;

/**
 * A porta de leitura de uma fonte do modo integrado (D-30). O leitor não sabe
 * se do outro lado há HTTP, um arquivo ou um teste: só pede a próxima página
 * de alterações a partir do cursor.
 *
 * O Manutec Clientes atende CLIENTES; o Jiraya, EQUIPAMENTOS e ORDENS_SERVICO.
 */
public interface FonteExterna {

    /** Um item lido: o id na fonte, a versão (atualizado_em, texto exato) e o corpo completo. */
    record Item(long id, String versao, String corpo) {}

    /**
     * Uma página. `ultimoDesde` e `ultimoId` são o cursor do último item da lista
     * (a fonte pode ter devolvido um corpo de detalhe com versão mais nova).
     */
    record Pagina(List<Item> itens, String ultimoDesde, Long ultimoId, boolean temMais) {}

    OrigemIntegracao origem();

    boolean configurada();

    /** `desde` nulo = desde o início (primeira carga). */
    Pagina alteracoes(RecursoLido recurso, String desde, Long aposId);

    /** A fonte recusou o token (401): é configuração errada, não se repete em laço. */
    class CredencialRecusadaException extends RuntimeException {
        public CredencialRecusadaException(String mensagem) {
            super(mensagem);
        }
    }

    /** Fora do ar, lenta ou com erro do lado de lá: tenta na próxima rodada. */
    class FonteIndisponivelException extends RuntimeException {
        public FonteIndisponivelException(String mensagem) {
            super(mensagem);
        }
    }
}
