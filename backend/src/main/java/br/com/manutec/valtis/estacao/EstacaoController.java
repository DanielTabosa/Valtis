package br.com.manutec.valtis.estacao;

import br.com.manutec.valtis.comum.ModoDeOperacao;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class EstacaoController {

    /** Q-2: o que a estação derivada do Jiraya não tem, completado no Valtis. */
    public record ComplementoEstacao(Integer andarInicial, Integer andarFinal, Boolean requerFechamentoGeral) {}

    private final EstacaoService service;
    private final ModoDeOperacao modo;

    public EstacaoController(EstacaoService service, ModoDeOperacao modo) {
        this.service = service;
        this.modo = modo;
    }

    @PostMapping("/api/clientes/{clienteId}/estacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public EstacaoResposta criar(@PathVariable UUID clienteId, @Valid @RequestBody DadosEstacao dados) {
        // RF-73: em externa a estação é derivada da localização interna do equipamento no Jiraya.
        modo.exigirCadastroProprio("a estação", ModoDeOperacao.JIRAYA);
        return service.criar(clienteId, dados);
    }

    @PutMapping("/api/estacoes/{id}/complemento")
    public EstacaoResposta completar(@PathVariable UUID id, @RequestBody ComplementoEstacao dados) {
        return service.completar(id, dados.andarInicial(), dados.andarFinal(),
                Boolean.TRUE.equals(dados.requerFechamentoGeral()));
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
