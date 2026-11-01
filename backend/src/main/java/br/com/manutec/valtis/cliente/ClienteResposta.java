package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.OrigemRegistro;
import br.com.manutec.valtis.estacao.EstacaoResposta;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Respostas da API de clientes.
 * O resumo vai na listagem; o detalhe traz estações e válvulas para a tela do cliente.
 */
public final class ClienteResposta {

    private ClienteResposta() {
    }

    public record Resumo(UUID id, String codigoReferencia, String nomeCondominio, String bairro, String cidade,
                         TipoAtendimento tipoAtendimento, StatusContrato statusContrato, boolean ativo,
                         boolean cadastroIncompleto) {

        public static Resumo de(Cliente c) {
            return new Resumo(c.getId(), c.getCodigoReferencia(), c.getNomeCondominio(), c.getBairro(),
                    c.getCidade(), c.getTipoAtendimento(), c.getStatusContrato(), c.isAtivo(),
                    c.isCadastroIncompleto());
        }
    }

    public record Detalhe(UUID id, String codigoReferencia, String nomeCondominio, String endereco, String bairro,
                          String cidade, String sindicoResponsavel, String telefoneContato, String emailContato,
                          TipoAtendimento tipoAtendimento, StatusContrato statusContrato, boolean ativo,
                          OrigemRegistro origem, LocalDate ultimaManutencaoLegado, boolean dataLegadoAproximada,
                          boolean cadastroIncompleto, List<EstacaoResposta> estacoes, boolean espelhado,
                          Long manutecId) {

        public static Detalhe de(Cliente c, List<EstacaoResposta> estacoes) {
            return new Detalhe(c.getId(), c.getCodigoReferencia(), c.getNomeCondominio(), c.getEndereco(),
                    c.getBairro(), c.getCidade(), c.getSindicoResponsavel(), c.getTelefoneContato(),
                    c.getEmailContato(), c.getTipoAtendimento(), c.getStatusContrato(), c.isAtivo(), c.getOrigem(),
                    c.getUltimaManutencaoLegado(), c.isDataLegadoAproximada(), c.isCadastroIncompleto(), estacoes,
                    c.isEspelhado(), c.getManutecId());
        }
    }
}
