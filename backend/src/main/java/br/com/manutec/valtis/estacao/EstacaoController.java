package br.com.manutec.valtis.estacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class EstacaoController {

    private final EstacaoService service;

    public EstacaoController(EstacaoService service) {
        this.service = service;
    }

    @PostMapping("/api/clientes/{clienteId}/estacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public EstacaoResposta criar(@PathVariable UUID clienteId, @Valid @RequestBody DadosEstacao dados) {
        return service.criar(clienteId, dados);
    }

    @PutMapping("/api/estacoes/{id}")
    public EstacaoResposta atualizar(@PathVariable UUID id, @Valid @RequestBody DadosEstacao dados) {
        return service.atualizar(id, dados);
    }

    @PostMapping("/api/estacoes/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }

    @PostMapping("/api/estacoes/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }
}
