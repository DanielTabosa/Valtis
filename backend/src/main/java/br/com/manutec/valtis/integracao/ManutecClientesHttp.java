package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador do Manutec Clientes (RF-71), pelo contrato que ele já publica:
 * `clientes/alteracoes/` com cursor e `clientes/{id}/` com o detalhe.
 *
 * A lista de alterações traz só o resumo; o Valtis precisa de endereço e
 * contato, então busca o detalhe de cada item. São poucos (296 clientes,
 * alterações esparsas).
 */
@Component
@ConditionalOnProperty(name = "valtis.cadastros.origem", havingValue = "externa")
public class ManutecClientesHttp extends LeitorHttp {

    @Autowired
    public ManutecClientesHttp(ValtisPropriedades propriedades, ObjectMapper json) {
        super(propriedades.integracao().manutecClientes(), json);
    }

    ManutecClientesHttp(ValtisPropriedades.Fonte fonte, ObjectMapper json, RestClient http) {
        super(fonte, json, http);
    }

    @Override
    public OrigemIntegracao origem() {
        return OrigemIntegracao.MANUTEC_CLIENTES;
    }

    @Override
    public Pagina alteracoes(RecursoLido recurso, String desde, Long aposId) {
        if (recurso != RecursoLido.CLIENTES) {
            throw new IllegalArgumentException("O Manutec Clientes só fornece clientes.");
        }
        JsonNode pagina = obter("clientes/alteracoes/" + cursor(desde, aposId));
        List<Item> itens = new ArrayList<>();
        String ultimoDesde = null;
        Long ultimoId = null;
        for (JsonNode resumo : pagina.path("resultados")) {
            long id = resumo.path("id").asLong();
            JsonNode detalhe = obter("clientes/" + id + "/");
            itens.add(new Item(id, detalhe.path("atualizado_em").asString(), texto(detalhe)));
            ultimoDesde = resumo.path("atualizado_em").asString();
            ultimoId = id;
        }
        return new Pagina(itens, ultimoDesde, ultimoId, !pagina.path("proxima").isNull()
                && !pagina.path("proxima").isMissingNode());
    }

    @Override
    protected String nome() {
        return "Manutec Clientes";
    }
}
