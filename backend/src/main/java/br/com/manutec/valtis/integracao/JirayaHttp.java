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
 * Adaptador do Jiraya, pelo contrato v1 escrito no Valtis (docs/integracao/jiraya.md):
 * `equipamentos/alteracoes/` e `ordens-servico/alteracoes/`, com os itens completos,
 * e `tipos-equipamento/`, relida inteira a cada rodada.
 */
@Component
@ConditionalOnProperty(name = "valtis.cadastros.origem", havingValue = "externa")
public class JirayaHttp extends LeitorHttp {

    @Autowired
    public JirayaHttp(ValtisPropriedades propriedades, ObjectMapper json) {
        super(propriedades.integracao().jiraya(), json);
    }

    JirayaHttp(ValtisPropriedades.Fonte fonte, ObjectMapper json, RestClient http) {
        super(fonte, json, http);
    }

    @Override
    public OrigemIntegracao origem() {
        return OrigemIntegracao.JIRAYA;
    }

    @Override
    public Pagina alteracoes(RecursoLido recurso, String desde, Long aposId) {
        String rota = switch (recurso) {
            case EQUIPAMENTOS -> "equipamentos/alteracoes/";
            case ORDENS_SERVICO -> "ordens-servico/alteracoes/";
            case CLIENTES -> throw new IllegalArgumentException("Clientes vêm do Manutec Clientes, não do Jiraya.");
        };
        JsonNode pagina = obter(rota + cursor(desde, aposId));
        List<Item> itens = new ArrayList<>();
        for (JsonNode item : pagina.path("resultados")) {
            itens.add(new Item(item.path("id").asLong(), item.path("atualizado_em").asString(), texto(item)));
        }
        Item ultimo = itens.isEmpty() ? null : itens.getLast();
        return new Pagina(itens, ultimo == null ? null : ultimo.versao(), ultimo == null ? null : ultimo.id(),
                !pagina.path("proxima").isNull() && !pagina.path("proxima").isMissingNode());
    }

    @Override
    public List<TipoDeEquipamento> tiposDeEquipamento() {
        List<TipoDeEquipamento> tipos = new ArrayList<>();
        for (JsonNode tipo : obter("tipos-equipamento/").path("resultados")) {
            tipos.add(new TipoDeEquipamento(tipo.path("id").asLong(), tipo.path("nome").asString()));
        }
        return tipos;
    }

    @Override
    protected String nome() {
        return "Jiraya";
    }
}
