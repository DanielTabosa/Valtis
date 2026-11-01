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

    /** Um tipo de equipamento do Jiraya como está agora (jiraya.md, rota 1). */
    record TipoDeEquipamento(long id, String nome) {}

    OrigemIntegracao origem();

    boolean configurada();

    /** `desde` nulo = desde o início (primeira carga). */
    Pagina alteracoes(RecursoLido recurso, String desde, Long aposId);

    /**
     * A lista inteira de tipos de equipamento, relida a cada rodada: renomear um tipo
     * não muda o `atualizado_em` dos equipamentos dele (jiraya.md, rota 1). Só o
     * Jiraya fornece; as outras fontes devolvem vazio.
     */
    default List<TipoDeEquipamento> tiposDeEquipamento() {
        return List.of();
    }

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
