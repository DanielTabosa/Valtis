package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.valvula.PainelProjecao;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caso de uso do painel de status.
 *
 * É AQUI que moram as regras RN-01 (cálculo do vencimento) e RN-04
 * (classificação). Não no SQL, não no controller, não na tela.
 */
@Service
public class PainelService {

    /** Janela de alerta comercial, em dias (D-08). */
    private static final int JANELA_ALERTA_DIAS = 30;

    private final ValvulaRepository valvulaRepository;

    public PainelService(ValvulaRepository valvulaRepository) {
        this.valvulaRepository = valvulaRepository;
    }

    public List<LinhaPainel> listar() {
        LocalDate hoje = LocalDate.now();

        return valvulaRepository.buscarDadosDoPainel()
                .stream()
                .map(p -> montarLinha(p, hoje))
                // vencidos primeiro, depois os que vencem mais cedo
                .sorted(Comparator.comparing(
                        LinhaPainel::diasRestantes,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Contagem por situação, para o resumo do painel (RF-39). */
    public Map<String, Long> resumo() {
        return listar().stream()
                .collect(Collectors.groupingBy(LinhaPainel::status, Collectors.counting()));
    }

    private LinhaPainel montarLinha(PainelProjecao p, LocalDate hoje) {
        LocalDate ultima = p.getUltimaManutencao();

        // RN-05: válvula sem manutenção lançada não tem data-base.
        // Não dá para calcular vencimento, então não vira oportunidade.
        if (ultima == null) {
            return new LinhaPainel(
                    p.getValvulaId(), p.getCodigoReferencia(), p.getNomeCondominio(), p.getBairro(),
                    p.getLocalizacaoInstalacao(), formatarAndares(p), p.getEspecificacao(),
                    Boolean.TRUE.equals(p.getRequerFechamentoGeral()),
                    null, null, null, StatusPainel.SEM_REGISTRO.getRotulo());
        }

        // RN-01
        LocalDate proxima = ultima.plusMonths(p.getPeriodicidadeMeses());
        long dias = ChronoUnit.DAYS.between(hoje, proxima);

        return new LinhaPainel(
                p.getValvulaId(), p.getCodigoReferencia(), p.getNomeCondominio(), p.getBairro(),
                p.getLocalizacaoInstalacao(), formatarAndares(p), p.getEspecificacao(),
                Boolean.TRUE.equals(p.getRequerFechamentoGeral()),
                ultima, proxima, dias, classificar(dias).getRotulo());
    }

    /** RN-04 */
    private StatusPainel classificar(long diasRestantes) {
        if (diasRestantes < 0) {
            return StatusPainel.VENCIDO;
        }
        if (diasRestantes <= JANELA_ALERTA_DIAS) {
            return StatusPainel.PROXIMO_DO_VENCIMENTO;
        }
        return StatusPainel.EM_DIA;
    }

    private String formatarAndares(PainelProjecao p) {
        return "%d ao %d".formatted(p.getAndarInicial(), p.getAndarFinal());
    }
}
