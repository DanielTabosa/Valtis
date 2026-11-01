package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O modo integrado de ponta a ponta (D-29, D-30, D-31), contra um Postgres real,
 * com o Manutec Clientes e o Jiraya falsos (FontesFalsas). O relógio não anda:
 * por isso a espera entre tentativas é zero aqui.
 *
 * Hoje é 13/10/2026 (RelogioFixo).
 */
@TestPropertySource(properties = {
        "valtis.cadastros.origem=externa",
        "valtis.integracao.agendada=false",
        "valtis.integracao.espera-inicial-segundos=0",
        "valtis.integracao.max-tentativas=3"})
@Import(FontesFalsas.class)
class ModoIntegradoIntegracaoTest extends TesteDeIntegracao {

    /** Ids externos distintos por teste: o banco é compartilhado dentro da classe. */
    private static final AtomicLong IDS = new AtomicLong(1000);

    @Autowired
    private FontesFalsas.FonteFalsa manutecFalso;
    @Autowired
    private FontesFalsas.FonteFalsa jirayaFalso;
    @Autowired
    private LeitorDaIntegracao leitor;

    private String token;
    private long manutecId;
    private String nomeCliente;

    @BeforeEach
    void publicarUmCliente() throws Exception {
        token = entrarComoAdmin();
        manutecId = IDS.incrementAndGet();
        nomeCliente = "Condomínio Fictício " + manutecId;
        manutecFalso.publicar(RecursoLido.CLIENTES, manutecId, versao(1), cliente(manutecId, nomeCliente, true));
    }

    @Test
    void cadastroDoQueVemDeForaEhRecusadoComOndeEGerido() throws Exception {
        obter("/api/sistema", token).andExpect(jsonPath("$.modoCadastros").value("externa"));
        enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Novo","tipoAtendimento":"Avulso"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Manutec Clientes")));
        enviar(post("/api/tipos-bomba"), token, """
                {"nome":"Fonte ornamental"}""").andExpect(status().isConflict());
        enviar(post("/api/patrimonio/bombas"), token, """
                {"periodicidadeMeses":12}""").andExpect(status().isConflict());
    }

    @Test
    void osConcluidaViraServicoEOportunidadeParaVrpEBomba() throws Exception {
        long vrp = IDS.incrementAndGet();
        long bomba = IDS.incrementAndGet();
        long os = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01"));
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, bomba, versao(2),
                equipamento(bomba, "Bomba de recalque", "Casa de bombas", "BR-01"));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, os, versao(3),
                ordem(os, "concluida", "manutencao_corretiva", "2026-10-01", vrp, bomba));

        leitor.rodar();

        String clienteId = clienteDoValtis();
        var cliente = obter("/api/clientes/" + clienteId, token)
                .andExpect(jsonPath("$.nomeCondominio").value(nomeCliente))
                .andExpect(jsonPath("$.espelhado").value(true))
                .andExpect(jsonPath("$.estacoes[0].localizacaoInstalacao").value("Barrilete"))
                .andExpect(jsonPath("$.estacoes[0].derivada").value(true))
                .andExpect(jsonPath("$.estacoes[0].aCompletar").value(true))
                .andExpect(jsonPath("$.estacoes[0].andares").value("andares a completar"));
        String valvulaId = ler(cliente, "$.estacoes[0].valvulas[0].id");

        // RF-74: data de execução da OS (P-28), técnico = primeiro executor (P-29), origem jiraya (RF-76).
        obter("/api/valvulas/" + valvulaId, token)
                .andExpect(jsonPath("$.ultimaManutencao").value("2026-10-01"))
                .andExpect(jsonPath("$.proximaManutencao").value("2027-10-01"))
                .andExpect(jsonPath("$.valvula.espelhada").value(true))
                .andExpect(jsonPath("$.valvula.especificacao").value("Bermad 720"))
                .andExpect(jsonPath("$.historico[0].origem").value("jiraya"))
                .andExpect(jsonPath("$.historico[0].jirayaOsId").value((int) os))
                .andExpect(jsonPath("$.historico[0].tecnicoResponsavel").value("João Executor"))
                .andExpect(jsonPath("$.historico[0].lancadoPor").value("Jiraya"))
                .andExpect(jsonPath("$.historico[0].tipoManutencao").value("Corretiva"));

        // RF-73, Q-4: a bomba entra num conjunto derivado de Recalque, na posição 01.
        String bombaId = ler(obter("/api/clientes/" + clienteId + "/bombas", token)
                .andExpect(jsonPath("$[0].espelhada").value(true))
                .andExpect(jsonPath("$[0].identificacaoExterna").value("BR-01"))
                .andExpect(jsonPath("$[0].proximaManutencao").value("2027-10-01"))
                .andExpect(jsonPath("$[0].posicoes[0].numeroPosicao").value("01")), "$[0].id");
        obter("/api/clientes/" + clienteId + "/conjuntos", token)
                .andExpect(jsonPath("$[0].tipo").value("Recalque"))
                .andExpect(jsonPath("$[0].derivado").value(true));

        // D-31: as duas viram oportunidade, como no lançamento manual.
        List<String> escopos = ler(obter("/api/oportunidades", token),
                "$[?(@.clienteId == '" + clienteId + "')].escopo");
        assertThat(escopos).containsExactlyInAnyOrder("valvula", "bomba");

        // RN-23: reler tudo não duplica nada.
        leitor.rodar();
        obter("/api/valvulas/" + valvulaId, token).andExpect(jsonPath("$.historico.length()").value(1));
        obter("/api/bombas/" + bombaId, token).andExpect(jsonPath("$.servicos.length()").value(1));
    }

    @Test
    void osReabertaDepoisDeAplicadaViraPendenciaComOServico() throws Exception {
        long vrp = IDS.incrementAndGet();
        long os = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01"));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, os, versao(3),
                ordem(os, "concluida", "manutencao_preventiva", "2026-10-05", vrp));
        leitor.rodar();

        // P-27: reaberta no Jiraya depois de aplicada.
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, os, versao(4), ordem(os, "aberta", "manutencao_preventiva", null, vrp));
        leitor.rodar();

        var pendencias = obter("/api/integracao/pendencias", token).andExpect(status().isOk());
        List<String> motivos = ler(pendencias, "$[?(@.idExterno == " + os + ")].motivo");
        assertThat(motivos).singleElement().asString().contains("já tinha virado serviço", "\"aberta\"");
        List<Boolean> servicoAtivo = ler(pendencias, "$[?(@.idExterno == " + os + ")].servicos[0].ativo");
        assertThat(servicoAtivo).containsExactly(true);

        // RF-75: descartar exige justificativa; com ela, sai da lista.
        String pendenciaId = ((List<String>) ler(pendencias, "$[?(@.idExterno == " + os + ")].id")).getFirst();
        enviar(post("/api/integracao/eventos/" + pendenciaId + "/descarte"), token, """
                {"justificativa":" "}""").andExpect(status().isBadRequest());
        enviar(post("/api/integracao/eventos/" + pendenciaId + "/descarte"), token, """
                {"justificativa":"Reaberta só para anexar foto; o serviço vale."}""").andExpect(status().isNoContent());
        List<String> restantes = ler(obter("/api/integracao/pendencias", token), "$[?(@.idExterno == " + os + ")].id");
        assertThat(restantes).isEmpty();
    }

    @Test
    void equipamentoQueChegaAntesDoClienteEsperaEDepoisEntra() throws Exception {
        long outroCliente = IDS.incrementAndGet();
        long vrp = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamentoDe(outroCliente, "Condomínio Que Ainda Não Chegou", vrp,
                        "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-09"));
        leitor.rodar();
        assertThat(pendenciasDo(vrp)).isEmpty(); // esperando, ainda não é pendência

        manutecFalso.publicar(RecursoLido.CLIENTES, outroCliente, versao(5),
                cliente(outroCliente, "Condomínio Que Ainda Não Chegou", true));
        leitor.rodar();

        List<String> codigos = ler(obter("/api/painel?busca=Ainda", token), "$.linhas[*].codigoReferencia");
        assertThat(codigos).hasSize(1);
    }

    @Test
    void equipamentoQueNaoEhVrpNemBombaViraPendenciaEAOsIgnoraEle() throws Exception {
        long quadro = IDS.incrementAndGet();
        long vrp = IDS.incrementAndGet();
        long soQuadro = IDS.incrementAndGet();
        long misto = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, quadro, versao(2),
                equipamento(quadro, "Quadro elétrico de monitoramento/controle", "Subsolo", "QE-01"));
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Subsolo", "VRP-01"));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, soQuadro, versao(3),
                ordem(soQuadro, "concluida", "manutencao_corretiva", "2026-10-02", quadro));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, misto, versao(3),
                ordem(misto, "concluida", "manutencao_corretiva", "2026-10-03", quadro, vrp));
        leitor.rodar();

        assertThat(pendenciasDo(quadro)).singleElement().asString().startsWith("Tipo de equipamento não acompanhado");
        assertThat(pendenciasDo(soQuadro)).singleElement().asString().contains("sem nenhuma VRP ou bomba");
        assertThat(pendenciasDo(misto)).isEmpty();
    }

    @Test
    void soOsDeManutencaoReiniciaOCiclo() throws Exception {
        long vrp = IDS.incrementAndGet();
        long visita = IDS.incrementAndGet();
        long semTipo = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01"));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, visita, versao(3),
                ordem(visita, "concluida", "visita_tecnica", "2026-10-04", vrp));
        jirayaFalso.publicar(RecursoLido.ORDENS_SERVICO, semTipo, versao(3),
                ordem(semTipo, "concluida", null, "2026-10-04", vrp));
        leitor.rodar();

        // RN-22: a visita não vira serviço; a OS sem tipo vira pendência (J-1).
        String valvulaId = ler(obter("/api/clientes/" + clienteDoValtis(), token), "$.estacoes[0].valvulas[0].id");
        obter("/api/valvulas/" + valvulaId, token)
                .andExpect(jsonPath("$.status").value("SEM_REGISTRO"))
                .andExpect(jsonPath("$.historico.length()").value(0));
        assertThat(pendenciasDo(semTipo)).singleElement().asString().contains("sem tipo de serviço");
        assertThat(pendenciasDo(visita)).isEmpty();
    }

    @Test
    void espelhadoSoSeCompletaNoValtis() throws Exception {
        long vrp = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01"));
        leitor.rodar();
        var cliente = obter("/api/clientes/" + clienteDoValtis(), token);
        String valvulaId = ler(cliente, "$.estacoes[0].valvulas[0].id");
        String estacaoId = ler(cliente, "$.estacoes[0].id");

        // RN-24 por campo: a edição comum é recusada; o complemento, não.
        enviar(put("/api/valvulas/" + valvulaId), token, """
                {"especificacaoId":"bbbbbbbb-0000-0000-0000-000000000001"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Jiraya")));
        enviar(post("/api/valvulas/" + valvulaId + "/inativar"), token, null).andExpect(status().isConflict());
        enviar(put("/api/valvulas/" + valvulaId + "/complemento"), token, """
                {"periodicidadeMeses":6}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodicidadeMeses").value(6));
        enviar(put("/api/estacoes/" + estacaoId + "/complemento"), token, """
                {"andarInicial":0,"andarFinal":10,"requerFechamentoGeral":true}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aCompletar").value(false));

        // Uma nova versão do Jiraya não apaga o que o Valtis completou.
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(6),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01-NOVO"));
        leitor.rodar();
        obter("/api/valvulas/" + valvulaId, token)
                .andExpect(jsonPath("$.valvula.periodicidadeMeses").value(6))
                .andExpect(jsonPath("$.valvula.identificacaoExterna").value("VRP-01-NOVO"))
                .andExpect(jsonPath("$.andares").value("Térreo ao 10º"));
    }

    @Test
    void clienteInativadoNaFonteESubstituidoNoJirayaSaemDeOperacao() throws Exception {
        long vrp = IDS.incrementAndGet();
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(2),
                equipamento(vrp, "Válvula redutora de pressão (VRP)", "Barrilete", "VRP-01"));
        leitor.rodar();
        String clienteId = clienteDoValtis();
        String valvulaId = ler(obter("/api/clientes/" + clienteId, token), "$.estacoes[0].valvulas[0].id");

        // RF-79: substituída no Jiraya, inativada aqui.
        jirayaFalso.publicar(RecursoLido.EQUIPAMENTOS, vrp, versao(7), """
                {"id":%d,"tipo":{"id":2,"nome":"Válvula redutora de pressão (VRP)"},
                 "cliente":{"id":7,"manutec_id":%d,"nome":"%s"},"localizacao_interna":"Barrilete",
                 "identificacao":"VRP-01","substituido_por":{"equipamento_id":99,"em":"2026-10-10T10:00:00-03:00"},
                 "excluido":false,"atualizado_em":"%s"}""".formatted(vrp, manutecId, nomeCliente, versao(7)));
        // RF-71: inativado no Manutec Clientes, inativado aqui (RN-12).
        manutecFalso.publicar(RecursoLido.CLIENTES, manutecId, versao(8), cliente(manutecId, nomeCliente, false));
        leitor.rodar();

        obter("/api/valvulas/" + valvulaId, token).andExpect(jsonPath("$.valvula.ativo").value(false));
        obter("/api/clientes/" + clienteId, token).andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void fonteQueRecusaOTokenFicaNoEstadoSemPararAOutra() throws Exception {
        manutecFalso.falharCom(new FonteExterna.CredencialRecusadaException("Manutec Clientes recusou o token (401)."));
        leitor.rodar();

        var estado = obter("/api/integracao/estado", token)
                .andExpect(jsonPath("$.modoCadastros").value("externa"))
                .andExpect(jsonPath("$.leituraAtiva").value(true));
        List<String> erros = ler(estado, "$.cursores[?(@.recurso == 'clientes')].ultimoErro");
        assertThat(erros).singleElement().asString().contains("401");
        List<String> errosJiraya = ler(estado, "$.cursores[?(@.recurso == 'equipamentos')].ultimoErro");
        assertThat(errosJiraya).containsOnlyNulls();
    }

    // ------------------------------------------------- apoio

    private String clienteDoValtis() throws Exception {
        List<String> ids = ler(obter("/api/clientes?incluirInativos=true&busca=" + manutecId, token), "$[*].id");
        assertThat(ids).hasSize(1);
        return ids.getFirst();
    }

    private List<String> pendenciasDo(long idExterno) throws Exception {
        return ler(obter("/api/integracao/pendencias", token), "$[?(@.idExterno == " + idExterno + ")].motivo");
    }

    private static String versao(int n) {
        return "2026-10-%02dT10:00:00.123456-03:00".formatted(n);
    }

    private static String cliente(long id, String nome, boolean ativo) {
        return """
                {"id":%d,"tipo_pessoa":"PJ","nome":"RAZAO SOCIAL %d","nome_fantasia":"%s","ativo":%s,
                 "enderecos":[{"id":1,"descricao":"Matriz","principal":true,"ativo":true,"logradouro":"Rua Fictícia",
                               "numero":"10","complemento":null,"bairro":"Pina","cidade":"Recife","estado":"PE"}],
                 "contatos":[{"id":1,"nome":"Zelador","funcao":"Zelador","telefone":"81900000000","email":null},
                             {"id":2,"nome":"Síndica Fictícia","funcao":"Síndica","telefone":"81911111111",
                              "email":"sindica@exemplo.invalid"}],
                 "atualizado_em":"%s"}""".formatted(id, id, nome, ativo, versao(1));
    }

    private String equipamento(long id, String tipo, String localizacao, String identificacao) {
        return equipamentoDe(manutecId, nomeCliente, id, tipo, localizacao, identificacao);
    }

    private static String equipamentoDe(long manutecId, String nomeCliente, long id, String tipo, String localizacao,
                                        String identificacao) {
        return """
                {"id":%d,"tipo":{"id":1,"nome":"%s"},"cliente":{"id":7,"manutec_id":%d,"nome":"%s"},
                 "endereco":null,"localizacao_interna":"%s","identificacao":"%s","fabricante":"Bermad","modelo":"720",
                 "numero_serie":"SN-%d","substitui":null,"substituido_por":null,"excluido":false,"excluido_em":null,
                 "atualizado_em":"%s"}""".formatted(id, tipo, manutecId, nomeCliente, localizacao, identificacao, id,
                versao(2));
    }

    private String ordem(long id, String situacao, String tipoServico, String dataExecucao, long... equipamentos) {
        StringBuilder itens = new StringBuilder();
        for (long e : equipamentos) {
            itens.append(itens.isEmpty() ? "" : ",").append("{\"item_id\":").append(e).append(",\"equipamento_id\":")
                    .append(e).append(",\"descricao\":null}");
        }
        String conclusao = "concluida".equals(situacao)
                ? "{\"concluida_em\":\"2026-10-12T09:40:00-03:00\",\"concluida_por\":{\"id\":5,\"nome\":\"Gestor\"}}"
                : "null";
        return """
                {"id":%d,"situacao":"%s","tipo_servico":%s,"data_execucao":%s,
                 "cliente":{"id":7,"manutec_id":%d,"nome":"%s"},"descricao":"Troca de reparo",
                 "equipamentos":[%s],"conclusao":%s,"executores":[{"id":9,"nome":"João Executor"}],
                 "atualizado_em":"%s"}""".formatted(id, situacao, tipoServico == null ? "null" : "\"" + tipoServico + "\"",
                dataExecucao == null ? "null" : "\"" + dataExecucao + "\"", manutecId, nomeCliente, itens, conclusao,
                versao(3));
    }
}
