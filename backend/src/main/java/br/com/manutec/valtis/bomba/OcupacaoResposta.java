package br.com.manutec.valtis.bomba;

import java.time.LocalDate;
import java.util.UUID;

/** Uma linha do histórico da bomba (RF-67). Exige conjunto, cliente e registrador carregados. */
public record OcupacaoResposta(
        UUID id,
        UUID bombaId,
        String codigoBomba,
        UUID conjuntoId,
        String conjunto,
        UUID clienteId,
        String nomeCondominio,
        String numeroPosicao,
        NaturezaOcupacao natureza,
        UUID bombaSubstituidaId,
        String codigoBombaSubstituida,
        LocalDate inicio,
        LocalDate fim,
        MotivoEncerramento motivoEncerramento,
        String observacoes,
        String registradoPor,
        boolean aberta
) {

    public static OcupacaoResposta de(OcupacaoBomba o) {
        ConjuntoBombeamento c = o.getConjunto();
        Bomba substituida = o.getBombaSubstituida();
        return new OcupacaoResposta(o.getId(), o.getBomba().getId(), o.getBomba().getCodigoReferencia(),
                c.getId(), c.getLocalizacao(), c.getCliente().getId(), c.getCliente().getNomeCondominio(),
                o.getNumeroPosicao(), o.getNatureza(),
                substituida == null ? null : substituida.getId(),
                substituida == null ? null : substituida.getCodigoReferencia(),
                o.getInicio(), o.getFim(), o.getMotivoEncerramento(), o.getObservacoes(),
                o.getRegistradoPor() == null ? "Jiraya" : o.getRegistradoPor().getNome(), o.isAberta());
    }
}
