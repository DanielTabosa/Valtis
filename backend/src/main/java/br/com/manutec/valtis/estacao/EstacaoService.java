package br.com.manutec.valtis.estacao;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.ModoDeOperacao;
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
        Estacao estacao = buscarPropria(id);
        estacao.atualizar(Textos.limpar(dados.localizacaoInstalacao()), dados.andarInicial(), dados.andarFinal(),
                Boolean.TRUE.equals(dados.requerFechamentoGeral()));
        return resposta(estacao);
    }

    /** Q-2, RN-24 por campo: andares e fechamento geral são do Valtis, inclusive na derivada. */
    @Transactional
    public EstacaoResposta completar(UUID id, Integer andarInicial, Integer andarFinal, boolean requerFechamentoGeral) {
        if ((andarInicial == null) != (andarFinal == null)) {
            throw new RegraDeNegocioException("Informe os dois andares, o inicial e o final.");
        }
        if (andarInicial != null && andarFinal < andarInicial) {
            throw new RegraDeNegocioException("O andar final não pode ser abaixo do andar inicial.");
        }
        Estacao estacao = buscar(id);
        if (!estacao.isDerivada() && andarInicial == null) {
            throw new RegraDeNegocioException("Informe o andar inicial e o final.");
        }
        estacao.completar(andarInicial, andarFinal, requerFechamentoGeral);
        return resposta(estacao);
    }

    @Transactional
    public void inativar(UUID id) {
        buscarPropria(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscarPropria(id).reativar();
    }

    /** A estação derivada acompanha os equipamentos do Jiraya: localização e situação vêm de lá. */
    private Estacao buscarPropria(UUID id) {
        Estacao estacao = buscar(id);
        ModoDeOperacao.exigirNaoEspelhado(estacao.isDerivada(), "a estação", ModoDeOperacao.JIRAYA);
        return estacao;
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
