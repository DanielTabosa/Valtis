package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Fila de oportunidades (RF-41 a RF-46). */
@RestController
@RequestMapping("/api/oportunidades")
public class OportunidadeController {

    private final OportunidadeService service;

    public OportunidadeController(OportunidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<OportunidadeResposta> fila(
            @RequestParam(defaultValue = "ABERTAS") OportunidadeService.Situacao situacao,
            @RequestParam(defaultValue = "QUALQUER") OportunidadeService.Responsavel responsavel,
            @AuthenticationPrincipal UsuarioLogado eu) {
        return service.fila(situacao, responsavel, eu.id());
    }

    @PostMapping("/{id}/assumir")
    public OportunidadeResposta assumir(@PathVariable UUID id, @AuthenticationPrincipal UsuarioLogado eu) {
        return service.assumir(id, eu.id());
    }

    @PostMapping("/{id}/liberar")
    public OportunidadeResposta liberar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioLogado eu) {
        return service.liberar(id, eu.id());
    }

    @PostMapping("/{id}/desfecho")
    public OportunidadeService.ResultadoDesfecho desfecho(@PathVariable UUID id,
                                                          @Valid @RequestBody DesfechoRequisicao pedido,
                                                          @AuthenticationPrincipal UsuarioLogado eu) {
        return service.registrarDesfecho(id, pedido, eu.id());
    }
}
