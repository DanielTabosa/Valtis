package br.com.manutec.valtis.cliente;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClienteResposta.Resumo> listar(@RequestParam(required = false) String busca,
                                               @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(busca, incluirInativos);
    }

    @GetMapping("/{id}")
    public ClienteResposta.Detalhe detalhar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResposta.Detalhe criar(@Valid @RequestBody DadosCliente dados) {
        return service.criar(dados);
    }

    @PutMapping("/{id}")
    public ClienteResposta.Detalhe atualizar(@PathVariable UUID id, @Valid @RequestBody DadosCliente dados) {
        return service.atualizar(id, dados);
    }

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }

    @PostMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }
}
