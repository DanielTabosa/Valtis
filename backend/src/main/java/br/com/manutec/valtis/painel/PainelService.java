package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.estacao.Andares;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.valvula.PainelProjecao;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso do painel de status (módulo M4).
 *
 * Aplica RN-01, RN-04 e RN-05 sobre os dados crus do repositório. As regras em
 * si moram em RegrasRecorrencia; aqui só se monta a linha.
 */
@Service
public class PainelService {

    public record ContagemStatus(StatusPainel status, String rotulo, long total) {}

    /** Valores que existem na base, para a tela montar as listas de bairro e técnico. */
    public record OpcoesDeFiltro(List<String> bairros, List<String> tecnicos) {}

    public record Painel(List<ContagemStatus> resumo, List<LinhaPainel> linhas, OpcoesDeFiltro opcoes) {}

    private final ValvulaRepository valvulaRepository;
    private final RegrasRecorrencia regras;
    private final Clock relogio;

    public PainelService(ValvulaRepository valvulaRepository, RegrasRecorrencia regras, Clock relogio) {
        this.valvulaRepository = valvulaRepository;
        this.regras = regras;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Painel montar(FiltroPainel filtro) {
        LocalDate hoje = LocalDate.now(relogio);

        List<LinhaPainel> todas = valvulaRepository.buscarDadosDoPainel()
                .stream()
                .map(p -> montarLinha(p, hoje))
                .toList();

        List<LinhaPainel> semFiltroDeSituacao = todas.stream()
                .filter(filtro::aceitaIgnorandoSituacao)
                .sorted(ordemDePrioridade())
                .toList();

        List<LinhaPainel> linhas = semFiltroDeSituacao.stream()
                .filter(filtro::aceitaSituacao)
                .toList();

        // Os cartões do resumo são o próprio filtro de situação: contam sem ele,
        // senão escolher um zeraria os outros três.
        return new Painel(contar(semFiltroDeSituacao), linhas, opcoes(todas));
    }

    // As opções vêm da base inteira, não do recorte: senão escolher um bairro
    // esvaziaria a lista de técnicos de outros bairros.
    private OpcoesDeFiltro opcoes(List<LinhaPainel> linhas) {
        return new OpcoesDeFiltro(distintos(linhas, LinhaPainel::bairro), distintos(linhas, LinhaPainel::ultimoTecnico));
    }

    private static List<String> distintos(List<LinhaPainel> linhas, Function<LinhaPainel, String> campo) {
        return linhas.stream()
                .map(campo)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** RF-39: contagem por situação, sempre com as quatro situações, mesmo as zeradas. */
    private List<ContagemStatus> contar(List<LinhaPainel> linhas) {
        Map<StatusPainel, Long> totais = linhas.stream()
                .collect(Collectors.groupingBy(LinhaPainel::status, Collectors.counting()));

        return Arrays.stream(StatusPainel.values())
                .map(s -> new ContagemStatus(s, s.getRotulo(), totais.getOrDefault(s, 0L)))
                .toList();
    }

    /** Vencidos primeiro; dentro da mesma situação, o que vence mais cedo. */
    private Comparator<LinhaPainel> ordemDePrioridade() {
        return Comparator.comparing(LinhaPainel::status)
                .thenComparing(LinhaPainel::diasRestantes, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LinhaPainel::codigoReferencia);
    }

    private LinhaPainel montarLinha(PainelProjecao p, LocalDate hoje) {
        LocalDate ultima = p.getUltimaManutencao();

        // RN-05: válvula sem manutenção lançada não tem data-base.
        // Não dá para calcular vencimento, então não vira oportunidade.
        if (ultima == null) {
            return linha(p, null, null, null, StatusPainel.SEM_REGISTRO);
        }

        LocalDate proxima = regras.proximaManutencao(ultima, p.getPeriodicidadeMeses());
        long dias = ChronoUnit.DAYS.between(hoje, proxima);
        return linha(p, ultima, proxima, dias, regras.classificar(dias));
    }

    private LinhaPainel linha(PainelProjecao p, LocalDate ultima, LocalDate proxima, Long dias, StatusPainel status) {
        return new LinhaPainel(
                p.getValvulaId(), p.getEstacaoId(), p.getClienteId(), p.getCodigoReferencia(),
                p.getNomeCondominio(), p.getBairro(), p.getTipoAtendimento(), p.getLocalizacaoInstalacao(),
                Andares.descrever(p.getAndarInicial(), p.getAndarFinal()), p.getEspecificacao(), p.getNumeroSerie(),
                Boolean.TRUE.equals(p.getRequerFechamentoGeral()),
                ultima, p.getUltimoTecnico(), proxima, dias, status, status.getRotulo());
    }
}
