package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O worker da entrada (D-30): aplica os eventos pendentes, um por transação, na
 * ordem cliente → equipamento → OS. É o SincronizadorDaAgenda ao contrário.
 *
 * - Aplicado: o evento é marcado e não roda de novo (RN-23).
 * - Pendência (negócio, inclusive RegraDeNegocioException do domínio): vai para a tela (RF-75).
 * - Aguardar (dependência que ainda vai chegar): espera e tenta de novo.
 * - Qualquer outro erro: retentativa com espera crescente; esgotada, FALHOU.
 */
@Component
public class AplicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(AplicadorDeEventos.class);
    private static final int LOTE = 200;

    private final EventoRecebidoRepository repository;
    private final AplicadorDeCliente clientes;
    private final AplicadorDeEquipamento equipamentos;
    private final AplicadorDeOrdemDeServico ordens;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final int maxTentativas;
    private final long esperaInicialSegundos;

    public AplicadorDeEventos(EventoRecebidoRepository repository, AplicadorDeCliente clientes,
                              AplicadorDeEquipamento equipamentos, AplicadorDeOrdemDeServico ordens,
                              TransactionTemplate transacao, Clock relogio, ValtisPropriedades propriedades) {
        this.repository = repository;
        this.clientes = clientes;
        this.equipamentos = equipamentos;
        this.ordens = ordens;
        this.transacao = transacao;
        this.relogio = relogio;
        ValtisPropriedades.Integracao config = propriedades.integracao();
        this.maxTentativas = config == null ? 8 : config.maxTentativas();
        this.esperaInicialSegundos = config == null ? 300 : config.esperaInicialSegundos();
    }

    /** Devolve quantos foram aplicados nesta rodada. */
    public int aplicarPendentes() {
        List<UUID> prontos = transacao.execute(s -> repository.prontos(OffsetDateTime.now(relogio), LOTE));
        int aplicados = 0;
        for (UUID id : prontos) {
            if (aplicar(id)) {
                aplicados++;
            }
        }
        return aplicados;
    }

    private boolean aplicar(UUID id) {
        try {
            Boolean aplicado = transacao.execute(s -> {
                EventoRecebido evento = repository.findById(id).orElseThrow();
                if (evento.getEstado() != EstadoEventoRecebido.PENDENTE) {
                    return false; // superado ou resolvido entre a consulta e agora
                }
                switch (evento.getTipo()) {
                    case CLIENTE -> clientes.aplicar(evento);
                    case EQUIPAMENTO -> equipamentos.aplicar(evento);
                    case ORDEM_SERVICO -> ordens.aplicar(evento);
                }
                evento.aplicado(OffsetDateTime.now(relogio));
                return true;
            });
            return Boolean.TRUE.equals(aplicado);
        } catch (ResultadoDaAplicacao.Pendencia | RegraDeNegocioException e) {
            registrar(id, evento -> evento.virarPendencia(e.getMessage()));
        } catch (ResultadoDaAplicacao.Aguardar e) {
            registrar(id, evento -> evento.aguardar(e.getMessage(), OffsetDateTime.now(relogio), maxTentativas,
                    esperaInicialSegundos));
        } catch (RuntimeException e) {
            log.warn("Falha ao aplicar o evento recebido {}: {}", id, e.toString());
            registrar(id, evento -> evento.falhar(e.toString(), OffsetDateTime.now(relogio), maxTentativas,
                    esperaInicialSegundos));
        }
        return false;
    }

    /** O resultado de um evento que não foi aplicado, numa transação nova (a da aplicação foi desfeita). */
    private void registrar(UUID id, java.util.function.Consumer<EventoRecebido> mudanca) {
        transacao.executeWithoutResult(s -> repository.findById(id).ifPresent(mudanca));
    }
}
