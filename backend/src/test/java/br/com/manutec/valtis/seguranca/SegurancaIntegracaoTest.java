package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-01, RNF-23, AGENTS.md §8: nenhuma rota sem autenticação, exceto o login. */
class SegurancaIntegracaoTest extends TesteDeIntegracao {

    @Test
    void semTokenNaoHaDado() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/oportunidades")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinalDeVidaEhPublico() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void senhaErradaNaoDizSeOEmailExiste() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@teste.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@teste.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void tokenAdulteradoEhRecusado() throws Exception {
        String token = entrarComoAdmin();
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        obter("/api/painel", adulterado).andExpect(status().isUnauthorized());
    }

    @Test
    void tecnicoUsaOSistemaMasNaoGerenciaUsuariosNemInativa() throws Exception {
        String admin = entrarComoAdmin();
        enviar(post("/api/usuarios"), admin, """
                {"nome":"Técnico Permissões","email":"tec.permissoes@teste.local","perfil":"tecnico",
                 "senha":"Senha-Tecnico1"}""").andExpect(status().isCreated());
        String tecnico = entrar("tec.permissoes@teste.local", "Senha-Tecnico1");

        obter("/api/painel", tecnico).andExpect(status().isOk());
        obter("/api/usuarios/ativos", tecnico).andExpect(status().isOk());
        obter("/api/usuarios", tecnico).andExpect(status().isForbidden());
        enviar(post("/api/clientes/00000000-0000-0000-0000-000000000000/inativar"), tecnico, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioInativadoPerdeOAcessoNaHoraMesmoComTokenValido() throws Exception {
        String admin = entrarComoAdmin();
        String id = ler(enviar(post("/api/usuarios"), admin, """
                {"nome":"Ex-funcionário","email":"saiu@teste.local","perfil":"tecnico",
                 "senha":"Senha-Tecnico1"}"""), "$.id");
        String tokenDele = entrar("saiu@teste.local", "Senha-Tecnico1");
        obter("/api/painel", tokenDele).andExpect(status().isOk());

        enviar(post("/api/usuarios/" + id + "/inativar"), admin, null).andExpect(status().isNoContent());

        obter("/api/painel", tokenDele).andExpect(status().isUnauthorized());
    }

    @Test
    void adminNaoInativaASiMesmo() throws Exception {
        String admin = entrarComoAdmin();
        String meuId = ler(obter("/api/auth/eu", admin), "$.id");

        enviar(post("/api/usuarios/" + meuId + "/inativar"), admin, null)
                .andExpect(status().isUnprocessableContent());
    }
}
