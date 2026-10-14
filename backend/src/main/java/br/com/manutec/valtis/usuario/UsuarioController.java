package br.com.manutec.valtis.usuario;

import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    public record NovoUsuario(
            @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 200) String email,
            @NotNull(message = "Escolha o perfil.") Perfil perfil,
            @NotBlank @Size(min = 8, max = 72, message = "A senha precisa ter de 8 a 72 caracteres.") String senha) {
    }

    public record AlteracaoUsuario(
            @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
            @NotNull(message = "Escolha o perfil.") Perfil perfil) {
    }

    public record NovaSenha(
            @NotBlank @Size(min = 8, max = 72, message = "A senha precisa ter de 8 a 72 caracteres.") String senha) {
    }

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    /** Para listas de escolha. Qualquer usuário logado pode ver nomes e perfis. */
    @GetMapping("/ativos")
    public List<UsuarioService.UsuarioOpcao> ativos() {
        return service.ativos();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioService.UsuarioResposta> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioService.UsuarioResposta criar(@Valid @RequestBody NovoUsuario dados) {
        return service.criar(dados.nome(), dados.email(), dados.perfil(), dados.senha());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioService.UsuarioResposta atualizar(@PathVariable UUID id, @Valid @RequestBody AlteracaoUsuario dados,
                                                    @AuthenticationPrincipal UsuarioLogado eu) {
        return service.atualizar(id, dados.nome(), dados.perfil(), eu.id());
    }

    @PostMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void redefinirSenha(@PathVariable UUID id, @Valid @RequestBody NovaSenha dados) {
        service.redefinirSenha(id, dados.senha());
    }

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void inativar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioLogado eu) {
        service.inativar(id, eu.id());
    }

    @PostMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }
}
