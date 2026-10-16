package br.com.manutec.valtis.agenda;

import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Coloca e tira eventos da fila de saída (Outbox). Não fala com o Google:
 * só grava linhas, sempre dentro da transação de quem chamou.
 *
 * Quem fala com o Google é o SincronizadorDaAgenda, depois e em separado.
 */
@Component
public class FilaDaAgenda {

    private final EventoAgendaRepository repository;
    private final RegrasRecorrencia regras;
    private final Clock relogio;

    public FilaDaAgenda(EventoAgendaRepository repository, RegrasRecorrencia regras, Clock relogio) {
        this.repository = repository;
        this.regras = regras;
        this.relogio = relogio;
    }

    /**
     * RN-06: um evento 30 dias antes e outro 7 dias antes da data prevista.
     * Alerta cuja data já passou não entra: evento no passado só polui a agenda (RN-13).
     */
    public void enfileirarAlertas(Oportunidade oportunidade) {
        LocalDate prevista = oportunidade.getDataPrevista();
        enfileirar(oportunidade, TipoEvento.ALERTA_COMERCIAL, regras.dataAlertaComercial(prevista));
        enfileirar(oportunidade, TipoEvento.LEMBRETE_FINAL, regras.dataLembreteFinal(prevista));
    }

    /**
     * Status de evento que contam como "a agenda avisa ou já avisou". Descartado não conta:
     * é o alerta que nunca chegou ao Google.
     */
    private static final Set<StatusEvento> COM_LEMBRETE =
            EnumSet.of(StatusEvento.PENDENTE, StatusEvento.CRIADO, StatusEvento.FALHOU);

    /** Oportunidades que têm lembrete na agenda, enviado ou a caminho. Uma consulta para a fila toda. */
    public Set<UUID> oportunidadesComLembrete() {
        return repository.oportunidadesComEventoEm(COM_LEMBRETE);
    }

    public boolean temLembrete(Oportunidade oportunidade) {
        return repository.existsByOportunidadeIdAndStatusIn(oportunidade.getId(), COM_LEMBRETE);
    }

    /** O ciclo fechou antes de o evento chegar ao Google: não há mais o que avisar. */
    public void descartarPendentes(Oportunidade oportunidade, String motivo) {
        repository.findByOportunidadeId(oportunidade.getId())
                .forEach(evento -> evento.descartar(motivo));
    }

    /** A data prevista mudou: eventos ainda não enviados acompanham a nova data. */
    public void reprogramar(Oportunidade oportunidade) {
        LocalDate prevista = oportunidade.getDataPrevista();
        List<EventoAgenda> existentes = repository.findByOportunidadeId(oportunidade.getId());

        for (TipoEvento tipo : TipoEvento.values()) {
            LocalDate novaData = tipo == TipoEvento.ALERTA_COMERCIAL
                    ? regras.dataAlertaComercial(prevista)
                    : regras.dataLembreteFinal(prevista);
            existentes.stream()
                    .filter(evento -> evento.getTipo() == tipo)
                    .findFirst()
                    .ifPresentOrElse(
                            evento -> evento.reprogramar(novaData, OffsetDateTime.now(relogio)),
                            // A data antiga já tinha passado e não gerou evento; a nova pode estar no futuro.
                            () -> enfileirar(oportunidade, tipo, novaData));
        }
    }

    private void enfileirar(Oportunidade oportunidade, TipoEvento tipo, LocalDate data) {
        if (data.isBefore(LocalDate.now(relogio))) {
            return;
        }
        repository.save(new EventoAgenda(oportunidade, tipo, data, OffsetDateTime.now(relogio)));
    }
}
