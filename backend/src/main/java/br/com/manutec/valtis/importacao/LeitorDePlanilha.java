package br.com.manutec.valtis.importacao;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Transforma o arquivo enviado em cabeçalho + linhas de texto, sem interpretar nada.
 * Quem decide o que cada coluna significa é o ImportacaoService.
 *
 * Aceita .xlsx e .xls (primeira aba) e .csv separado por ';' ou ','.
 */
@Component
class LeitorDePlanilha {

    /** Tabela crua: a primeira linha não vazia vira cabeçalho. */
    record Planilha(List<String> cabecalho, List<LinhaCrua> linhas) {}

    /** `numero` é o da planilha (1 = primeira linha), para a tela apontar o mesmo lugar que o Excel. */
    record LinhaCrua(int numero, List<String> celulas) {

        String celula(int indice) {
            return indice < celulas.size() ? celulas.get(indice) : "";
        }
    }

    /** Bem acima dos ~300 clientes reais; barra arquivo errado antes de travar a tela. */
    static final int MAXIMO_DE_LINHAS = 5000;

    /** O Excel brasileiro salva CSV neste encoding, não em UTF-8. */
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    Planilha ler(String nomeArquivo, byte[] conteudo) {
        String nome = nomeArquivo == null ? "" : nomeArquivo.toLowerCase(Locale.ROOT);
        List<List<String>> tabela;
        if (nome.endsWith(".csv")) {
            tabela = lerCsv(conteudo);
        } else if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            tabela = lerExcel(conteudo);
        } else {
            throw new RegraDeNegocioException("Envie uma planilha .xlsx, .xls ou .csv.");
        }

        List<LinhaCrua> naoVazias = new ArrayList<>();
        for (int i = 0; i < tabela.size(); i++) {
            if (tabela.get(i).stream().anyMatch(celula -> !celula.isBlank())) {
                naoVazias.add(new LinhaCrua(i + 1, tabela.get(i)));
            }
        }
        if (naoVazias.isEmpty()) {
            throw new RegraDeNegocioException("A planilha está vazia.");
        }
        if (naoVazias.size() - 1 > MAXIMO_DE_LINHAS) {
            throw new RegraDeNegocioException("A planilha tem mais de %d linhas.".formatted(MAXIMO_DE_LINHAS));
        }
        return new Planilha(naoVazias.getFirst().celulas(), naoVazias.subList(1, naoVazias.size()));
    }

    private List<List<String>> lerExcel(byte[] conteudo) {
        try (Workbook pasta = WorkbookFactory.create(new ByteArrayInputStream(conteudo))) {
            Sheet aba = pasta.getSheetAt(0);
            DataFormatter formatador = new DataFormatter(Locale.forLanguageTag("pt-BR"));
            List<List<String>> tabela = new ArrayList<>();
            for (Row linha : aba) {
                // O POI pula linhas que nunca foram tocadas; completa para o número bater com o Excel.
                while (tabela.size() < linha.getRowNum()) {
                    tabela.add(List.of());
                }
                List<String> celulas = new ArrayList<>();
                for (int i = 0; i < Math.max(linha.getLastCellNum(), 0); i++) {
                    celulas.add(texto(linha.getCell(i), formatador));
                }
                tabela.add(celulas);
            }
            return tabela;
        } catch (IOException | RuntimeException e) {
            throw new RegraDeNegocioException("Não foi possível ler a planilha. Confira se o arquivo abre no Excel.");
        }
    }

    /** Data do Excel é um número com formato de data: vira AAAA-MM-DD, sem ambiguidade de dia/mês. */
    private static String texto(Cell celula, DataFormatter formatador) {
        if (celula == null) {
            return "";
        }
        if (celula.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celula)) {
            return celula.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return formatador.formatCellValue(celula).strip();
    }

    private static List<List<String>> lerCsv(byte[] conteudo) {
        String texto = decodificar(conteudo);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        String primeiraLinha = texto.lines().findFirst().orElse("");
        char separador = contar(primeiraLinha, ';') >= contar(primeiraLinha, ',') ? ';' : ',';
        return separarCsv(texto, separador);
    }

    private static String decodificar(byte[] conteudo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo))
                    .toString();
        } catch (CharacterCodingException naoEhUtf8) {
            return new String(conteudo, WINDOWS_1252);
        }
    }

    /** CSV com aspas: "Ed. Sol; Bloco A" é uma célula só, e "" dentro de aspas é uma aspa. */
    private static List<List<String>> separarCsv(String texto, char separador) {
        List<List<String>> tabela = new ArrayList<>();
        List<String> linha = new ArrayList<>();
        StringBuilder celula = new StringBuilder();
        boolean entreAspas = false;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    celula.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    celula.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == separador) {
                linha.add(celula.toString().strip());
                celula.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                linha.add(celula.toString().strip());
                celula.setLength(0);
                tabela.add(linha);
                linha = new ArrayList<>();
            } else {
                celula.append(c);
            }
        }
        if (!celula.isEmpty() || !linha.isEmpty()) {
            linha.add(celula.toString().strip());
            tabela.add(linha);
        }
        return tabela;
    }

    private static long contar(String texto, char alvo) {
        return texto.chars().filter(c -> c == alvo).count();
    }
}
