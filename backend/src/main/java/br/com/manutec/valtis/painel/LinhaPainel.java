package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.recorrencia.StatusPainel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do painel, já com status e dias restantes calculados.
 * `status` é o código estável (VENCIDO, PROXIMO_DO_VENCIMENTO...);
 * `statusRotulo` é o texto da planilha, pronto para exibir.
 */
public record LinhaPainel(
        UUID valvulaId,
        UUID estacaoId,
        UUID clienteId,
        String codigoReferencia,
        String nomeCondominio,
        String bairro,
        String tipoAtendimento,
        String localizacaoInstalacao,
        String andaresAtendidos,
        String especificacao,
        String numeroSerie,
        boolean requerFechamentoGeral,
        LocalDate ultimaManutencao,
        LocalDate proximaManutencao,
        Long diasRestantes,
        StatusPainel status,
        String statusRotulo
) {
}
