package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cadastro de conjuntos de bombeamento (RF-61). */
@Service
public class ConjuntoService {

    private final ConjuntoRepository conjuntoRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final TipoBombaService tipoBombaService;
    private final ClienteService clienteService;

    public ConjuntoService(ConjuntoRepository conjuntoRepository, OcupacaoRepository ocupacaoRepository,
                           TipoBombaService tipoBombaService, ClienteService clienteService) {
        this.conjuntoRepository = conjuntoRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.tipoBombaService = tipoBombaService;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<ConjuntoResposta> doCliente(UUID clienteId) {
        List<ConjuntoBombeamento> conjuntos = conjuntoRepository.doCliente(clienteId);
        if (conjuntos.isEmpty()) {
            return List.of();
        }
        // Uma consulta para as posições de todos os conjuntos do cliente.
        Map<UUID, List<ConjuntoResposta.PosicaoOcupada>> posicoes = ocupacaoRepository
                .abertasDosConjuntos(conjuntos.stream().map(ConjuntoBombeamento::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(o -> o.getConjunto().getId(),
                        Collectors.mapping(ConjuntoResposta.PosicaoOcupada::de, Collectors.toList())));
        return conjuntos.stream()
                .map(c -> ConjuntoResposta.de(c, posicoes.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ConjuntoResposta criar(UUID clienteId, DadosConjunto dados) {
        Cliente cliente = clienteService.buscar(clienteId);
        if (!cliente.isAtivo()) {
            throw new RegraDeNegocioException("Não é possível cadastrar conjunto em cliente inativo.");
        }
        ConjuntoBombeamento conjunto = conjuntoRepository.save(new ConjuntoBombeamento(cliente,
                tipoAtivo(dados.tipoId()), Textos.limpar(dados.localizacao()), Textos.limpar(dados.observacoes())));
        return ConjuntoResposta.de(conjunto, List.of());
    }

    @Transactional
    public ConjuntoResposta atualizar(UUID id, DadosConjunto dados) {
        ConjuntoBombeamento conjunto = buscar(id);
        TipoBomba tipo = conjunto.getTipo().getId().equals(dados.tipoId())
                ? conjunto.getTipo()
                : tipoAtivo(dados.tipoId());
        conjunto.atualizar(tipo, Textos.limpar(dados.localizacao()), Textos.limpar(dados.observacoes()));
        return ConjuntoResposta.de(conjunto, ocupacaoRepository.abertasDosConjuntos(List.of(id)).stream()
                .map(ConjuntoResposta.PosicaoOcupada::de).toList());
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public ConjuntoBombeamento buscar(UUID id) {
        return conjuntoRepository.buscarCompleto(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conjunto de bombeamento não encontrado."));
    }

    private TipoBomba tipoAtivo(UUID id) {
        TipoBomba tipo = tipoBombaService.buscar(id);
        if (!tipo.isAtivo()) {
            throw new RegraDeNegocioException("O tipo escolhido está inativo.");
        }
        return tipo;
    }
}
