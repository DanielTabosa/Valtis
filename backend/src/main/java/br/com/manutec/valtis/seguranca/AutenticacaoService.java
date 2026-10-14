package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioRepository;
import br.com.manutec.valtis.usuario.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Login (RF-01). */
@Service
public class AutenticacaoService {

    /** Erro único para e-mail inexistente, senha errada e usuário inativo: não revela qual dos três. */
    public static class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException() {
            super("E-mail ou senha incorretos.");
        }
    }

    public static class MuitasTentativasException extends RuntimeException {
        public MuitasTentativasException(long minutos) {
            super("Muitas tentativas erradas. Tente de novo em %d minuto(s).".formatted(minutos));
        }
    }

    public record Sessao(EmissorDeToken.Token token, Usuario usuario) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificador;
    private final EmissorDeToken emissor;
    private final ProtecaoContraForcaBruta protecao;
    /**
     * Hash de uma senha qualquer, calculado uma vez. Quando o e-mail não existe,
     * comparamos contra ele mesmo assim: a resposta leva o mesmo tempo nos dois casos
     * e ninguém descobre quais e-mails estão cadastrados medindo o relógio.
     */
    private final String hashDeDisfarce;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder codificador,
                               EmissorDeToken emissor, ProtecaoContraForcaBruta protecao) {
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
        this.emissor = emissor;
        this.protecao = protecao;
        this.hashDeDisfarce = codificador.encode("senha-de-disfarce-para-tempo-constante");
    }

    @Transactional(readOnly = true)
    public Sessao entrar(String email, String senha) {
        String chave = UsuarioService.normalizar(email);
        long minutos = protecao.minutosBloqueado(chave);
        if (minutos > 0) {
            throw new MuitasTentativasException(minutos);
        }

        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(chave);
        boolean senhaConfere = codificador.matches(senha, encontrado.map(Usuario::getSenhaHash).orElse(hashDeDisfarce));

        if (encontrado.isEmpty() || !senhaConfere || !encontrado.get().isAtivo()) {
            protecao.registrarFalha(chave);
            throw new CredenciaisInvalidasException();
        }

        protecao.registrarSucesso(chave);
        Usuario usuario = encontrado.get();
        return new Sessao(emissor.emitir(usuario), usuario);
    }
}
