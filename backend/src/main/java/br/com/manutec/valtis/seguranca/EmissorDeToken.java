package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite o token de acesso.
 *
 * Leia o token como um crachá assinado: diz quem é a pessoa e até quando vale.
 * Qualquer um consegue LER o conteúdo (não é criptografado), mas só o servidor,
 * que tem a chave, consegue ASSINAR. Por isso o token não leva nada sigiloso.
 */
@Component
public class EmissorDeToken {

    public record Token(String valor, Instant expiraEm) {}

    private final JwtEncoder encoder;
    private final Duration validade;
    private final Clock relogio;

    public EmissorDeToken(JwtEncoder encoder, ValtisPropriedades propriedades, Clock relogio) {
        this.encoder = encoder;
        this.validade = Duration.ofHours(propriedades.seguranca().validadeTokenHoras());
        this.relogio = relogio;
    }

    public Token emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validade);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SegurancaConfig.EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("nome", usuario.getNome())
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new Token(valor, expiraEm);
    }
}
