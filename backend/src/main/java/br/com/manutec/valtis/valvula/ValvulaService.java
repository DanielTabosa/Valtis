package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.especificacao.Especificacao;
import br.com.manutec.valtis.especificacao.EspecificacaoService;
import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.estacao.EstacaoService;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import br.com.manutec.valtis.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/** Cadastro de válvulas (RF-08, RF-09, RF-12, RF-13). */
@Service
public class ValvulaService {

    private final ValvulaRepository valvulaRepository;
    private final EstacaoService estacaoService;
    private final EspecificacaoService especificacaoService;
    private final ServicoRepository servicoRepository;
    private final OportunidadeService oportunidadeService;

    public ValvulaService(ValvulaRepository valvulaRepository, EstacaoService estacaoService,
                          EspecificacaoService especificacaoService, ServicoRepository servicoRepository,
                          OportunidadeService oportunidadeService) {
        this.valvulaRepository = valvulaRepository;
        this.estacaoService = estacaoService;
        this.especificacaoService = especificacaoService;
        this.servicoRepository = servicoRepository;
        this.oportunidadeService = oportunidadeService;
    }

    @Transactional
    public ValvulaResposta criar(UUID estacaoId, DadosValvula dados) {
        Estacao estacao = estacaoService.buscar(estacaoId);
        Cliente cliente = estacao.getCliente();
        if (!estacao.isAtivo() || !cliente.isAtivo()) {
            throw new RegraDeNegocioException("Não é possível cadastrar válvula em estação ou cliente inativo.");
        }
        Especificacao especificacao = especificacaoAtiva(dados.especificacaoId());

        String numero = dados.numeroValvula() == null || dados.numeroValvula().isBlank()
                ? formatarNumero(valvulaRepository.maiorNumeroNaEstacao(estacaoId) + 1)
                : formatarNumero(Integer.parseInt(dados.numeroValvula().strip()));
        garantirNumeroLivre(estacaoId, numero, null);

        Valvula valvula = valvulaRepository.save(new Valvula(estacao, proximoCodigo(cliente), numero, especificacao,
                Textos.limpar(dados.numeroSerie()), dados.dataInstalacao(), dados.periodicidadeMeses()));
        return ValvulaResposta.de(valvula);
    }

    @Transactional
    public ValvulaResposta atualizar(UUID id, DadosValvula dados) {
        Valvula valvula = buscar(id);
        Especificacao especificacao = Objects.equals(valvula.getEspecificacao().getId(), dados.especificacaoId())
                ? valvula.getEspecificacao()
                : especificacaoAtiva(dados.especificacaoId());

        String numero = dados.numeroValvula() == null || dados.numeroValvula().isBlank()
                ? valvula.getNumeroValvula()
                : formatarNumero(Integer.parseInt(dados.numeroValvula().strip()));
        garantirNumeroLivre(valvula.getEstacao().getId(), numero, id);

        int periodicidadeAnterior = valvula.getPeriodicidadeMeses();
        valvula.atualizar(numero, especificacao, Textos.limpar(dados.numeroSerie()), dados.dataInstalacao(),
                dados.periodicidadeMeses());

        // RN-02: mudou a periodicidade, o vencimento do ciclo aberto muda junto.
        if (periodicidadeAnterior != valvula.getPeriodicidadeMeses()) {
            servicoRepository.ultimaDataConcluida(id)
                    .ifPresent(ultima -> oportunidadeService.reprogramarCicloDaValvula(valvula, ultima));
        }
        return ValvulaResposta.de(valvula);
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    /**
     * RF-12a: exclusão física só para o cadastro feito por engano, sem nenhum
     * serviço nem ciclo. Com histórico, o caminho é inativar (RF-12).
     */
    @Transactional
    public void excluir(UUID id) {
        Valvula valvula = buscar(id);
        if (servicoRepository.existsByValvulaId(id) || oportunidadeService.existeParaValvula(id)) {
            throw new ConflitoException(
                    "A válvula %s já tem serviço lançado. Para tirá-la do painel sem perder o histórico, inative-a."
                            .formatted(valvula.getCodigoReferencia()));
        }
        valvulaRepository.delete(valvula);
    }

    public Valvula buscar(UUID id) {
        return valvulaRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Válvula não encontrada."));
    }

    /** RF-13: duas válvulas com o mesmo número na mesma estação seriam ambíguas em campo. */
    private void garantirNumeroLivre(UUID estacaoId, String numero, UUID idAtual) {
        valvulaRepository.findByEstacaoIdAndNumeroValvula(estacaoId, numero)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflitoException("Já existe a válvula %s nesta estação (%s)%s."
                            .formatted(numero, existente.getCodigoReferencia(),
                                    existente.isAtivo() ? "" : ", inativa"));
                });
    }

    private Especificacao especificacaoAtiva(UUID id) {
        Especificacao especificacao = especificacaoService.buscar(id);
        if (!especificacao.isAtivo()) {
            throw new RegraDeNegocioException("A especificação escolhida está inativa.");
        }
        return especificacao;
    }

    /**
     * RN-14: COND-001 + -VRP- + sequencial do cliente. Inativar não libera o número;
     * excluir a última válvula (cadastro por engano, RF-12a) libera.
     */
    private String proximoCodigo(Cliente cliente) {
        int sequencial = valvulaRepository.maiorSequencialDoCliente(cliente.getId()) + 1;
        return "%s-VRP-%02d".formatted(cliente.getCodigoReferencia(), sequencial);
    }

    private static String formatarNumero(int numero) {
        if (numero < 1) {
            throw new RegraDeNegocioException("O número da válvula começa em 01.");
        }
        return "%02d".formatted(numero);
    }
}
