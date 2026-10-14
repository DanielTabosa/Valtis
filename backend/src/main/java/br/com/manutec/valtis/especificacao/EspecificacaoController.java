package br.com.manutec.valtis.especificacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/especificacoes")
public class EspecificacaoController {

    public record EspecificacaoResposta(UUID id, String marca, String modelo, String diametroPolegadas,
                                        TipoRegistro tipoRegistro, String descricaoCompleta, boolean ativo) {

        static EspecificacaoResposta de(Especificacao e) {
            return new EspecificacaoResposta(e.getId(), e.getMarca(), e.getModelo(), e.getDiametroPolegadas(),
                    e.getTipoRegistro(), e.getDescricaoCompleta(), e.isAtivo());
        }
    }

    private final EspecificacaoService service;

    public EspecificacaoController(EspecificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EspecificacaoResposta> listar(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return service.listar(incluirInativas).stream().map(EspecificacaoResposta::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspecificacaoResposta criar(@Valid @RequestBody DadosEspecificacao dados) {
        return EspecificacaoResposta.de(service.criar(dados));
    }

    @PutMapping("/{id}")
    public EspecificacaoResposta atualizar(@PathVariable UUID id, @Valid @RequestBody DadosEspecificacao dados) {
        return EspecificacaoResposta.de(service.atualizar(id, dados));
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
