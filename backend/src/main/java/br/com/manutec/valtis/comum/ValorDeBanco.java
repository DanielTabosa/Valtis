package br.com.manutec.valtis.comum;

/**
 * Enum cujo valor gravado no banco difere do nome da constante.
 *
 * O banco guarda os valores da planilha da Manutec ('tecnico', 'Preventiva'),
 * protegidos por CHECK. O Java segue a convenção de constantes em maiúsculas.
 * Esta interface é a ponte entre os dois.
 */
public interface ValorDeBanco {

    String valor();

    static <E extends Enum<E> & ValorDeBanco> E deValor(Class<E> tipo, String valor) {
        for (E constante : tipo.getEnumConstants()) {
            if (constante.valor().equals(valor)) {
                return constante;
            }
        }
        throw new IllegalArgumentException("Valor desconhecido para %s: %s".formatted(tipo.getSimpleName(), valor));
    }
}
