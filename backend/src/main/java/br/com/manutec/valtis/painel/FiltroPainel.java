package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.recorrencia.StatusPainel;

import java.util.Locale;
import java.util.UUID;

/**
 * Filtros do painel (RF-38). Todos opcionais.
 * `busca` procura em condomínio, código, bairro e número de série (RF-11).
 */
public record FiltroPainel(
        String busca,
        StatusPainel status,
        String bairro,
        String tipoAtendimento,
        UUID clienteId
) {

    public static FiltroPainel vazio() {
        return new FiltroPainel(null, null, null, null, null);
    }

    boolean aceita(LinhaPainel linha) {
        return (status == null || linha.status() == status)
                && (clienteId == null || clienteId.equals(linha.clienteId()))
                && (vazio(bairro) || bairro.equalsIgnoreCase(linha.bairro()))
                && (vazio(tipoAtendimento) || tipoAtendimento.equalsIgnoreCase(linha.tipoAtendimento()))
                && (vazio(busca) || contemBusca(linha));
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
