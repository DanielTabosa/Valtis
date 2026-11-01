package br.com.manutec.valtis.integracao;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Um tipo renomeado no Jiraya passou a ser VRP ou bomba ("Registro" → "VRP"). Os
 * equipamentos dele que tinham ficado nas pendências como "tipo não acompanhado"
 * voltam para a fila sozinhos, sem esperar alguém clicar em reprocessar (RF-75).
 *
 * Também volta a OS que tinha ido para as pendências por não citar nenhuma VRP ou
 * bomba, quando cita um desses equipamentos: aplicada depois deles (a fila aplica
 * equipamento antes de OS), vira serviço normalmente.
 *
 * O caminho contrário (VRP ou bomba que deixam de ser) não é automático: a próxima
 * versão do equipamento vai para as pendências (AplicadorDeEquipamento, S-16).
 */
@Component
public class ReclassificacaoPorTipo {

    private final EventoRecebidoRepository eventos;
    private final AplicadorDeEquipamento equipamentos;
    private final ObjectMapper json;
    private final Clock relogio;

    public ReclassificacaoPorTipo(EventoRecebidoRepository eventos, AplicadorDeEquipamento equipamentos,
                                  ObjectMapper json, Clock relogio) {
        this.eventos = eventos;
        this.equipamentos = equipamentos;
        this.json = json;
        this.relogio = relogio;
    }

    /** Devolve quantos eventos voltaram para a fila. */
    @Transactional
    public int devolverAFilaOQuePassouAContar() {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        List<EventoRecebido> pendencias = eventos.comEstado(Set.of(EstadoEventoRecebido.PENDENCIA));

        Set<Long> passaramAContar = new HashSet<>();
        for (EventoRecebido evento : pendencias) {
            if (ignoradoPorTipo(evento)
                    && equipamentos.classificarCorpo(evento.getCorpo()) != AplicadorDeEquipamento.Classe.OUTRO) {
                evento.reprocessar(null, agora); // ninguém clicou: resolvido_por fica vazio
                passaramAContar.add(evento.getIdExterno());
            }
        }
        if (passaramAContar.isEmpty()) {
            return 0;
        }

        int devolvidos = passaramAContar.size();
        for (EventoRecebido evento : pendencias) {
            if (osSemEquipamento(evento) && citaAlgum(evento, passaramAContar)) {
                evento.reprocessar(null, agora);
                devolvidos++;
            }
        }
        return devolvidos;
    }

    private static boolean ignoradoPorTipo(EventoRecebido evento) {
        return evento.getTipo() == TipoEventoRecebido.EQUIPAMENTO && evento.getMotivo() != null
                && evento.getMotivo().startsWith(ResultadoDaAplicacao.TIPO_NAO_MAPEADO);
    }

    private static boolean osSemEquipamento(EventoRecebido evento) {
        return evento.getTipo() == TipoEventoRecebido.ORDEM_SERVICO && evento.getMotivo() != null
                && evento.getMotivo().contains(ResultadoDaAplicacao.OS_SEM_EQUIPAMENTO);
    }

    private boolean citaAlgum(EventoRecebido evento, Set<Long> equipamentosIds) {
        Contratos.OrdemDeServicoDoJiraya os = json.readValue(evento.getCorpo(), Contratos.OrdemDeServicoDoJiraya.class);
        return os.equipamentos() != null && os.equipamentos().stream()
                .map(Contratos.ItemDaOs::equipamentoId)
                .filter(Objects::nonNull)
                .anyMatch(equipamentosIds::contains);
    }
}
