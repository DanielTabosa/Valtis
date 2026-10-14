package br.com.manutec.valtis.suporte;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.DadosCliente;
import br.com.manutec.valtis.cliente.TipoAtendimento;
import br.com.manutec.valtis.especificacao.Especificacao;
import br.com.manutec.valtis.especificacao.TipoRegistro;
import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.servico.TipoManutencao;
import br.com.manutec.valtis.usuario.Perfil;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.valvula.Valvula;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Monta objetos do domínio para testes unitários, com dados fictícios
 * (AGENTS.md §5: nunca dado real de cliente).
 *
 * O id normalmente é gerado pelo banco; aqui é atribuído à mão, porque não há banco.
 */
public final class Cenario {

    private Cenario() {
    }

    public static Usuario usuario(String nome, Perfil perfil) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase().replace(' ', '.') + "@teste.local", "hash", perfil);
        return comId(usuario);
    }

    public static Valvula valvula() {
        Cliente cliente = comId(new Cliente("COND-001", new DadosCliente("Edifício Fictício", null, "Pina", "Recife",
                null, null, null, TipoAtendimento.AVULSO, null)));
        Estacao estacao = comId(new Estacao(cliente, "Barrilete", 0, 4, true));
        Especificacao especificacao = comId(new Especificacao("Bermad", "720", "2", TipoRegistro.ESFERA));
        return comId(new Valvula(estacao, "COND-001-VRP-01", "01", especificacao, null, null, 12));
    }

    public static Servico servico(Valvula valvula, LocalDate dataRealizada) {
        Usuario tecnico = usuario("Técnico Fictício", Perfil.TECNICO);
        return comId(new Servico(valvula, dataRealizada, TipoManutencao.PREVENTIVA, tecnico, null, null, null, null,
                tecnico));
    }

    public static Oportunidade oportunidade(LocalDate dataPrevista) {
        Valvula valvula = valvula();
        return comId(Oportunidade.doServico(servico(valvula, dataPrevista.minusMonths(12)), dataPrevista));
    }

    public static <T> T comId(T entidade) {
        ReflectionTestUtils.setField(entidade, "id", UUID.randomUUID());
        return entidade;
    }
}
