package br.com.manutec.valtis.importacao;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Importador de ponta a ponta (RF-51, RF-54, RF-56): arquivo → prévia → gravação.
 * Hoje é 13/10/2026 (RelogioFixo). Dados fictícios (AGENTS.md §5).
 */
class ImportacaoIntegracaoTest extends TesteDeIntegracao {

    /** Cabeçalhos como uma planilha real viria: acentos, maiúsculas e uma coluna que o Valtis não usa. */
    private static final String CSV = """
            CLIENTE;Bairro;Data NF;Valor da NF;Telefone
            Edifício Importado Um;Boa Viagem;15/03/2024;1.200,00;81 99999-0001
            Edifício Importado Dois;Pina;2026-09-01;800,00;
            ;Graças;10/01/2025;500,00;
            Edifício Importado Um;Boa Viagem;20/05/2023;300,00;
            Edifício Sem Data;Espinheiro;;;
            """;

    private String token;

    @BeforeEach
    void entrar() throws Exception {
        token = entrarComoAdmin();
    }

    @Test
    void reconheceAsColunasEDescartaAsQueNaoExistemNoCadastro() throws Exception {
        analisar(CSV, StandardCharsets.UTF_8)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunasUsadas[0].campo").value("NOME_CONDOMINIO"))
                .andExpect(jsonPath("$.colunasUsadas[2].campo").value("ULTIMA_MANUTENCAO"))
                .andExpect(jsonPath("$.colunasDescartadas[0]").value("Valor da NF"))
                .andExpect(jsonPath("$.linhas.length()").value(5))
                .andExpect(jsonPath("$.linhas[0].dados.linha").value(2))
                .andExpect(jsonPath("$.linhas[0].sugerida").value(true))
                .andExpect(jsonPath("$.linhas[2].erros[0]").value(containsString("nome")))
                .andExpect(jsonPath("$.linhas[3].avisos[0]").value(containsString("Repetido no arquivo (linha 2)")))
                .andExpect(jsonPath("$.linhas[3].sugerida").value(false));
    }

    @Test
    void csvSalvoPeloExcelBrasileiroMantemOsAcentos() throws Exception {
        analisar(CSV, Charset.forName("windows-1252"))
                .andExpect(jsonPath("$.linhas[0].dados.nomeCondominio").value("Edifício Importado Um"));
    }

    @Test
    void semColunaDeNomeExplicaOQueFazer() throws Exception {
        analisar("Bairro;Data\nPina;01/01/2025\n", StandardCharsets.UTF_8)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("Condomínio")));
    }

    @Test
    void importaAsLinhasConfirmadasEColocaNaFila() throws Exception {
        enviar(post("/api/importacao/clientes"), token, """
                {"linhas":[
                  {"linha":2,"nomeCondominio":"Edifício Importado Fila","bairro":"Boa Viagem",
                   "ultimaManutencao":"15/03/2024","tipoAtendimento":"Contrato"},
                  {"linha":3,"nomeCondominio":"Edifício Importado Sem Data"}
                ]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importados").value(2))
                .andExpect(jsonPath("$.naFila").value(1))
                .andExpect(jsonPath("$.semData").value(1));

        // RF-54: escopo cliente, já vencida (15/03/2025 ficou para trás) e sem lembrete na agenda (RN-13).
        var fila = obter("/api/oportunidades", token);
        List<String> previstas = ler(fila, "$[?(@.nomeCondominio == 'Edifício Importado Fila')].dataPrevista");
        List<Boolean> semLembrete = ler(fila,
                "$[?(@.nomeCondominio == 'Edifício Importado Fila')].semLembreteNaAgenda");
        assertThat(previstas).containsExactly("2025-03-15");
        assertThat(semLembrete).containsExactly(true);

        // Depois de importado, o mesmo nome passa a ser apontado como já existente (RF-57).
        analisar("Condomínio\nEDIFICIO importado fila\n", StandardCharsets.UTF_8)
                .andExpect(jsonPath("$.linhas[0].avisos[0]").value(containsString("Já existe no sistema")));
    }

    @Test
    void linhaComErroBarraOLoteInteiro() throws Exception {
        enviar(post("/api/importacao/clientes"), token, """
                {"linhas":[
                  {"linha":2,"nomeCondominio":"Edifício Que Não Pode Entrar"},
                  {"linha":3,"nomeCondominio":"Edifício Data Ruim","ultimaManutencao":"31/02/2024"}
                ]}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("linha 3")));

        List<String> nomes = ler(obter("/api/clientes?busca=Que%20Não%20Pode", token), "$[*].nomeCondominio");
        assertThat(nomes).isEmpty();
    }

    private ResultActions analisar(String conteudo, Charset charset) throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "clientes.csv", "text/csv",
                conteudo.getBytes(charset));
        return mvc.perform(multipart("/api/importacao/clientes/analise").file(arquivo)
                .header("Authorization", "Bearer " + token));
    }
}
