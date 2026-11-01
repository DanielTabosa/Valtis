package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Regras de acesso (RF-01, RNF-23, AGENTS.md §8: nenhuma rota sem autenticação,
 * exceto o login).
 *
 * Modelo: o login devolve um token JWT assinado pelo servidor; o frontend manda
 * esse token no cabeçalho Authorization de toda requisição. Sem sessão no
 * servidor, sem cookie (D-22).
 */
@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    /** HS256 exige uma chave de pelo menos 256 bits. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    static final String EMISSOR = "valtis";

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, ConversorDeToken conversorDeToken) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // CSRF protege sessão por cookie. Aqui o token vai no cabeçalho,
                // que um site malicioso não consegue anexar sozinho.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken)));
        return http.build();
    }

    /** BCrypt: lento de propósito e com sal por senha (RNF-20). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(ValtisPropriedades propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    @Bean
    public JwtDecoder jwtDecoder(ValtisPropriedades propriedades, Clock relogio) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Confere validade (exp) e se o token foi emitido por este sistema.
        // A validade é conferida com o MESMO relógio que emitiu o token (EmissorDeToken):
        // com o relógio do sistema aqui, um "hoje" fixo nos testes fazia todo token
        // nascer vencido depois de 14 dias de calendário real.
        JwtTimestampValidator validade = new JwtTimestampValidator();
        validade.setClock(relogio);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validade, new JwtIssuerValidator(EMISSOR)));
        return decoder;
    }

    /**
     * A chave vem de variável de ambiente, nunca do código (AGENTS.md §8).
     * Sem ela, a aplicação se recusa a subir: melhor falhar na partida do que
     * rodar com uma chave fraca.
     */
    private static SecretKey chave(ValtisPropriedades propriedades) {
        String segredo = propriedades.seguranca().jwtSegredo();
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "Defina VALTIS_JWT_SEGREDO com pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
