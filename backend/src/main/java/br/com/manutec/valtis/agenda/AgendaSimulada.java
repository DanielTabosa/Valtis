package br.com.manutec.valtis.agenda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Agenda de mentira, para desenvolvimento local e teste manual.
 * Registra no log o que seria criado no Google e devolve um id fictício.
 * Nunca toca a agenda real (AGENTS.md §5).
 */
@Component
@ConditionalOnProperty(name = "valtis.agenda.provedor", havingValue = "simulada")
public class AgendaSimulada implements AgendaGateway {

    private static final Logger log = LoggerFactory.getLogger(AgendaSimulada.class);

    @Override
    public String criarEvento(NovoEvento evento) {
        log.info("[agenda simulada] {} em {}", evento.titulo(), evento.data());
        return "simulado-" + evento.idExterno();
    }

    @Override
    public String nome() {
        return "simulada";
    }
}
