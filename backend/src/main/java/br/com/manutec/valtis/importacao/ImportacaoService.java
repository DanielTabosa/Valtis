package br.com.manutec.valtis.importacao;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.cliente.DadosCliente;
import br.com.manutec.valtis.cliente.TipoAtendimento;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.importacao.LeitorDePlanilha.LinhaCrua;
import br.com.manutec.valtis.importacao.LeitorDePlanilha.Planilha;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Importação de clientes da base antiga (M7: RF-51 a RF-58).
 *
 * Duas etapas, como pede o RF-56: `analisar` lê o arquivo e devolve a prévia
 * (nada é gravado); `importar` recebe as linhas que o usuário confirmou, valida
 * tudo de novo e grava numa transação só. Uma linha com erro barra o lote inteiro.
 */
@Service
public class ImportacaoService {

    public record ColunaUsada(String cabecalho, CampoImportavel campo, String rotulo) {}

    public record LinhaAnalisada(LinhaImportacao dados, List<String> erros, List<String> avisos,
                                 boolean sugerida) {}

    public record Analise(List<ColunaUsada> colunasUsadas, List<String> colunasDescartadas,
                          List<LinhaAnalisada> linhas) {}

    public record Resultado(int importados, int naFila, int semData) {}

    private static final List<DateTimeFormatter> FORMATOS_DE_DATA = List.of(
            formato("uuuu-M-d"), formato("d/M/uuuu"), formato("d-M-uuuu"), formato("d.M.uuuu"), formato("d/M/uu"));
    private static final DateTimeFormatter MES_E_ANO = formato("M/uuuu");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final LeitorDePlanilha leitor;
    private final ClienteService clientes;
    private final OportunidadeService oportunidades;
    private final Clock relogio;

    public ImportacaoService(LeitorDePlanilha leitor, ClienteService clientes, OportunidadeService oportunidades,
                             Clock relogio) {
        this.leitor = leitor;
        this.clientes = clientes;
        this.oportunidades = oportunidades;
        this.relogio = relogio;
    }

    /** Etapa 1: entende as colunas e aponta o problema de cada linha. Não grava nada. */
    @Transactional(readOnly = true)
    public Analise analisar(String nomeArquivo, byte[] conteudo) {
        Planilha planilha = leitor.ler(nomeArquivo, conteudo);

        Map<CampoImportavel, Integer> indices = new EnumMap<>(CampoImportavel.class);
        List<ColunaUsada> usadas = new ArrayList<>();
        List<String> descartadas = new ArrayList<>();
        for (int i = 0; i < planilha.cabecalho().size(); i++) {
            String cabecalho = planilha.cabecalho().get(i);
            Optional<CampoImportavel> campo = CampoImportavel.reconhecer(cabecalho);
            // A primeira coluna de cada campo vence; uma segunda "Telefone" é descartada.
            if (campo.isPresent() && !indices.containsKey(campo.get())) {
                indices.put(campo.get(), i);
                usadas.add(new ColunaUsada(cabecalho, campo.get(), campo.get().getRotulo()));
            } else if (!cabecalho.isBlank()) {
                descartadas.add(cabecalho);
            }
        }
        if (!indices.containsKey(CampoImportavel.NOME_CONDOMINIO)) {
            throw new RegraDeNegocioException(
                    "Não encontrei a coluna com o nome do condomínio. Renomeie o cabeçalho para \"Condomínio\" "
                            + "ou \"Cliente\". Colunas da planilha: " + String.join(", ", planilha.cabecalho()) + ".");
        }

        List<LinhaImportacao> linhas = planilha.linhas().stream()
                .map(l -> montar(l, indices))
                .toList();
        return new Analise(usadas, descartadas, revalidar(linhas));
    }

    /** Depois de o usuário corrigir na tela: mesmas regras, sem reler o arquivo. */
    @Transactional(readOnly = true)
    public List<LinhaAnalisada> revalidar(List<LinhaImportacao> linhas) {
        Map<String, String> existentes = clientes.todos().stream()
                .collect(Collectors.toMap(c -> CampoImportavel.normalizar(c.getNomeCondominio()),
                        Cliente::getCodigoReferencia, (a, b) -> a));
        Map<String, Integer> jaNoArquivo = new HashMap<>();
        LocalDate hoje = LocalDate.now(relogio);

        List<LinhaAnalisada> resultado = new ArrayList<>();
        for (LinhaImportacao linha : linhas) {
            List<String> erros = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            boolean sugerida = true;

            String nome = Textos.limpar(linha.nomeCondominio());
            if (nome == null) {
                erros.add("Sem nome do condomínio.");
            } else if (nome.length() > 200) {
                erros.add("Nome com mais de 200 caracteres.");
            } else {
                // RF-57: o sistema aponta o provável repetido; quem decide é o usuário.
                String chave = CampoImportavel.normalizar(nome);
                if (existentes.containsKey(chave)) {
                    avisos.add("Já existe no sistema como " + existentes.get(chave) + ".");
                    sugerida = false;
                }
                Integer anterior = jaNoArquivo.putIfAbsent(chave, linha.linha());
                if (anterior != null) {
                    avisos.add("Repetido no arquivo (linha " + anterior + ").");
                    sugerida = false;
                }
            }

            String textoData = Textos.limpar(linha.ultimaManutencao());
            if (textoData == null) {
                avisos.add("Sem data da última manutenção: entra no cadastro, mas não na fila.");
            } else {
                Optional<LocalDate> data = lerData(textoData);
                if (data.isEmpty()) {
                    erros.add("Data \"" + textoData + "\" não reconhecida. Use DD/MM/AAAA.");
                } else if (data.get().isAfter(hoje)) {
                    erros.add("Data da última manutenção no futuro.");
                }
            }

            String email = Textos.limpar(linha.emailContato());
            if (email != null && !EMAIL.matcher(email).matches()) {
                avisos.add("E-mail \"" + email + "\" inválido: será descartado.");
            }
            String tipo = Textos.limpar(linha.tipoAtendimento());
            if (tipo != null && lerTipo(tipo).isEmpty()) {
                avisos.add("Atendimento \"" + tipo + "\" não reconhecido: entra como Avulso.");
            }
            resultado.add(new LinhaAnalisada(linha, erros, avisos, sugerida && erros.isEmpty()));
        }
        return resultado;
    }

    /** Etapa 2: grava as linhas confirmadas. Tudo ou nada. */
    @Transactional
    public Resultado importar(List<LinhaImportacao> linhas) {
        if (linhas.isEmpty()) {
            throw new RegraDeNegocioException("Nenhuma linha selecionada para importar.");
        }
        List<String> comErro = revalidar(linhas).stream()
                .filter(l -> !l.erros().isEmpty())
                .map(l -> "linha " + l.dados().linha() + ": " + String.join(" ", l.erros()))
                .toList();
        if (!comErro.isEmpty()) {
            throw new RegraDeNegocioException("Corrija antes de importar — " + String.join("; ", comErro));
        }

        int naFila = 0;
        for (LinhaImportacao linha : linhas) {
            LocalDate ultima = Optional.ofNullable(Textos.limpar(linha.ultimaManutencao()))
                    .flatMap(ImportacaoService::lerData)
                    .orElse(null);
            Cliente cliente = clientes.criarImportado(dadosDo(linha), ultima);
            if (ultima != null) {
                oportunidades.abrirCicloDeImportado(cliente, ultima);
                naFila++;
            }
        }
        return new Resultado(linhas.size(), naFila, linhas.size() - naFila);
    }

    private static LinhaImportacao montar(LinhaCrua linha, Map<CampoImportavel, Integer> indices) {
        return new LinhaImportacao(
                linha.numero(),
                valor(linha, indices, CampoImportavel.NOME_CONDOMINIO),
                valor(linha, indices, CampoImportavel.ENDERECO),
                valor(linha, indices, CampoImportavel.BAIRRO),
                valor(linha, indices, CampoImportavel.CIDADE),
                valor(linha, indices, CampoImportavel.SINDICO_RESPONSAVEL),
                valor(linha, indices, CampoImportavel.TELEFONE_CONTATO),
                valor(linha, indices, CampoImportavel.EMAIL_CONTATO),
                valor(linha, indices, CampoImportavel.TIPO_ATENDIMENTO),
                valor(linha, indices, CampoImportavel.ULTIMA_MANUTENCAO));
    }

    private static String valor(LinhaCrua linha, Map<CampoImportavel, Integer> indices, CampoImportavel campo) {
        Integer indice = indices.get(campo);
        return indice == null ? null : Textos.limpar(linha.celula(indice));
    }

    private static DadosCliente dadosDo(LinhaImportacao l) {
        String email = Textos.limpar(l.emailContato());
        return new DadosCliente(l.nomeCondominio(), l.endereco(), l.bairro(), l.cidade(), l.sindicoResponsavel(),
                l.telefoneContato(), email != null && EMAIL.matcher(email).matches() ? email : null,
                Optional.ofNullable(Textos.limpar(l.tipoAtendimento())).flatMap(ImportacaoService::lerTipo)
                        .orElse(TipoAtendimento.AVULSO),
                null);
    }

    /** Aceita 15/03/2024, 15/3/24, 2024-03-15 e, sem o dia, 03/2024 (vira dia 1: a data já é aproximada). */
    static Optional<LocalDate> lerData(String texto) {
        // "15/03/2024 00:00:00", como alguns sistemas exportam.
        String soAData = texto.strip().split("\\s+")[0];
        for (DateTimeFormatter formato : FORMATOS_DE_DATA) {
            try {
                return Optional.of(LocalDate.parse(soAData, formato));
            } catch (DateTimeParseException proximo) {
                // tenta o próximo formato
            }
        }
        try {
            return Optional.of(YearMonth.parse(soAData, MES_E_ANO).atDay(1));
        } catch (DateTimeParseException naoEhData) {
            return Optional.empty();
        }
    }

    private static Optional<TipoAtendimento> lerTipo(String texto) {
        String normalizado = CampoImportavel.normalizar(texto);
        if (normalizado.contains("contrato") || normalizado.equals("sim")) {
            return Optional.of(TipoAtendimento.CONTRATO);
        }
        if (normalizado.contains("avulso") || normalizado.equals("nao")) {
            return Optional.of(TipoAtendimento.AVULSO);
        }
        return Optional.empty();
    }

    private static DateTimeFormatter formato(String padrao) {
        return DateTimeFormatter.ofPattern(padrao).withResolverStyle(ResolverStyle.STRICT);
    }
}
