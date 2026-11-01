package br.com.manutec.valtis.integracao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Os corpos que as duas fontes mandam, como registros. Campos em snake_case,
 * como nos contratos. Campo desconhecido é ignorado: a fonte pode acrescentar
 * campos sem nova versão.
 */
final class Contratos {

    private Contratos() {
    }

    /** Detalhe do cliente no Manutec Clientes (INTEGRACAO_JIRAYA.md, rota 2). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClienteDoManutec(
            long id,
            String nome,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            boolean ativo,
            List<Endereco> enderecos,
            List<Contato> contatos,
            @JsonProperty("atualizado_em") String atualizadoEm) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Endereco(long id, boolean principal, boolean ativo, String logradouro, String numero,
                        String complemento, String bairro, String cidade) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Contato(String nome, String funcao, String telefone, String email) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClienteDoJiraya(long id, @JsonProperty("manutec_id") Long manutecId, String nome) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TipoDoJiraya(long id, String nome) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Substituicao(@JsonProperty("equipamento_id") long equipamentoId, String em) {}

    /** Equipamento do Jiraya (jiraya.md, rota 2). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EquipamentoDoJiraya(
            long id,
            TipoDoJiraya tipo,
            ClienteDoJiraya cliente,
            @JsonProperty("localizacao_interna") String localizacaoInterna,
            String identificacao,
            String fabricante,
            String modelo,
            @JsonProperty("numero_serie") String numeroSerie,
            Substituicao substitui,
            @JsonProperty("substituido_por") Substituicao substituidoPor,
            boolean excluido,
            @JsonProperty("atualizado_em") String atualizadoEm) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Pessoa(String nome) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItemDaOs(@JsonProperty("item_id") long itemId, @JsonProperty("equipamento_id") Long equipamentoId,
                    String descricao) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Conclusao(@JsonProperty("concluida_em") String concluidaEm,
                     @JsonProperty("concluida_por") Pessoa concluidaPor) {}

    /** Ordem de serviço do Jiraya (jiraya.md, rota 4). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OrdemDeServicoDoJiraya(
            long id,
            String situacao,
            @JsonProperty("tipo_servico") String tipoServico,
            @JsonProperty("data_execucao") LocalDate dataExecucao,
            ClienteDoJiraya cliente,
            String descricao,
            List<ItemDaOs> equipamentos,
            Conclusao conclusao,
            List<Pessoa> executores,
            @JsonProperty("atualizado_em") String atualizadoEm) {

        boolean concluida() {
            return "concluida".equals(situacao);
        }
    }
}
