package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.valvula.PainelProjecao;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * O painel com o repositório como dublê (Mockito) e o relógio parado (Clock.fixed).
 * Hoje, para estes testes, é sempre 13/10/2026 em Recife.
 */
@ExtendWith(MockitoExtension.class)
class PainelServiceTest {

    private static final ZoneId RECIFE = ZoneId.of("America/Recife");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 13);

    @Mock
    private ValvulaRepository repository;

    private PainelService painel;

    @BeforeEach
    void preparar() {
        Clock meioDiaEmRecife = Clock.fixed(HOJE.atTime(12, 0).atZone(RECIFE).toInstant(), RECIFE);
        painel = new PainelService(repository, new RegrasRecorrencia(30, 7), meioDiaEmRecife);
    }

    @Test
    void calculaVencimentoEDiasRestantesAPartirDoUltimoServico() {
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(valvula("VRP-01", HOJE.minusMonths(12).plusDays(10))));

        LinhaPainel linha = painel.montar(FiltroPainel.vazio()).linhas().getFirst();

        assertThat(linha.proximaManutencao()).isEqualTo(HOJE.plusDays(10));
        assertThat(linha.diasRestantes()).isEqualTo(10);
        assertThat(linha.status()).isEqualTo(StatusPainel.PROXIMO_DO_VENCIMENTO);
        assertThat(linha.statusRotulo()).isEqualTo("PRÓXIMO DO VENCIMENTO");
    }

    @Test
    void valvulaSemServicoFicaSemRegistroESemVencimento() {
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(valvula("VRP-01", null)));

        LinhaPainel linha = painel.montar(FiltroPainel.vazio()).linhas().getFirst();

        // RN-05: sem data-base, nada de vencimento calculado.
        assertThat(linha.status()).isEqualTo(StatusPainel.SEM_REGISTRO);
        assertThat(linha.proximaManutencao()).isNull();
        assertThat(linha.diasRestantes()).isNull();
    }

    @Test
    void ordenaPorUrgenciaEContaTodasAsSituacoes() {
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(
                valvula("EM-DIA", HOJE.minusMonths(1)),
                valvula("SEM-REGISTRO", null),
                valvula("VENCIDO-HA-POUCO", HOJE.minusMonths(12).minusDays(2)),
                valvula("VENCIDO-HA-MUITO", HOJE.minusMonths(30))));

        PainelService.Painel resultado = painel.montar(FiltroPainel.vazio());

        assertThat(resultado.linhas()).extracting(LinhaPainel::codigoReferencia)
                .containsExactly("VENCIDO-HA-MUITO", "VENCIDO-HA-POUCO", "SEM-REGISTRO", "EM-DIA");
        assertThat(resultado.resumo()).extracting(PainelService.ContagemStatus::total)
                .containsExactly(2L, 0L, 1L, 1L);
    }

    @Test
    void filtroPorStatusRestringeLinhasEResumo() {
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(
                valvula("EM-DIA", HOJE.minusMonths(1)),
                valvula("VENCIDO", HOJE.minusMonths(14))));

        PainelService.Painel resultado = painel.montar(
                new FiltroPainel(null, StatusPainel.VENCIDO, null, null, null));

        assertThat(resultado.linhas()).extracting(LinhaPainel::codigoReferencia).containsExactly("VENCIDO");
        assertThat(resultado.resumo().getFirst().total()).isEqualTo(1);
    }

    @Test
    void buscaEncontraPorNumeroDeSerie() {
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(
                valvula("A", HOJE.minusMonths(1)),
                valvula("B", HOJE.minusMonths(1))));

        PainelService.Painel resultado = painel.montar(new FiltroPainel("serie-b", null, null, null, null));

        assertThat(resultado.linhas()).extracting(LinhaPainel::codigoReferencia).containsExactly("B");
    }

    /**
     * O bug que motivou o Clock injetado: às 23h30 em Recife já é dia seguinte
     * em UTC. O painel precisa contar a partir do dia de Recife.
     */
    @Test
    void hojeEhODiaDeRecifeMesmoQuandoEmUtcJaEhAmanha() {
        Instant tardeDaNoiteEmRecife = HOJE.atTime(23, 30).atZone(RECIFE).toInstant();
        assertThat(tardeDaNoiteEmRecife.atZone(ZoneId.of("UTC")).toLocalDate()).isEqualTo(HOJE.plusDays(1));

        PainelService painelDeNoite = new PainelService(repository, new RegrasRecorrencia(30, 7),
                Clock.fixed(tardeDaNoiteEmRecife, RECIFE));
        when(repository.buscarDadosDoPainel()).thenReturn(List.of(valvula("VRP-01", HOJE.minusMonths(12))));

        LinhaPainel linha = painelDeNoite.montar(FiltroPainel.vazio()).linhas().getFirst();

        // Vence hoje: 0 dias, e não -1 (que viraria VENCIDO por engano).
        assertThat(linha.diasRestantes()).isZero();
        assertThat(linha.status()).isEqualTo(StatusPainel.PROXIMO_DO_VENCIMENTO);
    }

    private static PainelProjecao valvula(String codigo, LocalDate ultimaManutencao) {
        return new PainelProjecao() {
            private final UUID id = UUID.randomUUID();
            public UUID getValvulaId() { return id; }
            public UUID getEstacaoId() { return id; }
            public UUID getClienteId() { return id; }
            public String getCodigoReferencia() { return codigo; }
            public String getNumeroValvula() { return "01"; }
            public String getNumeroSerie() { return "serie-" + codigo.toLowerCase(); }
            public Integer getPeriodicidadeMeses() { return 12; }
            public String getNomeCondominio() { return "Edifício " + codigo; }
            public String getBairro() { return "Boa Viagem"; }
            public String getTipoAtendimento() { return "Avulso"; }
            public String getLocalizacaoInstalacao() { return "Barrilete"; }
            public Integer getAndarInicial() { return 0; }
            public Integer getAndarFinal() { return 4; }
            public Boolean getRequerFechamentoGeral() { return false; }
            public String getEspecificacao() { return "Bermad 720 - 2\" Esfera"; }
            public LocalDate getUltimaManutencao() { return ultimaManutencao; }
        };
    }
}
