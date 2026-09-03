package br.com.manutec.valtis.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autoriza o frontend a chamar esta API.
 *
 * O navegador bloqueia, por padrão, requisições de uma origem (localhost:5173)
 * para outra (localhost:8080). Quem libera é o SERVIDOR, respondendo que
 * aceita aquela origem. Isso é o CORS.
 *
 * As origens vêm de configuração para que produção não precise de recompilação.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${valtis.cors.origens}")
    private String[] origens;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origens)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
