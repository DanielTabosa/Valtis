package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Transforma um token válido em "quem é o usuário".
 *
 * A assinatura prova que o token foi emitido por nós; mas o perfil e o
 * status ativo são lidos do BANCO, não do token. Assim, inativar alguém ou
 * trocar o perfil vale na próxima requisição, sem esperar o token expirar.
 * O custo é uma consulta por requisição, irrelevante para 3 usuários.
 */
@Component
public class ConversorDeToken implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public ConversorDeToken(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarioRepository.findById(UUID.fromString(jwt.getSubject()))
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new DisabledException("Usuário inativo ou inexistente."));

        UsuarioLogado logado = new UsuarioLogado(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil());
        return UsernamePasswordAuthenticationToken.authenticated(logado, jwt,
                List.of(new SimpleGrantedAuthority(usuario.getPerfil().papel())));
    }
}
