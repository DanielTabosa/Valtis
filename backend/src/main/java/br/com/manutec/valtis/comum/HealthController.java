package br.com.manutec.valtis.comum;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sinal de vida. Público e sem banco: diz se a aplicação está no ar
 * mesmo quando o banco não está.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    public record HealthResponse(String status, String aplicacao) {}

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "valtis");
    }
}
