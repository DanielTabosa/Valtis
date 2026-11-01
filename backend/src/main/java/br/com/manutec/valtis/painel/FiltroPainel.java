package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.recorrencia.StatusPainel;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Filtros do painel (RF-38). Todos opcionais.
 * `busca` procura em condomínio, código, bairro e número de série (RF-11).
 * `tecnico` é quem fez o último serviço ativo. `vencimentoDe`/`vencimentoAte`
 * recortam a próxima manutenção (inclusive nas pontas); válvula SEM REGISTRO
 * não tem vencimento e sai quando o período é informado.
 */
public record FiltroPainel(
        String busca,
        StatusPainel status,
        String bairro,
        String tipoAtendimento,
        UUID clienteId,
        String tecnico,
        LocalDate vencimentoDe,
        LocalDate vencimentoAte
) {

    public static FiltroPainel vazio() {
        return new FiltroPainel(null, null, null, null, null, null, null, null);
    }

    boolean aceitaSituacao(LinhaPainel linha) {
        return status == null || linha.status() == status;
    }

    boolean aceitaIgnorandoSituacao(LinhaPainel linha) {
        return (clienteId == null || clienteId.equals(linha.clienteId()))
                && (vazio(bairro) || bairro.equalsIgnoreCase(linha.bairro()))
                && (vazio(tipoAtendimento) || tipoAtendimento.equalsIgnoreCase(linha.tipoAtendimento()))
                && (vazio(tecnico) || tecnico.equalsIgnoreCase(linha.ultimoTecnico()))
                && dentroDoPeriodo(linha.proximaManutencao())
                && (vazio(busca) || contemBusca(linha));
    }

    private boolean dentroDoPeriodo(LocalDate vencimento) {
        if (vencimentoDe == null && vencimentoAte == null) {
            return true;
        }
        return vencimento != null
                && (vencimentoDe == null || !vencimento.isBefore(vencimentoDe))
                && (vencimentoAte == null || !vencimento.isAfter(vencimentoAte));
    }

    private boolean contemBusca(LinhaPainel linha) {
        String termo = busca.strip().toLowerCase(Locale.ROOT);
        return contem(linha.nomeCondominio(), termo)
                || contem(linha.codigoReferencia(), termo)
                || contem(linha.bairro(), termo)
                || contem(linha.numeroSerie(), termo);
    }

    private static boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
