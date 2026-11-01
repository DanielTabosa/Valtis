package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.bomba.Bomba;
import br.com.manutec.valtis.bomba.BombaRepository;
import br.com.manutec.valtis.servico.LancamentoService;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.servico.ServicoRepository;
import br.com.manutec.valtis.servico.TipoManutencao;
import br.com.manutec.valtis.valvula.Valvula;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF-74: a OS concluída do Jiraya vira um serviço por equipamento acompanhado,
 * pelo MESMO LancamentoService do lançamento manual (RN-07, RN-10, RN-25).
 *
 * RN-22: só reinicia o ciclo a OS concluída de manutenção preventiva, corretiva
 * ou instalação. P-27: OS já aplicada que é reaberta, cancelada ou alterada vai
 * para as pendências; o admin decide (RN-26 para inativar o serviço).
 */
@Component
public class AplicadorDeOrdemDeServico {

    /** RN-22: os tipos de serviço do Jiraya (J-1) que reiniciam o ciclo. */
    private static final Map<String, TipoManutencao> REINICIAM_O_CICLO = Map.of(
            "manutencao_preventiva", TipoManutencao.PREVENTIVA,
            "manutencao_corretiva", TipoManutencao.CORRETIVA,
            "instalacao", TipoManutencao.INSTALACAO);

    private static final Set<String> NAO_REINICIAM = Set.of("visita_tecnica", "orcamento", "outro");

    private final ObjectMapper json;
    private final ServicoRepository servicos;
    private final ValvulaRepository valvulas;
    private final BombaRepository bombas;
    private final EventoRecebidoRepository eventos;
    private final LancamentoService lancamento;

    public AplicadorDeOrdemDeServico(ObjectMapper json, ServicoRepository servicos, ValvulaRepository valvulas,
                                     BombaRepository bombas, EventoRecebidoRepository eventos,
                                     LancamentoService lancamento) {
        this.json = json;
        this.servicos = servicos;
        this.valvulas = valvulas;
        this.bombas = bombas;
        this.eventos = eventos;
        this.lancamento = lancamento;
    }

    /** O equipamento da OS, já resolvido para o espelho do Valtis. */
    private record Alvo(Valvula valvula, Bomba bomba) {
        Object chave() { return valvula != null ? valvula.getId() : bomba.getId(); }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(EventoRecebido evento) {
        Contratos.OrdemDeServicoDoJiraya os = json.readValue(evento.getCorpo(),
                Contratos.OrdemDeServicoDoJiraya.class);
        List<Servico> jaAplicados = servicos.findByJirayaOsId(os.id());

        if (!os.concluida()) {
            if (jaAplicados.stream().anyMatch(Servico::isAtivo)) {
                throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " já tinha virado serviço no Valtis e "
                        + "agora está \"" + os.situacao() + "\" no Jiraya. Decida se mantém ou inativa o serviço.");
            }
            return; // aberta, pausada ou cancelada sem nada aplicado: não há o que fazer
        }

        TipoManutencao tipo = tipoQueReiniciaOCiclo(os);
        if (tipo == null) {
            return; // RN-22: visita, orçamento ou outro não reiniciam o ciclo
        }
        if (os.dataExecucao() == null) {
            throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " foi concluída sem data de execução (P-28).");
        }

        List<Alvo> alvos = alvos(os);
        if (!jaAplicados.isEmpty()) {
            conferirIgualAoAplicado(os, tipo, alvos, jaAplicados);
            return; // reler a mesma OS não duplica (RN-23)
        }
        String tecnico = tecnico(os);
        String observacoes = observacoes(os);
        for (Alvo alvo : alvos) {
            lancamento.registrarDaOrdemDeServico(Servico.daOrdemDeServico(alvo.valvula(), alvo.bomba(), os.id(),
                    os.dataExecucao(), tipo, tecnico, observacoes));
        }
    }

    private static TipoManutencao tipoQueReiniciaOCiclo(Contratos.OrdemDeServicoDoJiraya os) {
        if (os.tipoServico() == null) {
            throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " foi concluída sem tipo de serviço (J-1).");
        }
        TipoManutencao tipo = REINICIAM_O_CICLO.get(os.tipoServico());
        if (tipo == null && !NAO_REINICIAM.contains(os.tipoServico())) {
            // Valor novo do contrato (versionamento): ninguém decidiu ainda se reinicia o ciclo.
            throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " tem um tipo de serviço que o Valtis não "
                    + "conhece: \"" + os.tipoServico() + "\".");
        }
        return tipo;
    }

    /** Os equipamentos da OS que o Valtis acompanha. Os de tipo não acompanhado ficam de fora. */
    private List<Alvo> alvos(Contratos.OrdemDeServicoDoJiraya os) {
        List<Long> ids = os.equipamentos() == null ? List.of() : os.equipamentos().stream()
                .map(Contratos.ItemDaOs::equipamentoId).filter(Objects::nonNull).distinct().toList();
        List<Alvo> alvos = new ArrayList<>();
        for (Long id : ids) {
            Optional<Valvula> valvula = valvulas.findByJirayaEquipamentoId(id);
            Optional<Bomba> bomba = valvula.isPresent() ? Optional.empty() : bombas.findByJirayaEquipamentoId(id);
            if (valvula.isPresent() || bomba.isPresent()) {
                alvos.add(new Alvo(valvula.orElse(null), bomba.orElse(null)));
            } else if (!foiIgnoradoPorTipo(id)) {
                // O equipamento ainda está na fila (ou nem chegou): a OS espera por ele.
                throw new ResultadoDaAplicacao.Aguardar("O equipamento " + id + " do Jiraya, citado na OS " + os.id()
                        + ", ainda não chegou ao Valtis.");
            }
        }
        if (alvos.isEmpty()) {
            throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " foi concluída sem nenhuma VRP ou bomba "
                    + "cadastrada no Jiraya.");
        }
        return alvos;
    }

    /** Equipamento que não é VRP nem bomba: a pendência dele começa com TIPO_NAO_MAPEADO. */
    private boolean foiIgnoradoPorTipo(long equipamentoId) {
        return eventos.findFirstByOrigemAndTipoAndIdExternoOrderByRecebidoEmDesc(OrigemIntegracao.JIRAYA,
                        TipoEventoRecebido.EQUIPAMENTO, equipamentoId)
                .map(EventoRecebido::getMotivo)
                .filter(m -> m != null && m.startsWith(ResultadoDaAplicacao.TIPO_NAO_MAPEADO))
                .isPresent();
    }

    /** P-27: a OS foi aplicada e voltou diferente (data, tipo ou equipamentos). Uma pessoa decide. */
    private static void conferirIgualAoAplicado(Contratos.OrdemDeServicoDoJiraya os, TipoManutencao tipo,
                                                List<Alvo> alvos, List<Servico> aplicados) {
        Set<Object> esperados = alvos.stream().map(Alvo::chave).collect(Collectors.toSet());
        Set<Object> existentes = aplicados.stream()
                .map(s -> (Object) (s.isDaBomba() ? s.getBomba().getId() : s.getValvula().getId()))
                .collect(Collectors.toSet());
        boolean mesmaData = aplicados.stream().allMatch(s -> s.getDataRealizada().equals(os.dataExecucao()));
        boolean mesmoTipo = aplicados.stream().allMatch(s -> s.getTipoManutencao() == tipo);
        boolean todosAtivos = aplicados.stream().allMatch(Servico::isAtivo);
        if (!esperados.equals(existentes) || !mesmaData || !mesmoTipo || !todosAtivos) {
            throw new ResultadoDaAplicacao.Pendencia("A OS " + os.id() + " já tinha virado serviço no Valtis e voltou "
                    + "diferente do Jiraya (data, tipo ou equipamentos). Confira o serviço e decida.");
        }
    }

    /** P-29: o primeiro executor das etapas; sem executor, quem concluiu. */
    private static String tecnico(Contratos.OrdemDeServicoDoJiraya os) {
        if (os.executores() != null) {
            Optional<String> primeiro = os.executores().stream().map(Contratos.Pessoa::nome)
                    .filter(n -> n != null && !n.isBlank()).findFirst();
            if (primeiro.isPresent()) {
                return primeiro.get();
            }
        }
        return os.conclusao() == null || os.conclusao().concluidaPor() == null
                ? null : os.conclusao().concluidaPor().nome();
    }

    private static String observacoes(Contratos.OrdemDeServicoDoJiraya os) {
        String origem = "OS " + os.id() + " do Jiraya";
        return os.descricao() == null || os.descricao().isBlank() ? origem : os.descricao().strip() + " (" + origem + ")";
    }
}
