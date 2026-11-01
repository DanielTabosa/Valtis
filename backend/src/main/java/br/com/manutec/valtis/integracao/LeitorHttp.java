package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * O que os dois adaptadores HTTP têm em comum: token Bearer de serviço,
 * tempo-limite e a tradução dos erros do contrato (401 é configuração,
 * 5xx e rede são temporários).
 *
 * Os dois contratos são irmãos (docs/integracao/jiraya.md e o INTEGRACAO_JIRAYA.md
 * do Manutec Clientes): mesma paginação por `desde` + `apos_id`, mesmo formato de erro.
 */
abstract class LeitorHttp implements FonteExterna {

    static final int LIMITE_POR_PAGINA = 100;
    private static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(10);

    private final ValtisPropriedades.Fonte fonte;
    private final RestClient http;
    protected final ObjectMapper json;

    protected LeitorHttp(ValtisPropriedades.Fonte fonte, ObjectMapper json) {
        this.fonte = fonte;
        this.json = json;
        Duration tempoLimite = fonte == null || fonte.timeout() == null ? TEMPO_LIMITE_PADRAO : fonte.timeout();
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(tempoLimite);
        fabrica.setReadTimeout(tempoLimite);
        // RestClient.builder() direto: o Spring Boot 4 só cria um Builder com o starter de restclient.
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    /** Para os testes, com um servidor falso por trás do RestClient. */
    protected LeitorHttp(ValtisPropriedades.Fonte fonte, ObjectMapper json, RestClient http) {
        this.fonte = fonte;
        this.json = json;
        this.http = http;
    }

    @Override
    public boolean configurada() {
        return fonte != null && fonte.configurada();
    }

    /** `rota` relativa à URL base, já com a query montada. */
    protected JsonNode obter(String rota) {
        String base = fonte.url().endsWith("/") ? fonte.url() : fonte.url() + "/";
        try {
            String corpo = http.get()
                    .uri(URI.create(base + rota))
                    // O token nunca vai em URL nem em log (AGENTS.md §8).
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fonte.token())
                    .retrieve()
                    .body(String.class);
            return json.readTree(corpo);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 401) {
                throw new CredencialRecusadaException(nome() + " recusou o token de integração (401).");
            }
            throw new FonteIndisponivelException(nome() + " respondeu HTTP " + e.getStatusCode().value() + ".");
        } catch (ResourceAccessException e) {
            throw new FonteIndisponivelException(nome() + " fora do ar ou lento: " + e.getMostSpecificCause().getMessage());
        }
    }

    /** Query do cursor: `desde` codificado (o + do fuso viraria espaço), `apos_id` só quando há. */
    protected static String cursor(String desde, Long aposId) {
        StringBuilder query = new StringBuilder("?limite=").append(LIMITE_POR_PAGINA);
        if (desde != null) {
            query.append("&desde=").append(URLEncoder.encode(desde, StandardCharsets.UTF_8));
        }
        if (aposId != null) {
            query.append("&apos_id=").append(aposId);
        }
        return query.toString();
    }

    protected String texto(JsonNode no) {
        return json.writeValueAsString(no);
    }

    protected abstract String nome();
}
