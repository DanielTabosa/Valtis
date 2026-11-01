package br.com.manutec.valtis.integracao;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Os dois jeitos de um item não ser aplicado agora, como exceções: lançadas
 * dentro da transação da aplicação, desfazem tudo o que ela já tinha feito.
 */
final class ResultadoDaAplicacao {

    private ResultadoDaAplicacao() {
    }

    /** Problema de negócio: vai para a tela de pendências (RF-75). */
    static class Pendencia extends RuntimeException {
        Pendencia(String motivo) {
            super(motivo);
        }
    }

    /** Falta algo que deve chegar sozinho; o item espera e tenta de novo. */
    static class Aguardar extends RuntimeException {
        Aguardar(String motivo) {
            super(motivo);
        }
    }

    /** O começo do motivo dos equipamentos que não são VRP nem bomba: a OS os reconhece por ele. */
    static final String TIPO_NAO_MAPEADO = "Tipo de equipamento não acompanhado pelo Valtis";

    /** Trecho do motivo da OS cujos equipamentos não são VRP nem bomba: relida quando um deles passa a contar. */
    static final String OS_SEM_EQUIPAMENTO = "sem nenhuma VRP ou bomba";

    /** Sem acento, sem caixa, espaços simples: "Casa de Bombas " e "casa de bombas" são o mesmo lugar. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
