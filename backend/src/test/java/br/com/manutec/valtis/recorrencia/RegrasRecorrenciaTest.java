package br.com.manutec.valtis.recorrencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As regras de recorrência são o núcleo do sistema. Testadas sem banco, sem
 * Spring e sem relógio: só datas entrando e datas saindo.
 */
class RegrasRecorrenciaTest {

    private final RegrasRecorrencia regras = new RegrasRecorrencia(30, 7);

    @Nested
    @DisplayName("RN-01: próxima manutenção = data realizada + periodicidade")
    class ProximaManutencao {

        @Test
        void somaDozeMesesNoPadrao() {
            assertThat(regras.proximaManutencao(LocalDate.of(2026, 3, 10), 12))
                    .isEqualTo(LocalDate.of(2027, 3, 10));
        }

        @Test
        void respeitaPeriodicidadeDeExcecao() {
            assertThat(regras.proximaManutencao(LocalDate.of(2026, 3, 10), 6))
                    .isEqualTo(LocalDate.of(2026, 9, 10));
        }

        @Test
        void fimDeMesSemDiaCorrespondenteCaiNoUltimoDiaDoMes() {
            // 31/08 + 6 meses: fevereiro não tem dia 31. Fica no último dia de fevereiro.
            assertThat(regras.proximaManutencao(LocalDate.of(2026, 8, 31), 6))
                    .isEqualTo(LocalDate.of(2027, 2, 28));
        }

        @Test
        void vinteNoveDeFevereiroEmAnoNaoBissexto() {
            assertThat(regras.proximaManutencao(LocalDate.of(2028, 2, 29), 12))
                    .isEqualTo(LocalDate.of(2029, 2, 28));
        }
    }

    @Nested
    @DisplayName("RN-04: classificação do painel")
    class Classificacao {

        @ParameterizedTest(name = "{0} dias restantes → {1}")
        @CsvSource({
                "-365, VENCIDO",
                "-1,   VENCIDO",
                "0,    PROXIMO_DO_VENCIMENTO",
                "1,    PROXIMO_DO_VENCIMENTO",
                "30,   PROXIMO_DO_VENCIMENTO",
                "31,   EM_DIA",
                "365,  EM_DIA"
        })
        void classificaNasBordas(long diasRestantes, StatusPainel esperado) {
            assertThat(regras.classificar(diasRestantes)).isEqualTo(esperado);
        }

        @Test
        void janelaAcompanhaAConfiguracaoDoAlertaComercial() {
            RegrasRecorrencia janelaDe45 = new RegrasRecorrencia(45, 7);
            assertThat(janelaDe45.classificar(45)).isEqualTo(StatusPainel.PROXIMO_DO_VENCIMENTO);
            assertThat(janelaDe45.classificar(46)).isEqualTo(StatusPainel.EM_DIA);
        }
    }

    @Nested
    @DisplayName("RN-06: dois alertas por ciclo")
    class Alertas {

        @Test
        void alertaComercialTrintaDiasAntes() {
            assertThat(regras.dataAlertaComercial(LocalDate.of(2027, 3, 10))).isEqualTo(LocalDate.of(2027, 2, 8));
        }

        @Test
        void lembreteFinalSeteDiasAntes() {
            assertThat(regras.dataLembreteFinal(LocalDate.of(2027, 3, 10))).isEqualTo(LocalDate.of(2027, 3, 3));
        }

        @Test
        void recusaConfiguracaoComLembreteAntesDoAlerta() {
            assertThatThrownBy(() -> new RegrasRecorrencia(7, 30)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new RegrasRecorrencia(30, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
