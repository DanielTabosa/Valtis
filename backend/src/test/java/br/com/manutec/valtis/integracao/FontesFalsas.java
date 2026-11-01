package br.com.manutec.valtis.integracao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * O Manutec Clientes e o Jiraya de mentira, em memória. Nunca há rede nos testes
 * (AGENTS.md §5). @Order(0): o leitor usa a primeira fonte de cada origem, e os
 * adaptadores HTTP de verdade ficam depois.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FontesFalsas {

    @Bean
    @Order(0)
    FonteFalsa manutecFalso() {
        return new FonteFalsa(OrigemIntegracao.MANUTEC_CLIENTES);
    }

    @Bean
    @Order(0)
    FonteFalsa jirayaFalso() {
        return new FonteFalsa(OrigemIntegracao.JIRAYA);
    }

    /** Uma fonte que devolve, em ordem, o que o teste publicou depois do cursor. */
    public static class FonteFalsa implements FonteExterna {

        private final OrigemIntegracao origem;
        private final Map<RecursoLido, List<Item>> publicados = new ConcurrentHashMap<>();
        private RuntimeException falha;

        FonteFalsa(OrigemIntegracao origem) {
            this.origem = origem;
        }

        public void publicar(RecursoLido recurso, long id, String versao, String corpo) {
            publicados.computeIfAbsent(recurso, r -> new ArrayList<>()).add(new Item(id, versao, corpo));
        }

        public void falharCom(RuntimeException erro) {
            this.falha = erro;
        }

        @Override
        public OrigemIntegracao origem() {
            return origem;
        }

        @Override
        public boolean configurada() {
            return true;
        }

        @Override
        public synchronized Pagina alteracoes(RecursoLido recurso, String desde, Long aposId) {
            if (falha != null) {
                RuntimeException erro = falha;
                falha = null;
                throw erro;
            }
            List<Item> todos = publicados.getOrDefault(recurso, List.of());
            int inicio = 0;
            for (int i = 0; i < todos.size(); i++) {
                Item item = todos.get(i);
                if (item.versao().equals(desde) && Long.valueOf(item.id()).equals(aposId)) {
                    inicio = i + 1;
                }
            }
            List<Item> depois = List.copyOf(todos.subList(inicio, todos.size()));
            Item ultimo = depois.isEmpty() ? null : depois.getLast();
            return new Pagina(depois, ultimo == null ? null : ultimo.versao(), ultimo == null ? null : ultimo.id(),
                    false);
        }
    }
}
