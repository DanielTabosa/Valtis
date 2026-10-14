package br.com.manutec.valtis.estacao;

/**
 * Texto da faixa de andares atendida por uma estação.
 * Convenção do cadastro: térreo = 0, subsolo = negativo (database.md).
 */
public final class Andares {

    private Andares() {
    }

    /** Ex.: (0, 4) → "Térreo ao 4º"; (-1, 0) → "Subsolo 1 ao Térreo"; (5, 5) → "5º". */
    public static String descrever(int inicial, int fim) {
        if (inicial == fim) {
            return andar(inicial);
        }
        return "%s ao %s".formatted(andar(inicial), andar(fim));
    }

    private static String andar(int numero) {
        if (numero == 0) {
            return "Térreo";
        }
        if (numero < 0) {
            return "Subsolo " + Math.abs(numero);
        }
        return numero + "º";
    }
}
