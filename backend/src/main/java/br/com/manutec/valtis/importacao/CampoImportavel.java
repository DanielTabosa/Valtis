package br.com.manutec.valtis.importacao;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * O que o importador sabe aproveitar de uma planilha (RF-51). Cada campo lista os
 * nomes de cabeçalho que reconhece, já normalizados (sem acento, minúsculos).
 * Coluna cujo cabeçalho não bate com nenhum é descartada.
 */
public enum CampoImportavel {

    NOME_CONDOMINIO("Nome do condomínio",
            "nome do condominio", "condominio", "cliente", "nome do cliente", "nome", "razao social",
            "edificio", "predio", "nome do edificio"),
    ENDERECO("Endereço", "endereco", "logradouro", "rua", "endereco completo"),
    BAIRRO("Bairro", "bairro"),
    CIDADE("Cidade", "cidade", "municipio"),
    SINDICO_RESPONSAVEL("Síndico / responsável",
            "sindico", "sindico responsavel", "responsavel", "contato", "nome do contato", "sindica"),
    TELEFONE_CONTATO("Telefone", "telefone", "fone", "celular", "whatsapp", "telefone de contato", "tel"),
    EMAIL_CONTATO("E-mail", "email", "e mail", "email de contato"),
    TIPO_ATENDIMENTO("Atendimento (contrato/avulso)", "tipo de atendimento", "atendimento", "tipo", "contrato",
            "modalidade"),
    ULTIMA_MANUTENCAO("Última manutenção",
            "ultima manutencao", "data da ultima manutencao", "data ultima manutencao", "ultimo atendimento",
            "ultima visita", "data", "data nf", "data da nf", "data emissao", "data de emissao",
            "data do servico", "data da manutencao");

    private final String rotulo;
    private final List<String> cabecalhos;

    CampoImportavel(String rotulo, String... cabecalhos) {
        this.rotulo = rotulo;
        this.cabecalhos = List.of(cabecalhos);
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Primeiro tenta o nome exato; depois, um nome composto contido no cabeçalho
     * ("Nome do condomínio / cliente"). Palavras soltas como "data" não entram na
     * segunda tentativa: casariam com qualquer coisa.
     */
    static Optional<CampoImportavel> reconhecer(String cabecalho) {
        String normalizado = normalizar(cabecalho);
        if (normalizado.isEmpty()) {
            return Optional.empty();
        }
        Optional<CampoImportavel> exato = Arrays.stream(values())
                .filter(c -> c.cabecalhos.contains(normalizado))
                .findFirst();
        if (exato.isPresent()) {
            return exato;
        }
        String comBordas = " " + normalizado + " ";
        return Arrays.stream(values())
                .filter(c -> c.cabecalhos.stream()
                        .filter(nome -> nome.contains(" "))
                        .anyMatch(nome -> comBordas.contains(" " + nome + " ")))
                .findFirst();
    }

    /** "Condomínio  Ed. São-José" → "condominio ed sao jose". Também compara nomes de cliente (RF-57). */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
    }
}
