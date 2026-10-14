package br.com.manutec.valtis.comum;

/**
 * O pedido colide com algo que já existe. Vira HTTP 409.
 * Ex.: segunda válvula "01" na mesma estação (RF-13).
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
