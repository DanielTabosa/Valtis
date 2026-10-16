package br.com.manutec.valtis.valvula;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ValvulaController {

    private final ValvulaService service;
    private final ValvulaConsultaService consulta;

    public ValvulaController(ValvulaService service, ValvulaConsultaService consulta) {
        this.service = service;
        this.consulta = consulta;
    }

    @PostMapping("/api/estacoes/{estacaoId}/valvulas")
    @ResponseStatus(HttpStatus.CREATED)
    public ValvulaResposta criar(@PathVariable UUID estacaoId, @Valid @RequestBody DadosValvula dados) {
        return service.criar(estacaoId, dados);
    }

    @GetMapping("/api/valvulas/{id}")
    public ValvulaDetalhe detalhar(@PathVariable UUID id) {
        return consulta.detalhar(id);
    }

    @PutMapping("/api/valvulas/{id}")
    public ValvulaResposta atualizar(@PathVariable UUID id, @Valid @RequestBody DadosValvula dados) {
        return service.atualizar(id, dados);
    }

    // Técnico também inativa, reativa e exclui válvula: é ele quem conhece o equipamento em campo.
    @PostMapping("/api/valvulas/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }

    @PostMapping("/api/valvulas/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }

    @DeleteMapping("/api/valvulas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
