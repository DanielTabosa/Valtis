package br.com.manutec.valtis.comum;

/**
 * O pedido é bem formado, mas o negócio não permite. Vira HTTP 422.
 * Ex.: lançar serviço com data futura, mudar o desfecho de oportunidade já encerrada.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
