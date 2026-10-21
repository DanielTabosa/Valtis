package br.com.manutec.valtis.bomba;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Conjunto com quem ocupa cada posição agora, como a tela do cliente precisa. */
public record ConjuntoResposta(
        UUID id,
        UUID clienteId,
        UUID tipoId,
        String tipo,
        String localizacao,
        String observacoes,
        boolean ativo,
        List<PosicaoOcupada> posicoes
) {

    public record PosicaoOcupada(UUID ocupacaoId, String numeroPosicao, UUID bombaId, String codigoBomba,
                                 String descricaoBomba, boolean patrimonioManutec, NaturezaOcupacao natureza,
                                 LocalDate desde) {

        public static PosicaoOcupada de(OcupacaoBomba o) {
            Bomba b = o.getBomba();
            return new PosicaoOcupada(o.getId(), o.getNumeroPosicao(), b.getId(), b.getCodigoReferencia(),
                    b.descricao(), b.isPatrimonioManutec(), o.getNatureza(), o.getInicio());
        }
    }

    public static ConjuntoResposta de(ConjuntoBombeamento c, List<PosicaoOcupada> posicoes) {
        return new ConjuntoResposta(c.getId(), c.getCliente().getId(), c.getTipo().getId(), c.getTipo().getNome(),
                c.getLocalizacao(), c.getObservacoes(), c.isAtivo(), posicoes);
    }
}
