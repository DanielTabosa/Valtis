package br.com.manutec.valtis.comum;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * O "hoje" do sistema é o de Recife, não o da máquina onde ele roda.
 *
 * O servidor do Railway roda em UTC. Sem isto, entre 21h e meia-noite o
 * servidor já está no dia seguinte e todo prazo sai um dia menor.
 *
 * Injetar o relógio também deixa os testes determinísticos: no teste,
 * entra um Clock.fixed parado no dia que o teste escolher.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(ValtisPropriedades propriedades) {
        return Clock.system(ZoneId.of(propriedades.fusoHorario()));
    }
}
