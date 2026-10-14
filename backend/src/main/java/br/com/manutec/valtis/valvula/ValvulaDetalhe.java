package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.oportunidade.StatusOportunidade;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.servico.TipoManutencao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tudo sobre uma válvula numa tela só: onde está, situação atual,
 * histórico de serviços (RF-21) e o histórico comercial.
 */
public record ValvulaDetalhe(
        ValvulaResposta valvula,
        UUID clienteId,
        String codigoCliente,
        String nomeCondominio,
        boolean clienteAtivo,
        UUID estacaoId,
        String localizacaoInstalacao,
        String andares,
        boolean requerFechamentoGeral,
        boolean estacaoAtiva,
        LocalDate ultimaManutencao,
        LocalDate proximaManutencao,
        Long diasRestantes,
        StatusPainel status,
        String statusRotulo,
        List<ServicoDoHistorico> historico,
        List<OportunidadeDoHistorico> oportunidades
) {

    public record ServicoDoHistorico(UUID id, LocalDate dataRealizada, TipoManutencao tipoManutencao,
                                     String tecnicoResponsavel, String pecasSubstituidas, BigDecimal pressaoEntrada,
                                     BigDecimal pressaoSaida, String observacoes, String lancadoPor,
                                     OffsetDateTime lancadoEm) {}

    public record OportunidadeDoHistorico(UUID id, StatusOportunidade status, LocalDate dataPrevista,
                                          String responsavel, String motivoRecusa, OffsetDateTime encerradaEm) {}
}
