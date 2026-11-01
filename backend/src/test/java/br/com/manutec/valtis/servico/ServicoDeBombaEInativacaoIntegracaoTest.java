package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Serviço por bomba (RF-80, D-31) e inativação de serviço (RN-26), contra um Postgres real.
 *
 * Hoje é 13/10/2026 (RelogioFixo).
 */
class ServicoDeBombaEInativacaoIntegracaoTest extends TesteDeIntegracao {

    private String token;
    private String eu;
    private String clienteId;
    private String valvula;
    private String bomba;

    @BeforeEach
    void cadastrarClienteComValvulaEBomba() throws Exception {
        token = entrarComoAdmin();
        eu = ler(obter("/api/auth/eu", token), "$.id");

        clienteId = ler(enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Edifício Fictício Serviços","bairro":"Boa Viagem","cidade":"Recife","tipoAtendimento":"Avulso"}""")
                .andExpect(status().isCreated()), "$.id");

        String estacao = ler(enviar(post("/api/clientes/" + clienteId + "/estacoes"), token, """
                {"localizacaoInstalacao":"Barrilete","andarInicial":0,"andarFinal":4,"requerFechamentoGeral":false}""")
                .andExpect(status().isCreated()), "$.id");
        valvula = ler(enviar(post("/api/estacoes/" + estacao + "/valvulas"), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001"}""")
                .andExpect(status().isCreated()), "$.id");

        bomba = ler(enviar(post("/api/clientes/" + clienteId + "/bombas"), token, """
                {"fabricante":"WEG","modelo":"BC-92","periodicidadeMeses":12}""")
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void servicoNaBombaAbreCicloComOportunidadeDeBomba() throws Exception {
        // D-31: a bomba gera oportunidade como a VRP. RN-16: próxima = data + 12 meses.
        lancarNaBomba("2026-10-01")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].bombaId").value(bomba))
                .andExpect(jsonPath("$[0].proximaManutencao").value("2027-10-01"));

        obter("/api/bombas/" + bomba, token)
                .andExpect(jsonPath("$.bomba.ultimaManutencaoConsiderada").value("2026-10-01"))
                .andExpect(jsonPath("$.bomba.proximaManutencao").value("2027-10-01"))
                .andExpect(jsonPath("$.bomba.status").value("EM_DIA"))
                .andExpect(jsonPath("$.servicos.length()").value(1))
                .andExpect(jsonPath("$.servicos[0].origem").value("manual"))
                .andExpect(jsonPath("$.oportunidades[0].status").value("agendado"));

        List<String> escopos = ler(obter("/api/oportunidades", token), "$[?(@.bombaId == '" + bomba + "')].escopo");
        assertThat(escopos).containsExactly("bomba");
    }

    @Test
    void segundoServicoNaBombaFechaOCicloComoExecutado() throws Exception {
        lancarNaBomba("2025-11-01");
        lancarNaBomba("2026-10-10").andExpect(jsonPath("$[0].proximaManutencao").value("2027-10-10"));

        List<String> status = ler(obter("/api/bombas/" + bomba, token), "$.oportunidades[*].status");
        assertThat(status).containsExactly("agendado", "executado");
    }

    @Test
    void bombaDoPatrimonioNaoRecebeLancamento() throws Exception {
        String patrimonial = ler(enviar(post("/api/patrimonio/bombas"), token, """
                {"periodicidadeMeses":12}""").andExpect(status().isCreated()), "$.id");

        enviar(post("/api/servicos/lancamentos"), token, item("\"bombaId\":\"" + patrimonial + "\"", "2026-10-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("patrimônio")));
    }

    @Test
    void itemComValvulaEBombaAoMesmoTempoEhRecusado() throws Exception {
        enviar(post("/api/servicos/lancamentos"), token,
                item("\"valvulaId\":\"" + valvula + "\",\"bombaId\":\"" + bomba + "\"", "2026-10-01"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void bombaInativaNaoRecebeLancamento() throws Exception {
        enviar(post("/api/bombas/" + bomba + "/inativar"), token, null).andExpect(status().is2xxSuccessful());
        lancarNaBomba("2026-10-01").andExpect(status().isUnprocessableContent());
    }

    @Test
    void inativarOUltimoServicoVoltaOCicloParaOAnterior() throws Exception {
        // RN-26: o ciclo volta a contar do último serviço ATIVO.
        lancarNaValvula("2025-11-01");
        String recente = ler(lancarNaValvula("2026-10-10"), "$[0].servicoId");

        inativar(recente, "OS reaberta no Jiraya").andExpect(status().isNoContent());

        obter("/api/valvulas/" + valvula, token)
                .andExpect(jsonPath("$.ultimaManutencao").value("2025-11-01"))
                .andExpect(jsonPath("$.proximaManutencao").value("2026-11-01"))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[0].ativo").value(false))
                .andExpect(jsonPath("$.historico[0].motivoInativacao").value("OS reaberta no Jiraya"));

        // O ciclo aberto é o mesmo, reprogramado: nada de oportunidade nova.
        List<String> previstas = ler(obter("/api/oportunidades", token),
                "$[?(@.valvulaId == '" + valvula + "')].dataPrevista");
        assertThat(previstas).containsExactly("2026-11-01");

        // Já inativo não se inativa de novo.
        inativar(recente, "de novo").andExpect(status().isUnprocessableContent());
    }

    @Test
    void inativarServicoAntigoNaoMexeNoCiclo() throws Exception {
        String antigo = ler(lancarNaValvula("2025-11-01"), "$[0].servicoId");
        lancarNaValvula("2026-10-10");

        inativar(antigo, "Lançado por engano").andExpect(status().isNoContent());

        obter("/api/valvulas/" + valvula, token)
                .andExpect(jsonPath("$.ultimaManutencao").value("2026-10-10"))
                .andExpect(jsonPath("$.proximaManutencao").value("2027-10-10"));
    }

    @Test
    void unicoServicoDoEquipamentoNaoEhInativadoAteDecisao() throws Exception {
        // P-32: sem serviço anterior o equipamento ficaria sem ciclo.
        String unico = ler(lancarNaBomba("2026-10-01"), "$[0].servicoId");

        inativar(unico, "OS cancelada").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("P-32")));
        obter("/api/bombas/" + bomba, token).andExpect(jsonPath("$.servicos[0].ativo").value(true));
    }

    @Test
    void inativacaoExigeMotivo() throws Exception {
        String servico = ler(lancarNaValvula("2026-10-01"), "$[0].servicoId");
        inativar(servico, " ").andExpect(status().isBadRequest());
    }

    private ResultActions lancarNaValvula(String data) throws Exception {
        return enviar(post("/api/servicos/lancamentos"), token, item("\"valvulaId\":\"" + valvula + "\"", data));
    }

    private ResultActions lancarNaBomba(String data) throws Exception {
        return enviar(post("/api/servicos/lancamentos"), token, item("\"bombaId\":\"" + bomba + "\"", data));
    }

    private ResultActions inativar(String servicoId, String motivo) throws Exception {
        return enviar(post("/api/servicos/" + servicoId + "/inativacao"), token, """
                {"motivo":"%s"}""".formatted(motivo));
    }

    private String item(String equipamento, String data) {
        return """
                {"itens":[{%s,"dataRealizada":"%s","tipoManutencao":"Corretiva","tecnicoResponsavelId":"%s"}]}"""
                .formatted(equipamento, data, eu);
    }
}
