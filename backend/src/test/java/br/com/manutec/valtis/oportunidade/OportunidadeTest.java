package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.suporte.Cenario;
import br.com.manutec.valtis.usuario.Perfil;
import br.com.manutec.valtis.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ciclo de vida da oportunidade: RN-07, RN-08, RN-09, RN-10. */
class OportunidadeTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-13T12:00:00-03:00");

    private final Usuario ana = Cenario.usuario("Ana", Perfil.TECNICO);
    private final Usuario bruno = Cenario.usuario("Bruno", Perfil.TECNICO);
    private final Usuario admin = Cenario.usuario("Chefe", Perfil.ADMIN);

    @Test
    void nasceAgendadaESemResponsavel() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));

        // RN-07 e D-04: ninguém é dono até alguém assumir.
        assertThat(o.getStatus()).isEqualTo(StatusOportunidade.AGENDADO);
        assertThat(o.getResponsavel()).isNull();
        assertThat(o.isAberta()).isTrue();
    }

    @Test
    void qualquerUmAssumeMasNinguemTomaDeQuemJaAssumiu() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));

        o.assumir(ana, AGORA);
        o.assumir(ana, AGORA); // assumir de novo a própria não é erro

        assertThat(o.getResponsavel()).isEqualTo(ana);
        assertThatThrownBy(() -> o.assumir(bruno, AGORA))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Ana");
    }

    @Test
    void soQuemAssumiuOuAdminDevolveAFila() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));
        o.assumir(ana, AGORA);

        assertThatThrownBy(() -> o.liberar(bruno)).isInstanceOf(RegraDeNegocioException.class);

        o.liberar(admin);
        assertThat(o.getResponsavel()).isNull();
    }

    @Test
    void recusaEncerraEGuardaOMotivo() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));

        o.registrarDesfecho(StatusOportunidade.RECUSADO, "Ligou o síndico", "Sem verba", AGORA);

        assertThat(o.isAberta()).isFalse();
        assertThat(o.getMotivoRecusa()).isEqualTo("Sem verba");
        assertThat(o.getEncerradaEm()).isEqualTo(AGORA);
    }

    @Test
    void contatadoMantemAberta() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));

        o.registrarDesfecho(StatusOportunidade.CONTATADO, null, "ignorado", AGORA);

        assertThat(o.isAberta()).isTrue();
        assertThat(o.getMotivoRecusa()).isNull();
        assertThat(o.getEncerradaEm()).isNull();
    }

    @Test
    void executadoSoPorServicoLancado() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));

        // RN-10: quem fecha como executado é o lançamento do serviço, não um clique.
        assertThatThrownBy(() -> o.registrarDesfecho(StatusOportunidade.EXECUTADO, null, null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void encerradaNaoAceitaNovoDesfecho() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));
        o.registrarDesfecho(StatusOportunidade.PERDIDO, null, null, AGORA);

        assertThatThrownBy(() -> o.registrarDesfecho(StatusOportunidade.CONTATADO, null, null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> o.assumir(ana, AGORA)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void retornoDeRecusaApontaParaAMesmaValvulaComNovaData() {
        Oportunidade recusada = Cenario.oportunidade(LocalDate.of(2027, 3, 10));
        recusada.registrarDesfecho(StatusOportunidade.RECUSADO, null, null, AGORA);

        // RN-09: o cliente pediu para ser lembrado em junho.
        Oportunidade retorno = Oportunidade.retornoDe(recusada, LocalDate.of(2027, 6, 1));

        assertThat(retorno.getValvula()).isSameAs(recusada.getValvula());
        assertThat(retorno.getDataPrevista()).isEqualTo(LocalDate.of(2027, 6, 1));
        assertThat(retorno.getOportunidadeAnterior()).isSameAs(recusada);
        assertThat(retorno.isAberta()).isTrue();
    }

    @Test
    void canceladaEncerraGuardaOMotivoENaoEhDesfechoManual() {
        Oportunidade o = Cenario.oportunidade(LocalDate.of(2027, 3, 10));
        o.registrarDesfecho(StatusOportunidade.CONTATADO, "Síndico pediu proposta", null, AGORA);

        // RN-26, P-32: cancelado só vem da inativação do serviço de origem.
        assertThatThrownBy(() -> o.registrarDesfecho(StatusOportunidade.CANCELADO, null, null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class);

        o.cancelar("Cancelada: o serviço de origem foi inativado (OS cancelada).", AGORA);

        assertThat(o.getStatus()).isEqualTo(StatusOportunidade.CANCELADO);
        assertThat(o.isAberta()).isFalse();
        assertThat(o.getEncerradaEm()).isEqualTo(AGORA);
        assertThat(o.getNotas()).startsWith("Síndico pediu proposta").contains("OS cancelada");
        assertThatThrownBy(() -> o.cancelar("de novo", AGORA)).isInstanceOf(RegraDeNegocioException.class);
    }
}
