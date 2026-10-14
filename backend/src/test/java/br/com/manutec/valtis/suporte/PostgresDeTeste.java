package br.com.manutec.valtis.suporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Um Postgres de verdade, num container Docker que nasce com os testes e morre com eles.
 *
 * @ServiceConnection faz o Spring usar este banco no lugar do configurado.
 * Assim os testes nunca encostam no Supabase, mesmo com DB_URL definida na máquina.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresDeTeste {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
