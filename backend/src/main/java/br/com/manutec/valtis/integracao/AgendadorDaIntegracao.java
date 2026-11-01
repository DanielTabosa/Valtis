package br.com.manutec.valtis.integracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara a rodada a cada intervalo (P-23: 5 minutos). Separado do leitor para
 * os testes poderem desligar o relógio e rodar na mão, como na agenda.
 */
@Component
@ConditionalOnExpression("'${valtis.cadastros.origem}' == 'externa' && '${valtis.integracao.agendada:true}' == 'true'")
public class AgendadorDaIntegracao {

    private final LeitorDaIntegracao leitor;

    public AgendadorDaIntegracao(LeitorDaIntegracao leitor) {
        this.leitor = leitor;
    }

    @Scheduled(fixedDelayString = "${valtis.integracao.intervalo-ms}",
            initialDelayString = "${valtis.integracao.atraso-inicial-ms}")
    public void rodar() {
        leitor.rodar();
    }
}
