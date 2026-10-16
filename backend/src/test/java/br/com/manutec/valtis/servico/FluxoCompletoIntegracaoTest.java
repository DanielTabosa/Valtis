package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O caminho que o sistema existe para percorrer, de ponta a ponta, contra um
 * Postgres real: cadastrar, lançar, ver no painel, trabalhar a oportunidade.
 *
 * Hoje é 13/10/2026 (RelogioFixo).
 */
class FluxoCompletoIntegracaoTest extends TesteDeIntegracao {

    private String token;
    private String clienteId;
    private String codigoCliente;
    private String estacaoId;
    private String valvula1;
    private String valvula2;
    private String eu;

    @BeforeEach
    void cadastrarClienteComDuasValvulas() throws Exception {
        token = entrarComoAdmin();
        eu = ler(obter("/api/auth/eu", token), "$.id");

        var cliente = enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Edifício Fictício","bairro":"Pina","cidade":"Recife","tipoAtendimento":"Contrato"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoReferencia").value(matchesPattern("COND-\\d{3,}")));
        clienteId = ler(cliente, "$.id");
        codigoCliente = ler(cliente, "$.codigoReferencia");

        estacaoId = ler(enviar(post("/api/clientes/" + clienteId + "/estacoes"), token, """
                {"localizacaoInstalacao":"Barrilete - Casa de bombas","andarInicial":0,"andarFinal":8,
                 "requerFechamentoGeral":true}""").andExpect(status().isCreated()), "$.id");

        valvula1 = ler(enviar(post("/api/estacoes/" + estacaoId + "/valvulas"), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001","numeroSerie":"SERIE-1"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroValvula").value("01"))
                .andExpect(jsonPath("$.codigoReferencia").value(codigoCliente + "-VRP-01")), "$.id");

        valvula2 = ler(enviar(post("/api/estacoes/" + estacaoId + "/valvulas"), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001"}""")
                .andExpect(jsonPath("$.numeroValvula").value("02"))
                .andExpect(jsonPath("$.codigoReferencia").value(codigoCliente + "-VRP-02")), "$.id");
    }

    @Test
    void valvulaRecemCadastradaApareceSemRegistro() throws Exception {
        obter("/api/painel?clienteId=" + clienteId, token)
                .andExpect(jsonPath("$.linhas.length()").value(2))
                .andExpect(jsonPath("$.linhas[0].status").value("SEM_REGISTRO"))
                .andExpect(jsonPath("$.resumo[2].total").value(2));
    }

    @Test
    void duplicidadeNaEstacaoEhBarrada() throws Exception {
        enviar(post("/api/estacoes/" + estacaoId + "/valvulas"), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001","numeroValvula":"1"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(codigoCliente + "-VRP-01")));
    }

    @Test
    void faixaDeAndaresInvalidaVoltaComOCampo() throws Exception {
        enviar(post("/api/clientes/" + clienteId + "/estacoes"), token, """
                {"localizacaoInstalacao":"Shaft","andarInicial":10,"andarFinal":2}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.faixaDeAndaresValida").exists());
    }

    @Test
    void lancamentoAbreOCicloEPoeOsAlertasFuturosNaFila() throws Exception {
        // 01/11/2025 + 12 meses = 01/11/2026: faltam 19 dias, dentro da janela de 30.
        lancar(valvula1, "2025-11-01").andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].proximaManutencao").value("2026-11-01"));

        obter("/api/painel?clienteId=" + clienteId + "&status=PROXIMO_DO_VENCIMENTO", token)
                .andExpect(jsonPath("$.linhas[0].diasRestantes").value(19));

        // RF-43: já passou da data do alerta comercial e ninguém assumiu.
        var fila = obter("/api/oportunidades", token);
        List<Boolean> urgentes = ler(fila,
                "$[?(@.valvulaId == '" + valvula1 + "')].semDonoUrgente");
        assertThat(urgentes).containsExactly(true);

        // O alerta de 30 dias (02/10) já passou e não entra; o de 7 dias (25/10) entra.
        var agenda = obter("/api/agenda/situacao?status=PENDENTE", token);
        List<String> tipos = ler(agenda, "$.eventos[?(@.alvo == '" + codigoCliente + "-VRP-01')].tipo");
        assertThat(tipos).containsExactly("lembrete_final");
    }

    @Test
    void filtroDaAgendaRestringeAListaMasNaoOsTotais() throws Exception {
        lancar(valvula1, "2025-11-01");

        var agenda = obter("/api/agenda/situacao?status=CRIADO", token)
                .andExpect(jsonPath("$.totais.pendente").value(greaterThanOrEqualTo(1)));
        List<String> status = ler(agenda, "$.eventos[*].status");
        assertThat(status).doesNotContain("pendente");
    }

    @Test
    void novoServicoFechaOCicloAnteriorComoExecutado() throws Exception {
        lancar(valvula1, "2025-11-01");
        lancar(valvula1, "2026-10-10").andExpect(jsonPath("$[0].proximaManutencao").value("2027-10-10"));

        var ficha = obter("/api/valvulas/" + valvula1, token)
                .andExpect(jsonPath("$.status").value("EM_DIA"))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.oportunidades.length()").value(2));
        List<String> status = ler(ficha, "$.oportunidades[*].status");
        assertThat(status).containsExactly("agendado", "executado");
    }

    @Test
    void servicoAntigoLancadoAtrasadoNaoMexeNoCiclo() throws Exception {
        lancar(valvula1, "2026-09-01");
        lancar(valvula1, "2024-05-01").andExpect(jsonPath("$[0].proximaManutencao").doesNotExist());

        obter("/api/valvulas/" + valvula1, token)
                .andExpect(jsonPath("$.ultimaManutencao").value("2026-09-01"))
                .andExpect(jsonPath("$.oportunidades.length()").value(1));
    }

    @Test
    void lancamentoComUmaValvulaInvalidaNaoGravaNenhuma() throws Exception {
        enviar(post("/api/servicos/lancamentos"), token, """
                {"itens":[
                  {"valvulaId":"%s","dataRealizada":"2026-10-01","tipoManutencao":"Preventiva","tecnicoResponsavelId":"%s"},
                  {"valvulaId":"%s","dataRealizada":"2030-01-01","tipoManutencao":"Preventiva","tecnicoResponsavelId":"%s"}
                ]}""".formatted(valvula1, eu, valvula2, eu))
                .andExpect(status().isUnprocessableContent());

        // Tudo ou nada: a primeira válvula também não pode ter ficado gravada.
        obter("/api/valvulas/" + valvula1, token).andExpect(jsonPath("$.historico.length()").value(0));
    }

    @Test
    void recusaComPedidoDeLembreteAbreNovaOportunidade() throws Exception {
        lancar(valvula2, "2025-12-01");
        String oportunidade = idDaOportunidadeAberta(valvula2);

        enviar(post("/api/oportunidades/" + oportunidade + "/desfecho"), token, """
                {"status":"recusado","motivoRecusa":"Sem verba","lembrarNovamente":true,"lembrarEm":"2027-01-15"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oportunidade.status").value("recusado"))
                .andExpect(jsonPath("$.oportunidade.responsavelNome").value("Admin de Teste"))
                .andExpect(jsonPath("$.retorno.dataPrevista").value("2027-01-15"))
                .andExpect(jsonPath("$.retorno.status").value("agendado"));
    }

    @Test
    void mudarAPeriodicidadeReprogramaOCicloAberto() throws Exception {
        lancar(valvula1, "2026-10-10");

        enviar(put("/api/valvulas/" + valvula1), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001","numeroSerie":"SERIE-1",
                 "periodicidadeMeses":6}""").andExpect(status().isOk());

        var fila = obter("/api/oportunidades", token);
        List<String> datas = ler(fila, "$[?(@.valvulaId == '" + valvula1 + "')].dataPrevista");
        assertThat(datas).containsExactly("2027-04-10");
    }

    @Test
    void valvulaInativadaSaiDoPainelEDaFila() throws Exception {
        lancar(valvula2, "2026-01-01");

        enviar(post("/api/valvulas/" + valvula2 + "/inativar"), token, null).andExpect(status().isNoContent());

        obter("/api/painel?clienteId=" + clienteId, token).andExpect(jsonPath("$.linhas.length()").value(1));
        List<String> naFila = ler(obter("/api/oportunidades", token),
                "$[?(@.valvulaId == '" + valvula2 + "')].id");
        assertThat(naFila).isEmpty();
    }

    @Test
    void valvulaCadastradaPorEnganoPodeSerExcluida() throws Exception {
        enviar(delete("/api/valvulas/" + valvula2), token, null).andExpect(status().isNoContent());

        obter("/api/valvulas/" + valvula2, token).andExpect(status().isNotFound());
        obter("/api/painel?clienteId=" + clienteId, token).andExpect(jsonPath("$.linhas.length()").value(1));
    }

    @Test
    void valvulaComServicoNaoPodeSerExcluida() throws Exception {
        lancar(valvula1, "2026-01-01");

        enviar(delete("/api/valvulas/" + valvula1), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("inative")));
    }

    @Test
    void cicloQueJaChegaVencidoFicaMarcadoSemLembreteNaAgenda() throws Exception {
        // 01/10/2025 + 12 meses = 01/10/2026: os alertas de 30 e 7 dias já passaram.
        lancar(valvula1, "2025-10-01");
        // 01/11/2025: o alerta de 7 dias (25/10) ainda vai para a agenda.
        lancar(valvula2, "2025-11-01");

        var fila = obter("/api/oportunidades", token);
        List<Boolean> vencida = ler(fila, "$[?(@.valvulaId == '" + valvula1 + "')].semLembreteNaAgenda");
        List<Boolean> comLembrete = ler(fila, "$[?(@.valvulaId == '" + valvula2 + "')].semLembreteNaAgenda");
        assertThat(vencida).containsExactly(true);
        assertThat(comLembrete).containsExactly(false);
    }

    @Test
    void cartoesDoResumoNaoZeramAoFiltrarPorSituacao() throws Exception {
        lancar(valvula1, "2026-10-01");

        obter("/api/painel?clienteId=" + clienteId + "&status=EM_DIA", token)
                .andExpect(jsonPath("$.linhas.length()").value(1))
                .andExpect(jsonPath("$.resumo[2].total").value(1))
                .andExpect(jsonPath("$.resumo[3].total").value(1));
    }

    private ResultActions lancar(String valvulaId, String data) throws Exception {
        return enviar(post("/api/servicos/lancamentos"), token, """
                {"itens":[{"valvulaId":"%s","dataRealizada":"%s","tipoManutencao":"Preventiva",
                           "tecnicoResponsavelId":"%s"}]}""".formatted(valvulaId, data, eu));
    }

    private String idDaOportunidadeAberta(String valvulaId) throws Exception {
        List<String> ids = ler(obter("/api/oportunidades", token), "$[?(@.valvulaId == '" + valvulaId + "')].id");
        assertThat(ids).hasSize(1);
        return ids.getFirst();
    }
}
