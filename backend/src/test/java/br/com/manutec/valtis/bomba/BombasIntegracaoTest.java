package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O módulo de bombas de ponta a ponta, contra um Postgres real: catálogo,
 * conjunto, bomba do condomínio, patrimônio, empréstimo e devolução.
 *
 * Hoje é 13/10/2026 (RelogioFixo).
 */
class BombasIntegracaoTest extends TesteDeIntegracao {

    private String token;
    private String clienteId;
    private String codigoCliente;
    private String conjuntoId;

    @BeforeEach
    void cadastrarClienteComConjuntoDeRecalque() throws Exception {
        token = entrarComoAdmin();

        var cliente = enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Edifício Fictício Bombas","bairro":"Pina","cidade":"Recife","tipoAtendimento":"Avulso"}""")
                .andExpect(status().isCreated());
        clienteId = ler(cliente, "$.id");
        codigoCliente = ler(cliente, "$.codigoReferencia");

        // Os 8 da migração, mais os que outros testes desta classe criam (o banco é compartilhado).
        List<String> tipos = ler(obter("/api/tipos-bomba", token)
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(8))), "$[?(@.nome == 'Recalque')].id");

        conjuntoId = ler(enviar(post("/api/clientes/" + clienteId + "/conjuntos"), token, """
                {"tipoId":"%s","localizacao":"Casa de bombas - subsolo"}""".formatted(tipos.getFirst()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Recalque")), "$.id");
    }

    @Test
    void bombaDoCondominioGanhaCodigoEEntraNaPosicao01() throws Exception {
        // RN-15 e RN-16: código automático; próxima = última + periodicidade.
        var bomba = enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"fabricante":"WEG","modelo":"BC-92","potenciaCv":3,"tensaoVolts":220,"correnteAmperes":9.5,
                 "periodicidadeMeses":12,"ultimaManutencao":"2025-11-01","conjuntoId":"%s","dataInstalacao":"2024-05-10"}"""
                .formatted(conjuntoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoReferencia").value(codigoCliente + "-BMB-01"))
                .andExpect(jsonPath("$.proximaManutencao").value("2026-11-01"))
                .andExpect(jsonPath("$.status").value("PROXIMO_DO_VENCIMENTO"))
                .andExpect(jsonPath("$.dadosTecnicosIncompletos").value(false))
                .andExpect(jsonPath("$.posicoes[0].numeroPosicao").value("01"))
                .andExpect(jsonPath("$.posicoes[0].natureza").value("instalacao"));

        obter("/api/clientes/" + clienteId + "/conjuntos", token)
                .andExpect(jsonPath("$[0].posicoes[0].codigoBomba").value(codigoCliente + "-BMB-01"));

        // Segunda bomba, sem dados técnicos (RF-63): fica marcada e vai para a posição 02.
        enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"periodicidadeMeses":12,"conjuntoId":"%s"}""".formatted(conjuntoId))
                .andExpect(jsonPath("$.codigoReferencia").value(codigoCliente + "-BMB-02"))
                .andExpect(jsonPath("$.dadosTecnicosIncompletos").value(true))
                .andExpect(jsonPath("$.status").value("SEM_REGISTRO"))
                .andExpect(jsonPath("$.posicoes[0].numeroPosicao").value("02"));

        // Painel de bombas: as duas, com os cartões contados.
        obter("/api/bombas/painel?busca=Bombas", token)
                .andExpect(jsonPath("$.linhas.length()").value(2))
                .andExpect(jsonPath("$.resumo[1].total").value(1))
                .andExpect(jsonPath("$.resumo[2].total").value(1));

        assertThat(ler(bomba, "$.id").toString()).isNotBlank();
    }

    @Test
    void emprestimoDevolucaoEReinstalacaoDaOriginal() throws Exception {
        // Instalada em 2024: a retirada em 01/10/2026 vem depois da entrada.
        String original = ler(enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"periodicidadeMeses":12,"conjuntoId":"%s","dataInstalacao":"2024-05-10"}""".formatted(conjuntoId)),
                "$.id");

        // RF-65: patrimônio nasce disponível, com código próprio.
        var patrimonial = enviar(post("/api/patrimonio/bombas"), token, """
                {"fabricante":"Schneider","modelo":"BC-98","periodicidadeMeses":6}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoReferencia").value(containsString("MAN-BMB-")))
                .andExpect(jsonPath("$.disponivel").value(true));
        String emprestada = ler(patrimonial, "$.id");

        // Empresta no lugar da original: a original é retirada (substituição), a emprestada entra na 01.
        var emprestimo = enviar(post("/api/bombas/" + emprestada + "/instalacao"), token, """
                {"conjuntoId":"%s","inicio":"2026-10-01","bombaSubstituidaId":"%s","observacoes":"original foi para conserto"}"""
                .formatted(conjuntoId, original))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupacao.natureza").value("emprestimo"))
                .andExpect(jsonPath("$.ocupacao.numeroPosicao").value("01"))
                .andExpect(jsonPath("$.aviso").doesNotExist());
        String ocupacaoEmprestimo = ler(emprestimo, "$.ocupacao.id");

        obter("/api/bombas/" + original, token)
                .andExpect(jsonPath("$.bomba.posicoes.length()").value(0))
                .andExpect(jsonPath("$.historico[0].motivoEncerramento").value("retirada"))
                .andExpect(jsonPath("$.historico[0].fim").value("2026-10-01"));

        // RN-17: emprestada não está disponível e não pode ser emprestada de novo.
        obter("/api/patrimonio/bombas", token)
                .andExpect(jsonPath("$[?(@.id == '" + emprestada + "')].disponivel").value(false));
        enviar(post("/api/bombas/" + emprestada + "/instalacao"), token, """
                {"conjuntoId":"%s","inicio":"2026-10-05"}""".formatted(conjuntoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("Registre a devolução")));

        // RN-19: a original volta antes de a emprestada sair. Permitido, com aviso.
        enviar(post("/api/bombas/" + original + "/instalacao"), token, """
                {"conjuntoId":"%s","inicio":"2026-10-10","bombaSubstituidaId":"%s"}""".formatted(conjuntoId, emprestada))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupacao.numeroPosicao").value("01"))
                .andExpect(jsonPath("$.aviso").value(containsString("também está ocupada")));

        // RN-18: a emprestada continua emprestada até a devolução explícita.
        obter("/api/bombas/" + emprestada, token)
                .andExpect(jsonPath("$.bomba.disponivel").value(false))
                .andExpect(jsonPath("$.bomba.posicoes[0].numeroPosicao").value("01"));

        enviar(post("/api/ocupacoes/" + ocupacaoEmprestimo + "/encerramento"), token, """
                {"fim":"2026-10-12"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoEncerramento").value("devolucao"))
                .andExpect(jsonPath("$.aberta").value(false));

        obter("/api/bombas/" + emprestada, token)
                .andExpect(jsonPath("$.bomba.disponivel").value(true))
                .andExpect(jsonPath("$.historico.length()").value(1));

        // RN-20: não se reescreve uma ocupação encerrada.
        enviar(post("/api/ocupacoes/" + ocupacaoEmprestimo + "/encerramento"), token, """
                {"fim":"2026-10-13"}""")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void bombaDeUmCondominioNaoEntraNoConjuntoDeOutro() throws Exception {
        String outroCliente = ler(enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Outro Edifício Fictício","tipoAtendimento":"Avulso"}"""), "$.id");
        String bombaDoOutro = ler(enviar(post("/api/clientes/" + outroCliente + "/bombas"), token, """
                {"periodicidadeMeses":12}"""), "$.id");

        enviar(post("/api/bombas/" + bombaDoOutro + "/instalacao"), token, """
                {"conjuntoId":"%s","inicio":"2026-10-01"}""".formatted(conjuntoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("pertence a")));
    }

    @Test
    void excluirSoSemHistoricoETipoDuplicadoEhBarrado() throws Exception {
        String semHistorico = ler(enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"periodicidadeMeses":12}"""), "$.id");
        enviar(delete("/api/bombas/" + semHistorico), token, null).andExpect(status().isNoContent());

        String instalada = ler(enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"periodicidadeMeses":12,"conjuntoId":"%s"}""".formatted(conjuntoId)), "$.id");
        enviar(delete("/api/bombas/" + instalada), token, null).andExpect(status().isConflict());

        enviar(post("/api/tipos-bomba"), token, """
                {"nome":"recalque"}""").andExpect(status().isConflict());
        enviar(post("/api/tipos-bomba"), token, """
                {"nome":"Água gelada"}""").andExpect(status().isCreated());
    }
}
