package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.bomba.Bomba;
import br.com.manutec.valtis.bomba.BombaRepository;
import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.oportunidade.Oportunidade;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioService;
import br.com.manutec.valtis.valvula.Valvula;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso lancarServico (módulo M3): o coração do sistema.
 *
 * Numa ÚNICA transação: grava um serviço por equipamento, fecha o ciclo anterior,
 * abre a próxima oportunidade e põe os alertas na fila da agenda.
 * Nenhuma chamada ao Google acontece aqui (D-13): se o Google estiver fora do
 * ar, o técnico vê "salvo" do mesmo jeito.
 *
 * O serviço que vem de OS do Jiraya (RF-74) entra por `registrarDaOrdemDeServico`
 * e passa pelo MESMO `registrar`: RN-07 e RN-10 não têm segunda cópia (D-30).
 */
@Service
public class LancamentoService {

    /**
     * O que aconteceu com cada equipamento. `proximaManutencao` vem nula quando o
     * serviço lançado é mais antigo que o último já registrado: entra no
     * histórico, mas não muda o ciclo (S-03, RN-25).
     */
    public record ResultadoItem(UUID servicoId, UUID valvulaId, String codigoValvula, LocalDate dataRealizada,
                                LocalDate proximaManutencao, UUID bombaId, String codigoBomba) {
    }

    private final ServicoRepository servicoRepository;
    private final ValvulaRepository valvulaRepository;
    private final BombaRepository bombaRepository;
    private final UsuarioService usuarios;
    private final OportunidadeService oportunidades;
    private final Clock relogio;

    public LancamentoService(ServicoRepository servicoRepository, ValvulaRepository valvulaRepository,
                             BombaRepository bombaRepository, UsuarioService usuarios,
                             OportunidadeService oportunidades, Clock relogio) {
        this.servicoRepository = servicoRepository;
        this.valvulaRepository = valvulaRepository;
        this.bombaRepository = bombaRepository;
        this.usuarios = usuarios;
        this.oportunidades = oportunidades;
        this.relogio = relogio;
    }

    @Transactional
    public List<ResultadoItem> lancar(LancamentoRequisicao requisicao, UUID criadoPorId) {
        LocalDate hoje = LocalDate.now(relogio);
        Usuario criadoPor = usuarios.buscarAtivo(criadoPorId);
        Map<UUID, Usuario> tecnicos = new HashMap<>();
        Set<UUID> equipamentosVistos = new HashSet<>();

        List<ResultadoItem> resultado = new ArrayList<>();
        for (LancamentoRequisicao.Item item : requisicao.itens()) {
            if ((item.valvulaId() == null) == (item.bombaId() == null)) {
                throw new RegraDeNegocioException("Cada item do lançamento é de uma válvula ou de uma bomba.");
            }
            UUID equipamento = item.valvulaId() != null ? item.valvulaId() : item.bombaId();
            if (!equipamentosVistos.add(equipamento)) {
                throw new RegraDeNegocioException("O mesmo equipamento aparece duas vezes no lançamento.");
            }
            if (item.dataRealizada().isAfter(hoje)) {
                throw new RegraDeNegocioException("A data realizada não pode estar no futuro.");
            }
            Usuario tecnico = tecnicos.computeIfAbsent(item.tecnicoResponsavelId(), usuarios::buscarAtivo);
            Servico servico = item.valvulaId() != null
                    ? servicoDaValvula(item, tecnico, criadoPor)
                    : servicoDaBomba(item, tecnico, criadoPor);
            resultado.add(registrar(servico));
        }
        return resultado;
    }

    /**
     * RF-74: um serviço por equipamento da OS concluída. Chamado pelo aplicador da
     * integração, dentro da transação dele. Quem garante que a OS não é aplicada
     * duas vezes é o chamador (RN-23) e, por baixo, o índice único por OS e equipamento.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoItem registrarDaOrdemDeServico(Servico servico) {
        if (servico.getOrigem() != OrigemServico.JIRAYA) {
            throw new IllegalArgumentException("Só serviço vindo do Jiraya entra por aqui.");
        }
        exigirOperante(servico);
        return registrar(servico);
    }

    private Servico servicoDaValvula(LancamentoRequisicao.Item item, Usuario tecnico, Usuario criadoPor) {
        Valvula valvula = valvulaRepository.buscarCompleta(item.valvulaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Válvula não encontrada."));
        Servico servico = new Servico(valvula, item.dataRealizada(), item.tipoManutencao(),
                tecnico, Textos.limpar(item.pecasSubstituidas()), item.pressaoEntrada(), item.pressaoSaida(),
                Textos.limpar(item.observacoes()), criadoPor);
        exigirOperante(servico);
        return servico;
    }

    private Servico servicoDaBomba(LancamentoRequisicao.Item item, Usuario tecnico, Usuario criadoPor) {
        Bomba bomba = bombaRepository.buscarCompleta(item.bombaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bomba não encontrada."));
        // Sem dono não há a quem revender a manutenção: a oportunidade exige cliente (RN-07).
        // Ver P-31 nos requisitos.
        if (bomba.isPatrimonioManutec()) {
            throw new RegraDeNegocioException("A bomba " + bomba.getCodigoReferencia()
                    + " é do patrimônio da Manutec e não tem ciclo comercial. Registre a manutenção no cadastro dela.");
        }
        Servico servico = Servico.daBomba(bomba, item.dataRealizada(), item.tipoManutencao(), tecnico,
                Textos.limpar(item.pecasSubstituidas()), Textos.limpar(item.observacoes()), criadoPor);
        exigirOperante(servico);
        return servico;
    }

    /** RN-12: equipamento inativo, ou de estação ou cliente inativos, não recebe lançamento novo. */
    private static void exigirOperante(Servico servico) {
        if (servico.isDaBomba()) {
            Bomba bomba = servico.getBomba();
            if (!bomba.isAtivo() || bomba.getCliente() == null || !bomba.getCliente().isAtivo()) {
                throw new RegraDeNegocioException(
                        "A bomba " + bomba.getCodigoReferencia() + " está inativa, ou o cliente dela.");
            }
        } else if (!servico.getValvula().isOperante()) {
            throw new RegraDeNegocioException("A válvula " + servico.getValvula().getCodigoReferencia()
                    + " está inativa, ou a estação ou o cliente dela.");
        }
    }

    private ResultadoItem registrar(Servico novo) {
        Optional<LocalDate> ultimaAnterior = novo.isDaBomba()
                ? servicoRepository.ultimaDataConcluidaDaBomba(novo.getBomba().getId())
                : servicoRepository.ultimaDataConcluida(novo.getValvula().getId());

        Servico servico = servicoRepository.save(novo);

        // Lançamento atrasado de um serviço antigo: vira histórico, mas o ciclo
        // continua contando do serviço mais recente (S-03, RN-25).
        boolean ehOMaisRecente = ultimaAnterior.map(u -> !servico.getDataRealizada().isBefore(u)).orElse(true);
        if (!ehOMaisRecente) {
            return resultado(servico, null);
        }

        Oportunidade proxima = oportunidades.abrirNovoCiclo(servico);
        Cliente cliente = servico.getCliente();
        if (cliente.isCadastroIncompleto()) {
            cliente.completarCadastro();
        }
        return resultado(servico, proxima.getDataPrevista());
    }

    private static ResultadoItem resultado(Servico s, LocalDate proxima) {
        Valvula v = s.getValvula();
        Bomba b = s.getBomba();
        return new ResultadoItem(s.getId(), v == null ? null : v.getId(), v == null ? null : v.getCodigoReferencia(),
                s.getDataRealizada(), proxima, b == null ? null : b.getId(), b == null ? null : b.getCodigoReferencia());
    }
}
