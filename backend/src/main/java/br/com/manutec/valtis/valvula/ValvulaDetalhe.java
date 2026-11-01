package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.oportunidade.StatusOportunidade;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.servico.OrigemServico;
import br.com.manutec.valtis.servico.Servico;
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

    /**
     * Um serviço do histórico (RF-21). Os inativos vêm junto, com o motivo (RN-26):
     * a tela os mostra riscados. `origem` diz se veio do Jiraya ou foi lançado aqui (RF-76).
     */
    public record ServicoDoHistorico(UUID id, LocalDate dataRealizada, TipoManutencao tipoManutencao,
                                     String tecnicoResponsavel, String pecasSubstituidas, BigDecimal pressaoEntrada,
                                     BigDecimal pressaoSaida, String observacoes, String lancadoPor,
                                     OffsetDateTime lancadoEm, OrigemServico origem, Long jirayaOsId,
                                     boolean ativo, String motivoInativacao) {

        public static ServicoDoHistorico de(Servico s) {
            return new ServicoDoHistorico(s.getId(), s.getDataRealizada(), s.getTipoManutencao(), s.nomeDoTecnico(),
                    s.getPecasSubstituidas(), s.getPressaoEntrada(), s.getPressaoSaida(), s.getObservacoes(),
                    s.nomeDeQuemLancou(), s.getCriadoEm(), s.getOrigem(), s.getJirayaOsId(), s.isAtivo(),
                    s.getMotivoInativacao());
        }
    }

    public record OportunidadeDoHistorico(UUID id, StatusOportunidade status, LocalDate dataPrevista,
                                          String responsavel, String motivoRecusa, OffsetDateTime encerradaEm) {

        public static OportunidadeDoHistorico de(Oportunidade o) {
            return new OportunidadeDoHistorico(o.getId(), o.getStatus(), o.getDataPrevista(),
                    o.getResponsavel() == null ? null : o.getResponsavel().getNome(), o.getMotivoRecusa(),
                    o.getEncerradaEm());
        }
    }
}
