package br.com.manutec.valtis.cliente;

import br.com.manutec.valtis.comum.ModoDeOperacao;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    /** RN-24 por campo: no cliente espelhado, só o que o Manutec Clientes não tem. */
    public record ComplementoCliente(TipoAtendimento tipoAtendimento, StatusContrato statusContrato) {}

    private final ClienteService service;
    private final ModoDeOperacao modo;

    public ClienteController(ClienteService service, ModoDeOperacao modo) {
        this.service = service;
        this.modo = modo;
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
        modo.exigirCadastroProprio("o cliente", ModoDeOperacao.MANUTEC_CLIENTES); // RF-71
        return service.criar(dados);
    }

    @PutMapping("/{id}/complemento")
    public ClienteResposta.Detalhe completar(@PathVariable UUID id, @RequestBody ComplementoCliente dados) {
        return service.completar(id, dados.tipoAtendimento(), dados.statusContrato());
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
