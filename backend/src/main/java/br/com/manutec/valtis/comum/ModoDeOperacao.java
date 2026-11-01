package br.com.manutec.valtis.comum;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * RF-70, D-29: quem escreve os cadastros. Lido uma vez, na subida.
 *
 * O modo é conferido em dois lugares só (architecture.md §9): nos controllers de
 * cadastro, que em `externa` recusam escrita dizendo onde o dado é gerido, e na
 * ativação dos leitores da integração. Recorrência, oportunidade, agenda e painel
 * não consultam o modo.
 */
@Component
public class ModoDeOperacao {

    public enum Origem { PROPRIA, EXTERNA }

    public static final String MANUTEC_CLIENTES = "Manutec Clientes";
    public static final String JIRAYA = "Jiraya";

    private final Origem origem;

    public ModoDeOperacao(ValtisPropriedades propriedades) {
        String valor = propriedades.cadastros() == null || propriedades.cadastros().origem() == null
                ? "propria"
                : propriedades.cadastros().origem().strip().toLowerCase(Locale.ROOT);
        this.origem = switch (valor) {
            case "propria" -> Origem.PROPRIA;
            case "externa" -> Origem.EXTERNA;
            // Melhor não subir do que subir no modo errado e aceitar cadastro que o Jiraya vai contradizer.
            default -> throw new IllegalStateException(
                    "VALTIS_CADASTROS_ORIGEM deve ser 'propria' ou 'externa', não '" + valor + "'.");
        };
    }

    public Origem origem() {
        return origem;
    }

    public boolean isExterna() {
        return origem == Origem.EXTERNA;
    }

    /** RF-71, RF-77: em `externa`, o cadastro do que vem de fora não é feito aqui. */
    public void exigirCadastroProprio(String oQue, String ondeEGerido) {
        if (isExterna()) {
            throw new ConflitoException(geridoFora(oQue, ondeEGerido));
        }
    }

    /**
     * RN-24 por campo: o registro espelhado não se edita pela tela comum, em nenhum
     * modo. O que é do Valtis nele se completa pela rota de complemento.
     */
    public static void exigirNaoEspelhado(boolean espelhado, String oQue, String ondeEGerido) {
        if (espelhado) {
            throw new ConflitoException(geridoFora(oQue, ondeEGerido)
                    + " No Valtis, só se completa o que a fonte não tem.");
        }
    }

    private static String geridoFora(String oQue, String ondeEGerido) {
        return "No modo integrado, %s é gerido no %s.".formatted(oQue, ondeEGerido);
    }
}
