package br.com.manutec.valtis.estacao;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import br.com.manutec.valtis.valvula.ValvulaResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Cadastro de estações (RF-07, RF-12). */
@Service
public class EstacaoService {

    private final EstacaoRepository estacaoRepository;
    private final ValvulaRepository valvulaRepository;
    private final ClienteService clienteService;

    public EstacaoService(EstacaoRepository estacaoRepository, ValvulaRepository valvulaRepository,
                          ClienteService clienteService) {
        this.estacaoRepository = estacaoRepository;
        this.valvulaRepository = valvulaRepository;
        this.clienteService = clienteService;
    }

    @Transactional
    public EstacaoResposta criar(UUID clienteId, DadosEstacao dados) {
        Cliente cliente = clienteService.buscar(clienteId);
        if (!cliente.isAtivo()) {
            throw new RegraDeNegocioException("Não é possível cadastrar estação em cliente inativo.");
        }
        Estacao estacao = estacaoRepository.save(new Estacao(cliente, Textos.limpar(dados.localizacaoInstalacao()),
                dados.andarInicial(), dados.andarFinal(), Boolean.TRUE.equals(dados.requerFechamentoGeral())));
        return EstacaoResposta.de(estacao, List.of());
    }

    @Transactional
    public EstacaoResposta atualizar(UUID id, DadosEstacao dados) {
        Estacao estacao = buscar(id);
        estacao.atualizar(Textos.limpar(dados.localizacaoInstalacao()), dados.andarInicial(), dados.andarFinal(),
                Boolean.TRUE.equals(dados.requerFechamentoGeral()));
        return resposta(estacao);
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public Estacao buscar(UUID id) {
        return estacaoRepository.buscarComCliente(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estação não encontrada."));
    }

    private EstacaoResposta resposta(Estacao estacao) {
        List<ValvulaResposta> valvulas = valvulaRepository.buscarDasEstacoes(List.of(estacao.getId()))
                .stream()
                .map(ValvulaResposta::de)
                .toList();
        return EstacaoResposta.de(estacao, valvulas);
    }
}
