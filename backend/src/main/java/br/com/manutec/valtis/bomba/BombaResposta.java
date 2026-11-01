package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.recorrencia.StatusPainel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Bomba como a API devolve: o cadastro mais tudo o que se calcula sobre ela (SituacaoBomba). */
public record BombaResposta(
        UUID id,
        String codigoReferencia,
        boolean patrimonioManutec,
        UUID clienteId,
        String nomeCondominio,
        String fabricante,
        String modelo,
        BigDecimal potenciaCv,
        Integer tensaoVolts,
        BigDecimal correnteAmperes,
        String funcao,
        LocalDate dataInstalacao,
        int periodicidadeMeses,
        LocalDate ultimaManutencao,
        String descricao,
        boolean ativo,
        /** A data-base do ciclo: a mais recente entre o cadastro e o último serviço ativo (RF-80). */
        LocalDate ultimaManutencaoConsiderada,
        LocalDate proximaManutencao,
        Long diasRestantes,
        StatusPainel status,
        String statusRotulo,
        boolean dadosTecnicosIncompletos,
        List<SituacaoBomba.Posicao> posicoes,
        boolean disponivel
) {

    public static BombaResposta de(Bomba b, SituacaoBomba s) {
        return new BombaResposta(b.getId(), b.getCodigoReferencia(), b.isPatrimonioManutec(),
                b.getCliente() == null ? null : b.getCliente().getId(),
                b.getCliente() == null ? null : b.getCliente().getNomeCondominio(),
                b.getFabricante(), b.getModelo(), b.getPotenciaCv(), b.getTensaoVolts(), b.getCorrenteAmperes(),
                b.getFuncao(), b.getDataInstalacao(), b.getPeriodicidadeMeses(), b.getUltimaManutencao(),
                b.descricao(), b.isAtivo(),
                s.ultimaManutencao(), s.proximaManutencao(), s.diasRestantes(), s.status(), s.status().getRotulo(),
                s.dadosTecnicosIncompletos(), s.posicoes(), s.disponivel());
    }
}
