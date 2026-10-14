package br.com.manutec.valtis.estacao;

import br.com.manutec.valtis.valvula.ValvulaResposta;

import java.util.List;
import java.util.UUID;

/** Estação com as válvulas dela, como a tela do cliente precisa. */
public record EstacaoResposta(
        UUID id,
        UUID clienteId,
        String localizacaoInstalacao,
        int andarInicial,
        int andarFinal,
        String andares,
        boolean requerFechamentoGeral,
        boolean ativo,
        List<ValvulaResposta> valvulas
) {

    public static EstacaoResposta de(Estacao e, List<ValvulaResposta> valvulas) {
        return new EstacaoResposta(e.getId(), e.getCliente().getId(), e.getLocalizacaoInstalacao(),
                e.getAndarInicial(), e.getAndarFinal(), e.descreverAndares(), e.isRequerFechamentoGeral(),
                e.isAtivo(), valvulas);
    }
}
