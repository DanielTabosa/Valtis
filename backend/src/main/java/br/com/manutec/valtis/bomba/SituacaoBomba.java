package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.recorrencia.StatusPainel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Tudo o que se calcula sobre uma bomba e nunca se grava (D-15, D-27):
 * vencimento (RN-16), dados técnicos incompletos (RF-63), onde está e se
 * está disponível (RN-17).
 *
 * Sem banco, sem Spring, sem relógio: recebe a bomba, as ocupações abertas
 * e o "hoje". Por isso é testável com um simples `new`.
 */
public record SituacaoBomba(
        LocalDate proximaManutencao,
        Long diasRestantes,
        StatusPainel status,
        boolean dadosTecnicosIncompletos,
        List<Posicao> posicoes,
        boolean disponivel
) {

    /** Onde a bomba está agora. Mais de uma só no caso de RN-19. */
    public record Posicao(UUID ocupacaoId, UUID conjuntoId, String conjunto, UUID clienteId, String nomeCondominio,
                          String numeroPosicao, NaturezaOcupacao natureza, LocalDate desde) {

        static Posicao de(OcupacaoBomba o) {
            ConjuntoBombeamento c = o.getConjunto();
            return new Posicao(o.getId(), c.getId(), c.getLocalizacao(), c.getCliente().getId(),
                    c.getCliente().getNomeCondominio(), o.getNumeroPosicao(), o.getNatureza(), o.getInicio());
        }
    }

    public static SituacaoBomba calcular(Bomba bomba, List<OcupacaoBomba> ocupacoesAbertas, LocalDate hoje,
                                         RegrasRecorrencia regras) {
        // RN-16: mesma conta da válvula (RN-01). Sem última manutenção, não há data-base.
        LocalDate ultima = bomba.getUltimaManutencao();
        LocalDate proxima = ultima == null ? null : regras.proximaManutencao(ultima, bomba.getPeriodicidadeMeses());
        Long dias = proxima == null ? null : ChronoUnit.DAYS.between(hoje, proxima);
        StatusPainel status = dias == null ? StatusPainel.SEM_REGISTRO : regras.classificar(dias);

        List<Posicao> posicoes = ocupacoesAbertas.stream().map(Posicao::de).toList();

        // RN-17: disponível = patrimônio da Manutec sem nenhuma ocupação aberta.
        // Bomba de condomínio nunca é "disponível": ou está instalada, ou está fora (em conserto, por exemplo).
        boolean disponivel = bomba.isPatrimonioManutec() && posicoes.isEmpty();

        return new SituacaoBomba(proxima, dias, status, bomba.dadosTecnicos().incompletos(), posicoes, disponivel);
    }
}
