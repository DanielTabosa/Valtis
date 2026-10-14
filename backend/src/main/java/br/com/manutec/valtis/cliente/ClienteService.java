package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.estacao.EstacaoRepository;
import br.com.manutec.valtis.estacao.EstacaoResposta;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import br.com.manutec.valtis.valvula.ValvulaResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cadastro de clientes (RF-05, RF-09, RF-11, RF-12). */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EstacaoRepository estacaoRepository;
    private final ValvulaRepository valvulaRepository;

    public ClienteService(ClienteRepository clienteRepository, EstacaoRepository estacaoRepository,
                          ValvulaRepository valvulaRepository) {
        this.clienteRepository = clienteRepository;
        this.estacaoRepository = estacaoRepository;
        this.valvulaRepository = valvulaRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResposta.Resumo> listar(String busca, boolean incluirInativos) {
        String termo = Textos.limpar(busca);
        return clienteRepository.buscar(termo == null ? "" : termo, incluirInativos)
                .stream()
                .map(ClienteResposta.Resumo::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResposta.Detalhe detalhar(UUID id) {
        Cliente cliente = buscar(id);
        List<Estacao> estacoes = estacaoRepository.findByClienteIdOrderByLocalizacaoInstalacao(id);
        if (estacoes.isEmpty()) {
            return ClienteResposta.Detalhe.de(cliente, List.of());
        }

        // Uma consulta para todas as válvulas do cliente, em vez de uma por estação.
        Map<UUID, List<ValvulaResposta>> valvulasPorEstacao = valvulaRepository
                .buscarDasEstacoes(estacoes.stream().map(Estacao::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(v -> v.getEstacao().getId(),
                        Collectors.mapping(ValvulaResposta::de, Collectors.toList())));

        List<EstacaoResposta> estacoesResposta = estacoes.stream()
                .map(e -> EstacaoResposta.de(e, valvulasPorEstacao.getOrDefault(e.getId(), List.of())))
                .toList();

        return ClienteResposta.Detalhe.de(cliente, estacoesResposta);
    }

    @Transactional
    public ClienteResposta.Detalhe criar(DadosCliente dados) {
        Cliente cliente = clienteRepository.save(new Cliente(proximoCodigo(), limpar(dados)));
        return ClienteResposta.Detalhe.de(cliente, List.of());
    }

    @Transactional
    public ClienteResposta.Detalhe atualizar(UUID id, DadosCliente dados) {
        buscar(id).atualizar(limpar(dados));
        return detalhar(id);
    }

    /** RF-12: o cliente some do painel e das listas, mas o histórico permanece. */
    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public Cliente buscar(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    /**
     * RF-09: COND-001, COND-002... sempre o maior já usado + 1.
     * A constraint UNIQUE do banco é a rede de segurança se dois cadastros
     * simultâneos calcularem o mesmo número.
     */
    private String proximoCodigo() {
        return "COND-%03d".formatted(clienteRepository.maiorNumeroDeCodigo() + 1);
    }

    private DadosCliente limpar(DadosCliente d) {
        return new DadosCliente(Textos.limpar(d.nomeCondominio()), Textos.limpar(d.endereco()),
                Textos.limpar(d.bairro()), Textos.limpar(d.cidade()), Textos.limpar(d.sindicoResponsavel()),
                Textos.limpar(d.telefoneContato()), Textos.limpar(d.emailContato()), d.tipoAtendimento(),
                d.statusContrato());
    }
}
