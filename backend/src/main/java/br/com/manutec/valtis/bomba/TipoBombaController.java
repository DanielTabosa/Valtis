package br.com.manutec.valtis.bomba;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tipos-bomba")
public class TipoBombaController {

    public record DadosTipoBomba(@NotBlank(message = "Informe o nome do tipo.") @Size(max = 60) String nome) {}

    public record TipoBombaResposta(UUID id, String nome, boolean ativo) {
        static TipoBombaResposta de(TipoBomba t) {
            return new TipoBombaResposta(t.getId(), t.getNome(), t.isAtivo());
        }
    }

    private final TipoBombaService service;

    public TipoBombaController(TipoBombaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoBombaResposta> listar(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(incluirInativos).stream().map(TipoBombaResposta::de).toList();
    }

    // Catálogo é configuração do negócio: só admin (P-19).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoBombaResposta criar(@Valid @RequestBody DadosTipoBomba dados) {
        return TipoBombaResposta.de(service.criar(dados.nome()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TipoBombaResposta renomear(@PathVariable UUID id, @Valid @RequestBody DadosTipoBomba dados) {
        return TipoBombaResposta.de(service.renomear(id, dados.nome()));
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
