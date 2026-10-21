package br.com.manutec.valtis.bomba;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ConjuntoController {

    private final ConjuntoService service;

    public ConjuntoController(ConjuntoService service) {
        this.service = service;
    }

    @GetMapping("/api/clientes/{clienteId}/conjuntos")
    public List<ConjuntoResposta> doCliente(@PathVariable UUID clienteId) {
        return service.doCliente(clienteId);
    }

    @PostMapping("/api/clientes/{clienteId}/conjuntos")
    @ResponseStatus(HttpStatus.CREATED)
    public ConjuntoResposta criar(@PathVariable UUID clienteId, @Valid @RequestBody DadosConjunto dados) {
        return service.criar(clienteId, dados);
    }

    @PutMapping("/api/conjuntos/{id}")
    public ConjuntoResposta atualizar(@PathVariable UUID id, @Valid @RequestBody DadosConjunto dados) {
        return service.atualizar(id, dados);
    }

    // Como a estação: só admin inativa (S-01).
    @PostMapping("/api/conjuntos/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }

    @PostMapping("/api/conjuntos/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }
}
