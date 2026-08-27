package br.com.manutec.valtis.fase0;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de sinal de vida. Não depende de banco: serve para saber se a
 * aplicação está no ar mesmo quando o banco não está.
 *
 * Código de Fase 0 — será removido antes da Fase 1.
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
