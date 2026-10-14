package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-11: busca de clientes. */
class BuscaDeClientesIntegracaoTest extends TesteDeIntegracao {

    /**
     * Bug encontrado no teste manual: sem termo de busca, o Postgres recebia o
     * parâmetro nulo como bytea e a consulta quebrava com "lower(bytea) does not exist".
     */
    @Test
    void listarSemTermoDeBuscaDevolveTodos() throws Exception {
        String token = entrarComoAdmin();

        obter("/api/clientes", token).andExpect(status().isOk());
        obter("/api/clientes?busca=", token).andExpect(status().isOk());
    }

    @Test
    void buscaIgnoraMaiusculasEOlhaNomeCodigoEBairro() throws Exception {
        String token = entrarComoAdmin();
        enviar(post("/api/clientes"), token, """
                {"nomeCondominio":"Residencial Busca Única","bairro":"Casa Forte"}""")
                .andExpect(status().isCreated());

        List<String> porNome = ler(obter("/api/clientes?busca=busca única", token), "$[*].nomeCondominio");
        List<String> porBairro = ler(obter("/api/clientes?busca=CASA FORTE", token), "$[*].nomeCondominio");

        assertThat(porNome).containsExactly("Residencial Busca Única");
        assertThat(porBairro).contains("Residencial Busca Única");
    }
}
