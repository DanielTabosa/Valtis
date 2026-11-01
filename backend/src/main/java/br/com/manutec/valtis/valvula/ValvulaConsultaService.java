package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.oportunidade.OportunidadeRepository;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.servico.ServicoRepository;
import br.com.manutec.valtis.servico.StatusServico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Monta a ficha da válvula. Só leitura. */
@Service
public class ValvulaConsultaService {

    private final ValvulaService valvulaService;
    private final ServicoRepository servicoRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final RegrasRecorrencia regras;
    private final Clock relogio;

    public ValvulaConsultaService(ValvulaService valvulaService, ServicoRepository servicoRepository,
                                  OportunidadeRepository oportunidadeRepository, RegrasRecorrencia regras,
                                  Clock relogio) {
        this.valvulaService = valvulaService;
        this.servicoRepository = servicoRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.regras = regras;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public ValvulaDetalhe detalhar(UUID id) {
        Valvula valvula = valvulaService.buscar(id);
        Estacao estacao = valvula.getEstacao();
        List<Servico> historico = servicoRepository.historicoDaValvula(id);

        LocalDate ultima = historico.stream()
                .filter(s -> s.isAtivo() && s.getStatus() == StatusServico.CONCLUIDO)
                .map(Servico::getDataRealizada)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Mesmas regras do painel (RN-01, RN-04, RN-05): a ficha nunca diverge dele.
        LocalDate proxima = ultima == null ? null : regras.proximaManutencao(ultima, valvula.getPeriodicidadeMeses());
        Long dias = proxima == null ? null : ChronoUnit.DAYS.between(LocalDate.now(relogio), proxima);
        StatusPainel status = dias == null ? StatusPainel.SEM_REGISTRO : regras.classificar(dias);

        return new ValvulaDetalhe(
                ValvulaResposta.de(valvula),
                estacao.getCliente().getId(), estacao.getCliente().getCodigoReferencia(),
                estacao.getCliente().getNomeCondominio(), estacao.getCliente().isAtivo(),
                estacao.getId(), estacao.getLocalizacaoInstalacao(), estacao.descreverAndares(),
                estacao.isRequerFechamentoGeral(), estacao.isAtivo(),
                ultima, proxima, dias, status, status.getRotulo(),
                historico.stream().map(ValvulaConsultaService::doHistorico).toList(),
                oportunidadeRepository.daValvula(id).stream()
                        .map(ValvulaDetalhe.OportunidadeDoHistorico::de)
                        .toList());
    }

    private static ValvulaDetalhe.ServicoDoHistorico doHistorico(Servico s) {
        return ValvulaDetalhe.ServicoDoHistorico.de(s);
    }
}
