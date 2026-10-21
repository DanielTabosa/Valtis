package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cadastro de bombas (RF-62 a RF-65). Movimentação fica em MovimentacaoService. */
@Service
public class BombaService {

    private final BombaRepository bombaRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final ClienteService clienteService;
    private final RegrasRecorrencia regras;
    private final Clock relogio;

    public BombaService(BombaRepository bombaRepository, OcupacaoRepository ocupacaoRepository,
                        ClienteService clienteService, RegrasRecorrencia regras, Clock relogio) {
        this.bombaRepository = bombaRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.clienteService = clienteService;
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
        Bomba bomba = buscar(id);
        bomba.atualizar(tecnicos(dados), dados.periodicidadeMeses(), dados.ultimaManutencao());
        return resposta(bomba);
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    /** Como RF-12a: exclusão física só para o cadastro por engano, sem nenhuma ocupação. */
    @Transactional
    public void excluir(UUID id) {
        Bomba bomba = buscar(id);
        if (ocupacaoRepository.existsByBombaId(id)) {
            throw new ConflitoException("A bomba %s já tem histórico de posição. Para tirá-la das listas, inative-a."
                    .formatted(bomba.getCodigoReferencia()));
        }
        bombaRepository.delete(bomba);
    }

    public Bomba buscar(UUID id) {
        return bombaRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bomba não encontrada."));
    }

    /** Cadastro + situação calculada de uma bomba. */
    public BombaResposta resposta(Bomba bomba) {
        return BombaResposta.de(bomba, SituacaoBomba.calcular(bomba,
                ocupacaoRepository.abertasDaBomba(bomba.getId()), LocalDate.now(relogio), regras));
    }

    /** Situação de várias bombas com uma consulta só de ocupações. */
    public List<BombaResposta> respostas(List<Bomba> bombas) {
        if (bombas.isEmpty()) {
            return List.of();
        }
        LocalDate hoje = LocalDate.now(relogio);
        List<OcupacaoBomba> abertas = ocupacaoRepository.abertasDasBombas(bombas.stream().map(Bomba::getId).toList());
        return bombas.stream()
                .map(b -> BombaResposta.de(b, SituacaoBomba.calcular(b,
                        abertas.stream().filter(o -> o.getBomba().getId().equals(b.getId())).toList(), hoje, regras)))
                .toList();
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
