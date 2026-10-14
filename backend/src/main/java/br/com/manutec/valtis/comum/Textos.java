package br.com.manutec.valtis.comum;

/** Normalização de texto vindo de formulário. */
public final class Textos {

    private Textos() {
    }

    /** Tira espaços das pontas e transforma texto vazio em null, para não gravar "" no banco. */
    public static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.isEmpty() ? null : limpo;
    }
}
