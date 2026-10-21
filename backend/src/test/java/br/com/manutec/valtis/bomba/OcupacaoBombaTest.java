package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RN-18 e RN-20: a ocupação encerra uma vez, com o motivo certo, e nunca se reescreve. */
class OcupacaoBombaTest {

    private final Cliente cliente = CenarioBombas.cliente("COND-001");
    private final ConjuntoBombeamento conjunto = CenarioBombas.conjunto(cliente);
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);

    @Test
    void bombaPatrimonialEntraComoEmprestimoESaiPorDevolucao() {
        OcupacaoBomba ocupacao = CenarioBombas.ocupacao(CenarioBombas.patrimonial(), conjunto, inicio);
        assertThat(ocupacao.getNatureza()).isEqualTo(NaturezaOcupacao.EMPRESTIMO);

        ocupacao.encerrar(LocalDate.of(2026, 10, 15), null);

        assertThat(ocupacao.isAberta()).isFalse();
        assertThat(ocupacao.getMotivoEncerramento()).isEqualTo(MotivoEncerramento.DEVOLUCAO);
    }

    @Test
    void bombaDoCondominioEntraComoInstalacaoESaiPorRetirada() {
        OcupacaoBomba ocupacao = CenarioBombas.ocupacao(CenarioBombas.doCondominio(cliente, null), conjunto, inicio);
        assertThat(ocupacao.getNatureza()).isEqualTo(NaturezaOcupacao.INSTALACAO);

        ocupacao.encerrar(inicio, "foi para conserto");

        assertThat(ocupacao.getMotivoEncerramento()).isEqualTo(MotivoEncerramento.RETIRADA);
        assertThat(ocupacao.getObservacoes()).isEqualTo("foi para conserto");
    }

    @Test
    void naoEncerraDuasVezes() {
        OcupacaoBomba ocupacao = CenarioBombas.ocupacao(CenarioBombas.patrimonial(), conjunto, inicio);
        ocupacao.encerrar(LocalDate.of(2026, 10, 15), null);

        assertThatThrownBy(() -> ocupacao.encerrar(LocalDate.of(2026, 10, 20), null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já foi encerrada");
        assertThat(ocupacao.getFim()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void saidaAntesDaEntradaEhRecusada() {
        OcupacaoBomba ocupacao = CenarioBombas.ocupacao(CenarioBombas.patrimonial(), conjunto, inicio);

        assertThatThrownBy(() -> ocupacao.encerrar(inicio.minusDays(1), null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(ocupacao.isAberta()).isTrue();
    }

    @Test
    void observacaoDoEncerramentoSeSomaAoQueJaHavia() {
        OcupacaoBomba ocupacao = CenarioBombas.ocupacao(CenarioBombas.patrimonial(), conjunto, inicio);
        ocupacao.encerrar(inicio.plusDays(3), "devolvida limpa");

        assertThat(ocupacao.getObservacoes()).isEqualTo("devolvida limpa");
    }
}
