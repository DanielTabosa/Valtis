package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.ModoDeOperacao;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cadastro de bombas (RF-62 a RF-65). Movimentação fica em MovimentacaoService. */
@Service
public class BombaService {

    private final BombaRepository bombaRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final ClienteService clienteService;
    private final ServicoRepository servicoRepository;
    private final OportunidadeService oportunidades;
    private final RegrasRecorrencia regras;
    private final Clock relogio;

    public BombaService(BombaRepository bombaRepository, OcupacaoRepository ocupacaoRepository,
                        ClienteService clienteService, ServicoRepository servicoRepository,
                        OportunidadeService oportunidades, RegrasRecorrencia regras, Clock relogio) {
        this.bombaRepository = bombaRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.clienteService = clienteService;
        this.servicoRepository = servicoRepository;
        this.oportunidades = oportunidades;
        this.regras = regras;
        this.relogio = relogio;
    }

    @Transactional
    public Bomba criarDoCondominio(UUID clienteId, DadosBomba dados) {
        Cliente cliente = clienteService.buscar(clienteId);
        if (!cliente.isAtivo()) {
            throw new RegraDeNegocioException("Não é possível cadastrar bomba em cliente inativo.");
        }
        return bombaRepository.save(Bomba.doCondominio(cliente, proximoCodigoDoCliente(cliente), tecnicos(dados),
                dados.periodicidadeMeses(), dados.ultimaManutencao()));
    }

    @Transactional
    public Bomba criarPatrimonial(DadosBomba dados) {
        return bombaRepository.save(Bomba.patrimonial(proximoCodigoDoPatrimonio(), tecnicos(dados),
                dados.periodicidadeMeses(), dados.ultimaManutencao()));
    }

    @Transactional
    public BombaResposta atualizar(UUID id, DadosBomba dados) {
        Bomba bomba = buscarPropria(id);
        Integer periodicidadeAnterior = bomba.getPeriodicidadeMeses();
        bomba.atualizar(tecnicos(dados), dados.periodicidadeMeses(), dados.ultimaManutencao());
        reprogramarSeMudouAPeriodicidade(bomba, periodicidadeAnterior);
        return resposta(bomba);
    }

    /** RN-24 por campo: na espelhada, fabricante e modelo ficam como o Jiraya mandou. */
    @Transactional
    public BombaResposta completar(UUID id, DadosBomba dados) {
        Bomba bomba = buscar(id);
        Integer periodicidadeAnterior = bomba.getPeriodicidadeMeses();
        if (bomba.isEspelhada()) {
            bomba.completar(tecnicos(dados), dados.periodicidadeMeses(), dados.ultimaManutencao());
        } else {
            bomba.atualizar(tecnicos(dados), dados.periodicidadeMeses(), dados.ultimaManutencao());
        }
        reprogramarSeMudouAPeriodicidade(bomba, periodicidadeAnterior);
        return resposta(bomba);
    }

    @Transactional
    public void inativar(UUID id) {
        buscarPropria(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscarPropria(id).reativar();
    }

    /** A espelhada só muda pelo Jiraya, inclusive a inativação (RF-72, RF-79). */
    private Bomba buscarPropria(UUID id) {
        Bomba bomba = buscar(id);
        ModoDeOperacao.exigirNaoEspelhado(bomba.isEspelhada(), "a bomba", ModoDeOperacao.JIRAYA);
        return bomba;
    }

    /** Como RF-12a: exclusão física só para o cadastro por engano, sem nenhuma ocupação. */
    @Transactional
    public void excluir(UUID id) {
        Bomba bomba = buscarPropria(id);
        if (ocupacaoRepository.existsByBombaId(id) || servicoRepository.existsByBombaId(id)
                || oportunidades.existeParaBomba(id)) {
            throw new ConflitoException("A bomba %s já tem histórico de posição. Para tirá-la das listas, inative-a."
                    .formatted(bomba.getCodigoReferencia()));
        }
        bombaRepository.delete(bomba);
    }

    /** RF-72: a bomba espelhada ganha código COND-NNN-BMB-NN como qualquer outra; periodicidade 12 (P-22). */
    @Transactional
    public Bomba criarEspelho(Cliente cliente, long jirayaEquipamentoId, Bomba.DadosDoJiraya dados,
                              OffsetDateTime agora) {
        return bombaRepository.save(Bomba.espelhoDoJiraya(cliente, proximoCodigoDoCliente(cliente),
                jirayaEquipamentoId, dados, Bomba.PERIODICIDADE_PADRAO_MESES, agora));
    }

    public Bomba buscar(UUID id) {
        return bombaRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bomba não encontrada."));
    }

    /** Cadastro + situação calculada de uma bomba. */
    public BombaResposta resposta(Bomba bomba) {
        return BombaResposta.de(bomba, SituacaoBomba.calcular(bomba,
                ocupacaoRepository.abertasDaBomba(bomba.getId()),
                servicoRepository.ultimaDataConcluidaDaBomba(bomba.getId()).orElse(null),
                LocalDate.now(relogio), regras));
    }

    /** Situação de várias bombas com uma consulta só de ocupações. */
    public List<BombaResposta> respostas(List<Bomba> bombas) {
        if (bombas.isEmpty()) {
            return List.of();
        }
        LocalDate hoje = LocalDate.now(relogio);
        List<UUID> ids = bombas.stream().map(Bomba::getId).toList();
        List<OcupacaoBomba> abertas = ocupacaoRepository.abertasDasBombas(ids);
        Map<UUID, LocalDate> ultimosServicos = servicoRepository.ultimosDasBombas(ids).stream()
                .collect(Collectors.toMap(ServicoRepository.UltimoServicoDaBomba::getBombaId,
                        ServicoRepository.UltimoServicoDaBomba::getUltima));
        return bombas.stream()
                .map(b -> BombaResposta.de(b, SituacaoBomba.calcular(b,
                        abertas.stream().filter(o -> o.getBomba().getId().equals(b.getId())).toList(),
                        ultimosServicos.get(b.getId()), hoje, regras)))
                .toList();
    }

    /** P-22: mudou a periodicidade, o ciclo aberto acompanha (como RN-02 na válvula). */
    private void reprogramarSeMudouAPeriodicidade(Bomba bomba, Integer anterior) {
        if (bomba.getPeriodicidadeMeses().equals(anterior)) {
            return;
        }
        LocalDate ultima = SituacaoBomba.calcular(bomba, List.of(),
                servicoRepository.ultimaDataConcluidaDaBomba(bomba.getId()).orElse(null),
                LocalDate.now(relogio), regras).ultimaManutencao();
        if (ultima != null) {
            oportunidades.reprogramarCicloDaBomba(bomba, ultima);
        }
    }

    private static Bomba.DadosTecnicos tecnicos(DadosBomba d) {
        return new Bomba.DadosTecnicos(Textos.limpar(d.fabricante()), Textos.limpar(d.modelo()), d.potenciaCv(),
                d.tensaoVolts(), d.correnteAmperes(), Textos.limpar(d.funcao()), d.dataInstalacao());
    }

    /** RN-15: COND-001-BMB-NN. Inativar não libera o número. */
    private String proximoCodigoDoCliente(Cliente cliente) {
        int sequencial = bombaRepository.maiorSequencialDoCliente(cliente.getId()) + 1;
        return "%s-BMB-%02d".formatted(cliente.getCodigoReferencia(), sequencial);
    }

    /** RN-15: MAN-BMB-NNN, sequencial único do patrimônio. */
    private String proximoCodigoDoPatrimonio() {
        return "MAN-BMB-%03d".formatted(bombaRepository.maiorSequencialDoPatrimonio() + 1);
    }
}
