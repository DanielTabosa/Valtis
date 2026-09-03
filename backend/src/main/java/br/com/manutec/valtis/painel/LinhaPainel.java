package br.com.manutec.valtis.painel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do painel, já com status e dias restantes calculados.
 */
public record LinhaPainel(
        UUID valvulaId,
        String codigoReferencia,
        String nomeCondominio,
        String bairro,
        String localizacaoInstalacao,
        String andaresAtendidos,
        String especificacao,
        boolean requerFechamentoGeral,
        LocalDate ultimaManutencao,
        LocalDate proximaManutencao,
        Long diasRestantes,
        String status
) {
}
