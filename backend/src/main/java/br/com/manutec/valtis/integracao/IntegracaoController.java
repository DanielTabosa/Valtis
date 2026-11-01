package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Telas de admin do modo integrado (RF-75, RF-78). */
@RestController
@RequestMapping("/api/integracao")
@PreAuthorize("hasRole('ADMIN')")
public class IntegracaoController {

    public record Descarte(@NotBlank(message = "Informe a justificativa.") @Size(max = 1000) String justificativa) {}

    private final IntegracaoService service;

    public IntegracaoController(IntegracaoService service) {
        this.service = service;
    }

    @GetMapping("/pendencias")
    public List<IntegracaoService.Pendencia> pendencias() {
        return service.pendencias();
    }

    @PostMapping("/eventos/{id}/reprocessamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reprocessar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioLogado eu) {
        service.reprocessar(id, eu.id());
    }

    @PostMapping("/eventos/{id}/descarte")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descartar(@PathVariable UUID id, @Valid @RequestBody Descarte corpo,
                          @AuthenticationPrincipal UsuarioLogado eu) {
        service.descartar(id, corpo.justificativa(), eu.id());
    }

    @GetMapping("/estado")
    public IntegracaoService.Estado estado() {
        return service.estado();
    }

    @PostMapping("/leitura")
    public LeitorDaIntegracao.ResumoDaRodada lerAgora() {
        return service.lerAgora();
    }
}
