package br.com.manutec.valtis.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Autoriza o frontend a chamar esta API.
 *
 * O navegador bloqueia, por padrão, requisições de uma origem (localhost:5173)
 * para outra (localhost:8080). Quem libera é o SERVIDOR, respondendo que
 * aceita aquela origem. Isso é o CORS.
 *
 * É um bean (e não mais um WebMvcConfigurer) porque o Spring Security precisa
 * responder ao CORS antes de exigir o token: a pergunta prévia do navegador
 * (OPTIONS) nunca leva token.
 *
 * As origens vêm de configuração para que produção não precise de recompilação.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${valtis.cors.origens}") List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);
        return fonte;
    }
}
