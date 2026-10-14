package br.com.manutec.valtis.suporte;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: a aplicação inteira, com Postgres real,
 * Flyway rodando as migrations de verdade e segurança ligada.
 *
 * As requisições passam pelo MockMvc: o caminho HTTP completo (filtros de
 * segurança, validação, JSON), sem abrir porta de rede.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresDeTeste.class, RelogioFixo.class})
public abstract class TesteDeIntegracao {

    protected static final String ADMIN_EMAIL = "admin@teste.local";
    protected static final String ADMIN_SENHA = "Senha-De-Teste1";

    @Autowired
    protected MockMvc mvc;

    protected String entrar(String email, String senha) throws Exception {
        String corpo = """
                {"email":"%s","senha":"%s"}""".formatted(email, senha);
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.token");
    }

    protected String entrarComoAdmin() throws Exception {
        return entrar(ADMIN_EMAIL, ADMIN_SENHA);
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder requisicao, String token, String json)
            throws Exception {
        requisicao.header("Authorization", "Bearer " + token);
        if (json != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(requisicao);
    }

    protected ResultActions obter(String url, String token) throws Exception {
        return enviar(get(url), token, null);
    }

    protected static String corpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static <T> T ler(ResultActions resultado, String caminho) throws Exception {
        return JsonPath.read(corpo(resultado), caminho);
    }
}
