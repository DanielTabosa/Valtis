package br.com.manutec.valtis.recorrencia;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * As regras de recorrência, num lugar só (módulo M5).
 *
 * Antes desta classe, a RN-01 estava escrita duas vezes: no painel e na
 * entidade Servico. Duas cópias de uma regra são duas verdades esperando para
 * divergir. Painel, oportunidades e agenda agora perguntam tudo aqui.
 *
 * Não depende de banco, rede nem relógio: recebe as datas prontas.
 * Por isso é testável com um simples `new`.
 */
@Component
public class RegrasRecorrencia {

    private final int alertaComercialDias;
    private final int lembreteFinalDias;

    @Autowired
    public RegrasRecorrencia(ValtisPropriedades propriedades) {
        this(propriedades.recorrencia().alertaComercialDias(), propriedades.recorrencia().lembreteFinalDias());
    }

    public RegrasRecorrencia(int alertaComercialDias, int lembreteFinalDias) {
        if (alertaComercialDias <= lembreteFinalDias || lembreteFinalDias <= 0) {
            throw new IllegalArgumentException(
                    "O alerta comercial precisa vir antes do lembrete final, e ambos com antecedência positiva.");
        }
        this.alertaComercialDias = alertaComercialDias;
        this.lembreteFinalDias = lembreteFinalDias;
    }

    /** RN-01: próxima manutenção = data realizada + periodicidade da válvula. */
    public LocalDate proximaManutencao(LocalDate dataRealizada, int periodicidadeMeses) {
        return dataRealizada.plusMonths(periodicidadeMeses);
    }

    /** RN-06: primeiro alerta, para o contato comercial (padrão: 30 dias antes). */
    public LocalDate dataAlertaComercial(LocalDate dataPrevista) {
        return dataPrevista.minusDays(alertaComercialDias);
    }

    /** RN-06: segundo alerta, o lembrete final (padrão: 7 dias antes). */
    public LocalDate dataLembreteFinal(LocalDate dataPrevista) {
        return dataPrevista.minusDays(lembreteFinalDias);
    }

    /**
     * RN-04. A janela de PRÓXIMO DO VENCIMENTO é a mesma do alerta comercial (D-08):
     * a válvula fica amarela no painel no mesmo dia em que o alerta chega na agenda.
     */
    public StatusPainel classificar(long diasRestantes) {
        if (diasRestantes < 0) {
            return StatusPainel.VENCIDO;
        }
        if (diasRestantes <= alertaComercialDias) {
            return StatusPainel.PROXIMO_DO_VENCIMENTO;
        }
        return StatusPainel.EM_DIA;
    }
}
