package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.valvula.Valvula;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Escreve o texto do evento (RF-29): o técnico tem que conseguir agir só com
 * o que lê no celular, sem abrir o sistema.
 *
 * Não leva telefone nem nome do síndico: a agenda é um serviço de terceiro (LGPD, AGENTS.md §8).
 */
@Component
public class MontadorDeEvento {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String urlFrontend;
    private final List<String> convidados;

    public MontadorDeEvento(ValtisPropriedades propriedades) {
        this.urlFrontend = propriedades.urlFrontend();
        ValtisPropriedades.Google google = propriedades.agenda().google();
        this.convidados = google == null || google.convidados() == null ? List.of() : google.convidados();
    }

    public AgendaGateway.NovoEvento montar(EventoAgenda evento) {
        Oportunidade o = evento.getOportunidade();
        Cliente cliente = o.getCliente();
        Valvula valvula = o.getValvula();

        String alvo = valvula == null ? cliente.getCodigoReferencia() : valvula.getCodigoReferencia();
        String titulo = "[Valtis] %s: %s (%s)".formatted(evento.getTipo().titulo(), cliente.getNomeCondominio(), alvo);

        List<String> linhas = new ArrayList<>();
        linhas.add("Condomínio: %s (%s)%s".formatted(cliente.getNomeCondominio(), cliente.getCodigoReferencia(),
                cliente.getBairro() == null ? "" : " - " + cliente.getBairro()));
        if (valvula == null) {
            linhas.add("Válvulas ainda não cadastradas: cliente vindo da base antiga.");
        } else {
            Estacao estacao = valvula.getEstacao();
            linhas.add("Válvula: " + valvula.getCodigoReferencia());
            linhas.add("Estação: %s - andares %s".formatted(estacao.getLocalizacaoInstalacao(),
                    estacao.descreverAndares()));
            linhas.add("Exige fechamento geral: " + (estacao.isRequerFechamentoGeral() ? "SIM" : "não"));
        }
        linhas.add("Última manutenção: " + formatar(ultimaManutencao(o)));
        linhas.add("Manutenção prevista: " + formatar(o.getDataPrevista()));
        linhas.add("Abrir no Valtis: " + link(o));

        return new AgendaGateway.NovoEvento(evento.idExterno(), evento.getDataEvento(), titulo,
                String.join("\n", linhas), convidados);
    }

    private static LocalDate ultimaManutencao(Oportunidade o) {
        if (o.getServicoOrigem() != null) {
            return o.getServicoOrigem().getDataRealizada();
        }
        return o.getCliente().getUltimaManutencaoLegado();
    }

    private String link(Oportunidade o) {
        return o.getValvula() == null
                ? "%s/clientes/%s".formatted(urlFrontend, o.getCliente().getId())
                : "%s/valvulas/%s".formatted(urlFrontend, o.getValvula().getId());
    }

    private static String formatar(LocalDate data) {
        return data == null ? "não registrada" : DATA.format(data);
    }
}
