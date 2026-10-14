package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.usuario.Perfil;
import br.com.manutec.valtis.usuario.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    public record Login(@NotBlank(message = "Informe o e-mail.") String email,
                        @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record UsuarioSessao(UUID id, String nome, String email, Perfil perfil) {}

    public record RespostaLogin(String token, Instant expiraEm, UsuarioSessao usuario) {}

    public record TrocaDeSenha(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank @Size(min = 8, max = 72, message = "A nova senha precisa ter de 8 a 72 caracteres.")
            String novaSenha) {
    }

    private final AutenticacaoService autenticacao;
    private final UsuarioService usuarios;

    public AutenticacaoController(AutenticacaoService autenticacao, UsuarioService usuarios) {
        this.autenticacao = autenticacao;
        this.usuarios = usuarios;
    }

    @PostMapping("/login")
    public RespostaLogin entrar(@Valid @RequestBody Login login) {
        AutenticacaoService.Sessao sessao = autenticacao.entrar(login.email(), login.senha());
        var u = sessao.usuario();
        return new RespostaLogin(sessao.token().valor(), sessao.token().expiraEm(),
                new UsuarioSessao(u.getId(), u.getNome(), u.getEmail(), u.getPerfil()));
    }

    /** Quem sou eu: o frontend usa para restaurar a sessão ao recarregar a página. */
    @GetMapping("/eu")
    public UsuarioSessao eu(@AuthenticationPrincipal UsuarioLogado eu) {
        return new UsuarioSessao(eu.id(), eu.nome(), eu.email(), eu.perfil());
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocaDeSenha troca, @AuthenticationPrincipal UsuarioLogado eu) {
        usuarios.trocarPropriaSenha(eu.id(), troca.senhaAtual(), troca.novaSenha());
    }

    @ExceptionHandler(AutenticacaoService.CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(AutenticacaoService.CredenciaisInvalidasException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AutenticacaoService.MuitasTentativasException.class)
    public ProblemDetail muitasTentativas(AutenticacaoService.MuitasTentativasException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }
}
