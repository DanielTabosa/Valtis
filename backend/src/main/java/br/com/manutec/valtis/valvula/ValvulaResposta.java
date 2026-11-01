package br.com.manutec.valtis.valvula;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Válvula como a API devolve. Exige a especificação já carregada, quando houver.
 * `espelhada`: veio do Jiraya; os campos da fonte são só leitura (RN-24).
 */
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
        boolean ativo,
        boolean espelhada,
        String fabricante,
        String modelo,
        String identificacaoExterna
) {

    public static ValvulaResposta de(Valvula v) {
        return new ValvulaResposta(v.getId(), v.getEstacao().getId(), v.getCodigoReferencia(), v.getNumeroValvula(),
                v.getEspecificacao() == null ? null : v.getEspecificacao().getId(), v.descricaoTecnica(),
                v.getNumeroSerie(), v.getDataInstalacao(), v.getPeriodicidadeMeses(), v.isAtivo(), v.isEspelhada(),
                v.getFabricante(), v.getModelo(), v.getIdentificacaoExterna());
    }
}
