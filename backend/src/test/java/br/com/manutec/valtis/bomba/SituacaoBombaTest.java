package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.suporte.Cenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O que se calcula sobre a bomba: vencimento (RN-16), disponibilidade (RN-17)
 * e dados técnicos incompletos (RF-63). Sem banco, sem Spring.
 */
class SituacaoBombaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
    private final RegrasRecorrencia regras = new RegrasRecorrencia(30, 7);
    private final Cliente cliente = CenarioBombas.cliente("COND-001");

    @Nested
    @DisplayName("RN-16: próxima manutenção = última + periodicidade, pela mesma regra da válvula")
    class Vencimento {

        @Test
        void bombaComUltimaManutencaoTemProximaCalculada() {
            Bomba bomba = CenarioBombas.doCondominio(cliente, LocalDate.of(2025, 11, 1));

            SituacaoBomba situacao = SituacaoBomba.calcular(bomba, List.of(), HOJE, regras);

            assertThat(situacao.proximaManutencao()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(situacao.diasRestantes()).isEqualTo(12);
            assertThat(situacao.status()).isEqualTo(StatusPainel.PROXIMO_DO_VENCIMENTO);
        }

        @Test
        void bombaVencidaTemDiasNegativos() {
            Bomba bomba = CenarioBombas.doCondominio(cliente, LocalDate.of(2025, 1, 1));

            SituacaoBomba situacao = SituacaoBomba.calcular(bomba, List.of(), HOJE, regras);

            assertThat(situacao.diasRestantes()).isNegative();
            assertThat(situacao.status()).isEqualTo(StatusPainel.VENCIDO);
        }

        @Test
        void semUltimaManutencaoEhSemRegistroENaoTemData() {
            Bomba bomba = CenarioBombas.doCondominio(cliente, null);

            SituacaoBomba situacao = SituacaoBomba.calcular(bomba, List.of(), HOJE, regras);

            assertThat(situacao.proximaManutencao()).isNull();
            assertThat(situacao.diasRestantes()).isNull();
            assertThat(situacao.status()).isEqualTo(StatusPainel.SEM_REGISTRO);
        }
    }

    @Nested
    @DisplayName("RN-17: patrimônio disponível = sem ocupação aberta")
    class Disponibilidade {

        @Test
        void patrimonialSemOcupacaoEstaDisponivel() {
            SituacaoBomba situacao = SituacaoBomba.calcular(CenarioBombas.patrimonial(), List.of(), HOJE, regras);

            assertThat(situacao.disponivel()).isTrue();
            assertThat(situacao.posicoes()).isEmpty();
        }

        @Test
        void patrimonialEmprestadaNaoEstaDisponivelEDizOndeEsta() {
            Bomba bomba = CenarioBombas.patrimonial();
            ConjuntoBombeamento conjunto = CenarioBombas.conjunto(cliente);
            OcupacaoBomba emprestimo = CenarioBombas.ocupacao(bomba, conjunto, LocalDate.of(2026, 10, 1));

            SituacaoBomba situacao = SituacaoBomba.calcular(bomba, List.of(emprestimo), HOJE, regras);

            assertThat(situacao.disponivel()).isFalse();
            assertThat(situacao.posicoes()).singleElement().satisfies(p -> {
                assertThat(p.nomeCondominio()).isEqualTo("Edifício Fictício COND-001");
                assertThat(p.natureza()).isEqualTo(NaturezaOcupacao.EMPRESTIMO);
                assertThat(p.desde()).isEqualTo(LocalDate.of(2026, 10, 1));
            });
        }

        @Test
        void bombaDoCondominioNuncaEhDisponivel() {
            // Fora de posição ela está em conserto ou guardada, não "disponível para empréstimo".
            Bomba bomba = CenarioBombas.doCondominio(cliente, null);

            assertThat(SituacaoBomba.calcular(bomba, List.of(), HOJE, regras).disponivel()).isFalse();
        }
    }

    @Nested
    @DisplayName("RF-63: dados técnicos incompletos")
    class DadosTecnicos {

        @Test
        void faltandoUmDosCincoCamposFicaMarcada() {
            Bomba bomba = Cenario.comId(Bomba.doCondominio(cliente, "COND-001-BMB-02", CenarioBombas.SEM_POTENCIA,
                    12, null));

            assertThat(SituacaoBomba.calcular(bomba, List.of(), HOJE, regras).dadosTecnicosIncompletos()).isTrue();
        }

        @Test
        void funcaoEDataDeInstalacaoNaoContamComoDadoTecnico() {
            Bomba bomba = Cenario.comId(Bomba.doCondominio(cliente, "COND-001-BMB-02",
                    new Bomba.DadosTecnicos("WEG", "BC-92", CenarioBombas.COMPLETOS.potenciaCv(), 220,
                            CenarioBombas.COMPLETOS.correnteAmperes(), null, null), 12, null));

            assertThat(SituacaoBomba.calcular(bomba, List.of(), HOJE, regras).dadosTecnicosIncompletos()).isFalse();
        }
    }
}
