package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Os adaptadores HTTP contra servidores falsos: o que vai na requisição (token,
 * cursor codificado) e como os erros do contrato viram as exceções da porta.
 */
class LeitoresHttpTest {

    private static final String BASE = "https://fonte.teste/api/integracao/v1/";
    private static final ValtisPropriedades.Fonte FONTE =
            new ValtisPropriedades.Fonte(BASE, "token-de-teste-com-32-caracteres!!", Duration.ofSeconds(1));

    private final ObjectMapper json = JsonMapper.builder().build();
    private MockRestServiceServer servidor;
    private RestClient http;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        http = builder.build();
    }

    @Test
    void jirayaMandaOTokenECodificaOCursorComFuso() {
        // O + do fuso viraria espaço sem codificação (convenção dos contratos).
        servidor.expect(requestTo(BASE + "equipamentos/alteracoes/?limite=100&desde=2026-10-24T10%3A15%3A30%2B00%3A00&apos_id=12"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-teste-com-32-caracteres!!"))
                .andRespond(withSuccess("""
                        {"resultados":[{"id":31,"tipo":{"id":1,"nome":"Bomba de recalque"},
                                        "atualizado_em":"2026-10-28T14:00:00.123456-03:00"}],
                         "limite":100,"proxima":null}""", MediaType.APPLICATION_JSON));

        FonteExterna.Pagina pagina = new JirayaHttp(FONTE, json, http)
                .alteracoes(RecursoLido.EQUIPAMENTOS, "2026-10-24T10:15:30+00:00", 12L);

        assertThat(pagina.itens()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(31);
            // A versão é guardada exatamente como veio, com a fração de segundo.
            assertThat(item.versao()).isEqualTo("2026-10-28T14:00:00.123456-03:00");
            assertThat(item.corpo()).contains("Bomba de recalque");
        });
        assertThat(pagina.ultimoId()).isEqualTo(31);
        assertThat(pagina.temMais()).isFalse();
        servidor.verify();
    }

    @Test
    void primeiraCargaVaiSemCursor() {
        servidor.expect(requestTo(BASE + "ordens-servico/alteracoes/?limite=100"))
                .andRespond(withSuccess("""
                        {"resultados":[],"limite":100,"proxima":"https://fonte.teste/proxima"}""",
                        MediaType.APPLICATION_JSON));

        FonteExterna.Pagina pagina = new JirayaHttp(FONTE, json, http)
                .alteracoes(RecursoLido.ORDENS_SERVICO, null, null);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.ultimoId()).isNull();
        servidor.verify();
    }

    @Test
    void manutecBuscaODetalheDeCadaClienteAlterado() {
        servidor.expect(requestTo(BASE + "clientes/alteracoes/?limite=100"))
                .andRespond(withSuccess("""
                        {"resultados":[{"id":412,"nome":"CONDOMINIO","atualizado_em":"2026-10-24T10:15:30-03:00"}],
                         "limite":100,"proxima":null}""", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "clientes/412/"))
                .andRespond(withSuccess("""
                        {"id":412,"nome":"CONDOMINIO","nome_fantasia":"Solar","ativo":true,"enderecos":[],
                         "contatos":[],"atualizado_em":"2026-10-24T10:15:31-03:00"}""", MediaType.APPLICATION_JSON));

        FonteExterna.Pagina pagina = new ManutecClientesHttp(FONTE, json, http)
                .alteracoes(RecursoLido.CLIENTES, null, null);

        // O cursor segue a lista; a versão é a do detalhe, que pode ser mais nova.
        assertThat(pagina.ultimoDesde()).isEqualTo("2026-10-24T10:15:30-03:00");
        assertThat(pagina.itens()).singleElement().satisfies(item -> {
            assertThat(item.versao()).isEqualTo("2026-10-24T10:15:31-03:00");
            assertThat(item.corpo()).contains("Solar");
        });
        servidor.verify();
    }

    @Test
    void tokenRecusadoEhConfiguracaoErrada() {
        servidor.expect(requestTo(BASE + "equipamentos/alteracoes/?limite=100"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> new JirayaHttp(FONTE, json, http).alteracoes(RecursoLido.EQUIPAMENTOS, null, null))
                .isInstanceOf(FonteExterna.CredencialRecusadaException.class)
                .hasMessageContaining("401")
                .hasMessageNotContaining("token-de-teste"); // o segredo nunca vai para mensagem (AGENTS.md §8)
    }

    @Test
    void erroDoServidorEhTemporario() {
        servidor.expect(requestTo(BASE + "equipamentos/alteracoes/?limite=100"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> new JirayaHttp(FONTE, json, http).alteracoes(RecursoLido.EQUIPAMENTOS, null, null))
                .isInstanceOf(FonteExterna.FonteIndisponivelException.class)
                .hasMessageContaining("503");
    }
}
