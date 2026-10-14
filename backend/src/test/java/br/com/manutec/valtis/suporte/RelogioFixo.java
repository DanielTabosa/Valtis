package br.com.manutec.valtis.suporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Nos testes de integração, "hoje" é sempre 13/10/2026, meio-dia em Recife. */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioFixo {

    public static final ZoneId RECIFE = ZoneId.of("America/Recife");
    public static final LocalDate HOJE = LocalDate.of(2026, 10, 13);

    @Bean
    @Primary
    Clock relogioDeTeste() {
        return Clock.fixed(HOJE.atTime(12, 0).atZone(RECIFE).toInstant(), RECIFE);
    }
}
