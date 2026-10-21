package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.painel.PainelService;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Listas e fichas de bombas (RF-67, RF-68). Só leitura. */
@Service
public class BombaConsultaService {

    public record BombaDetalhe(BombaResposta bomba, List<OcupacaoResposta> historico) {}

    public record PainelBombas(List<PainelService.ContagemStatus> resumo, List<BombaResposta> linhas) {}

    private final BombaRepository bombaRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final BombaService bombaService;

    public BombaConsultaService(BombaRepository bombaRepository, OcupacaoRepository ocupacaoRepository,
                                BombaService bombaService) {
        this.bombaRepository = bombaRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.bombaService = bombaService;
    }

    @Transactional(readOnly = true)
    public BombaDetalhe detalhar(UUID id) {
        Bomba bomba = bombaService.buscar(id);
        return new BombaDetalhe(bombaService.resposta(bomba),
                ocupacaoRepository.historicoDaBomba(id).stream().map(OcupacaoResposta::de).toList());
    }

    @Transactional(readOnly = true)
    public List<BombaResposta> doCliente(UUID clienteId) {
        return bombaService.respostas(bombaRepository.findByClienteIdOrderByCodigoReferencia(clienteId));
    }

    /** RF-65: o patrimônio da Manutec, com disponibilidade. */
    @Transactional(readOnly = true)
    public List<BombaResposta> patrimonio() {
        return bombaService.respostas(bombaRepository.findByPatrimonioManutecTrueOrderByCodigoReferencia());
    }

    /** Painel de bombas: mesma classificação do painel de válvulas (RN-16, RN-04), sem oportunidade (P-14). */
    @Transactional(readOnly = true)
    public PainelBombas painel(StatusPainel status, String busca) {
        String termo = busca == null ? "" : busca.strip().toLowerCase();
        List<BombaResposta> todas = bombaService.respostas(bombaRepository.ativasParaOPainel()).stream()
                .filter(b -> termo.isEmpty() || combina(b, termo))
                .sorted(Comparator.comparing(BombaResposta::status)
                        .thenComparing(BombaResposta::diasRestantes, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(BombaResposta::codigoReferencia))
                .toList();

        Map<StatusPainel, Long> totais = todas.stream()
                .collect(Collectors.groupingBy(BombaResposta::status, Collectors.counting()));
        List<PainelService.ContagemStatus> resumo = Arrays.stream(StatusPainel.values())
                .map(s -> new PainelService.ContagemStatus(s, s.getRotulo(), totais.getOrDefault(s, 0L)))
                .toList();

        // Os cartões contam sem o filtro de situação, como no painel de válvulas.
        return new PainelBombas(resumo, status == null ? todas : todas.stream().filter(b -> b.status() == status).toList());
    }

    private static boolean combina(BombaResposta b, String termo) {
        return b.codigoReferencia().toLowerCase().contains(termo)
                || (b.nomeCondominio() != null && b.nomeCondominio().toLowerCase().contains(termo))
                || b.descricao().toLowerCase().contains(termo)
                || b.posicoes().stream().anyMatch(p -> p.nomeCondominio().toLowerCase().contains(termo));
    }
}
