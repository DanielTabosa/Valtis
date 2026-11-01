package br.com.manutec.valtis.valvula;

import br.com.manutec.valtis.comum.ModoDeOperacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class ValvulaController {

    /** Q-1, RN-24 por campo: o que a válvula espelhada não traz do Jiraya. */
    public record ComplementoValvula(
            UUID especificacaoId,
            @PastOrPresent(message = "A data de instalação não pode estar no futuro.") LocalDate dataInstalacao,
            @Min(value = 1, message = "A periodicidade mínima é 1 mês.")
            @Max(value = 60, message = "A periodicidade máxima é 60 meses.")
            Integer periodicidadeMeses) {}

    private final ValvulaService service;
    private final ValvulaConsultaService consulta;
    private final ModoDeOperacao modo;

    public ValvulaController(ValvulaService service, ValvulaConsultaService consulta, ModoDeOperacao modo) {
        this.service = service;
        this.consulta = consulta;
        this.modo = modo;
    }

    @PostMapping("/api/estacoes/{estacaoId}/valvulas")
    @ResponseStatus(HttpStatus.CREATED)
    public ValvulaResposta criar(@PathVariable UUID estacaoId, @Valid @RequestBody DadosValvula dados) {
        modo.exigirCadastroProprio("a válvula", ModoDeOperacao.JIRAYA); // RF-72
        return service.criar(estacaoId, dados);
    }

    @PutMapping("/api/valvulas/{id}/complemento")
    public ValvulaResposta completar(@PathVariable UUID id, @Valid @RequestBody ComplementoValvula dados) {
        return service.completar(id, dados.especificacaoId(), dados.dataInstalacao(), dados.periodicidadeMeses());
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
