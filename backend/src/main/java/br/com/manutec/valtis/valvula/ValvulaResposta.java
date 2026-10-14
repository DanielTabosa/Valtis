package br.com.manutec.valtis.valvula;

import java.time.LocalDate;
import java.util.UUID;

/** Válvula como a API devolve. Exige a especificação já carregada. */
public record ValvulaResposta(
        UUID id,
        UUID estacaoId,
        String codigoReferencia,
        String numeroValvula,
        UUID especificacaoId,
        String especificacao,
        String numeroSerie,
        LocalDate dataInstalacao,
        int periodicidadeMeses,
        boolean ativo
) {

    public static ValvulaResposta de(Valvula v) {
        return new ValvulaResposta(v.getId(), v.getEstacao().getId(), v.getCodigoReferencia(), v.getNumeroValvula(),
                v.getEspecificacao().getId(), v.getEspecificacao().getDescricaoCompleta(), v.getNumeroSerie(),
                v.getDataInstalacao(), v.getPeriodicidadeMeses(), v.isAtivo());
    }
}
