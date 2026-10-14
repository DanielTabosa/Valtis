package br.com.manutec.valtis.servico;

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
 * Numa ÚNICA transação: grava um serviço por válvula, fecha o ciclo anterior,
 * abre a próxima oportunidade e põe os alertas na fila da agenda.
 * Nenhuma chamada ao Google acontece aqui (D-13): se o Google estiver fora do
 * ar, o técnico vê "salvo" do mesmo jeito.
 */
@Service
public class LancamentoService {

    /**
     * O que aconteceu com cada válvula. `proximaManutencao` vem nula quando o
     * serviço lançado é mais antigo que o último já registrado: entra no
     * histórico, mas não muda o ciclo.
     */
    public record ResultadoItem(UUID servicoId, UUID valvulaId, String codigoValvula, LocalDate dataRealizada,
                                LocalDate proximaManutencao) {}

    private final ServicoRepository servicoRepository;
    private final ValvulaRepository valvulaRepository;
    private final UsuarioService usuarios;
    private final OportunidadeService oportunidades;
    private final Clock relogio;

    public LancamentoService(ServicoRepository servicoRepository, ValvulaRepository valvulaRepository,
                             UsuarioService usuarios, OportunidadeService oportunidades, Clock relogio) {
        this.servicoRepository = servicoRepository;
        this.valvulaRepository = valvulaRepository;
        this.usuarios = usuarios;
        this.oportunidades = oportunidades;
        this.relogio = relogio;
    }

    @Transactional
    public List<ResultadoItem> lancar(LancamentoRequisicao requisicao, UUID criadoPorId) {
        LocalDate hoje = LocalDate.now(relogio);
        Usuario criadoPor = usuarios.buscarAtivo(criadoPorId);
        Map<UUID, Usuario> tecnicos = new HashMap<>();
        Set<UUID> valvulasVistas = new HashSet<>();

        List<ResultadoItem> resultado = new ArrayList<>();
        for (LancamentoRequisicao.Item item : requisicao.itens()) {
            if (!valvulasVistas.add(item.valvulaId())) {
                throw new RegraDeNegocioException("A mesma válvula aparece duas vezes no lançamento.");
            }
            Usuario tecnico = tecnicos.computeIfAbsent(item.tecnicoResponsavelId(), usuarios::buscarAtivo);
            resultado.add(lancarItem(item, tecnico, criadoPor, hoje));
        }
        return resultado;
    }

    private ResultadoItem lancarItem(LancamentoRequisicao.Item item, Usuario tecnico, Usuario criadoPor,
                                     LocalDate hoje) {
        Valvula valvula = valvulaRepository.buscarCompleta(item.valvulaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Válvula não encontrada."));

        // RN-12: equipamento inativo não recebe lançamento novo.
        if (!valvula.isOperante()) {
            throw new RegraDeNegocioException(
                    "A válvula " + valvula.getCodigoReferencia() + " está inativa, ou a estação ou o cliente dela.");
        }
        if (item.dataRealizada().isAfter(hoje)) {
            throw new RegraDeNegocioException("A data realizada não pode estar no futuro.");
        }

        Optional<LocalDate> ultimaAnterior = servicoRepository.ultimaDataConcluida(valvula.getId());

        Servico servico = servicoRepository.save(new Servico(valvula, item.dataRealizada(), item.tipoManutencao(),
                tecnico, Textos.limpar(item.pecasSubstituidas()), item.pressaoEntrada(), item.pressaoSaida(),
                Textos.limpar(item.observacoes()), criadoPor));

        // Lançamento atrasado de um serviço antigo: vira histórico, mas o ciclo
        // continua contando do serviço mais recente.
        boolean ehOMaisRecente = ultimaAnterior.map(u -> !item.dataRealizada().isBefore(u)).orElse(true);
        if (!ehOMaisRecente) {
            return new ResultadoItem(servico.getId(), valvula.getId(), valvula.getCodigoReferencia(),
                    servico.getDataRealizada(), null);
        }

        Oportunidade proxima = oportunidades.abrirNovoCiclo(servico);
        if (valvula.getEstacao().getCliente().isCadastroIncompleto()) {
            valvula.getEstacao().getCliente().completarCadastro();
        }
        return new ResultadoItem(servico.getId(), valvula.getId(), valvula.getCodigoReferencia(),
                servico.getDataRealizada(), proxima.getDataPrevista());
    }
}
