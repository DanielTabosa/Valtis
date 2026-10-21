package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.DadosCliente;
import br.com.manutec.valtis.cliente.TipoAtendimento;
import br.com.manutec.valtis.suporte.Cenario;
import br.com.manutec.valtis.usuario.Perfil;
import br.com.manutec.valtis.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objetos fictícios do módulo de bombas para os testes unitários, sem banco. */
final class CenarioBombas {

    static final Bomba.DadosTecnicos COMPLETOS = new Bomba.DadosTecnicos("WEG", "BC-92", new BigDecimal("3"), 220,
            new BigDecimal("9.5"), "principal", LocalDate.of(2024, 5, 10));

    static final Bomba.DadosTecnicos SEM_POTENCIA = new Bomba.DadosTecnicos("WEG", "BC-92", null, 220,
            new BigDecimal("9.5"), null, null);

    private CenarioBombas() {
    }

    static Cliente cliente(String codigo) {
        return Cenario.comId(new Cliente(codigo, new DadosCliente("Edifício Fictício " + codigo, null, "Pina",
                "Recife", null, null, null, TipoAtendimento.AVULSO, null)));
    }

    static ConjuntoBombeamento conjunto(Cliente cliente) {
        TipoBomba tipo = Cenario.comId(new TipoBomba("Recalque"));
        return Cenario.comId(new ConjuntoBombeamento(cliente, tipo, "Casa de bombas", null));
    }

    static Bomba doCondominio(Cliente cliente, LocalDate ultimaManutencao) {
        return Cenario.comId(Bomba.doCondominio(cliente, cliente.getCodigoReferencia() + "-BMB-01", COMPLETOS, 12,
                ultimaManutencao));
    }

    static Bomba patrimonial() {
        return Cenario.comId(Bomba.patrimonial("MAN-BMB-001", COMPLETOS, 12, null));
    }

    static OcupacaoBomba ocupacao(Bomba bomba, ConjuntoBombeamento conjunto, LocalDate inicio) {
        Usuario tecnico = Cenario.usuario("Técnico Fictício", Perfil.TECNICO);
        return Cenario.comId(new OcupacaoBomba(bomba, conjunto, "01", inicio, null, null, tecnico));
    }
}
