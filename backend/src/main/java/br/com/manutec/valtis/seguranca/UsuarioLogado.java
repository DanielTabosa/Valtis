package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.usuario.Perfil;

import java.util.UUID;

/**
 * Quem está fazendo a requisição. Os controllers recebem com
 * `@AuthenticationPrincipal UsuarioLogado eu`.
 *
 * Montado a cada requisição a partir do banco, não só do token:
 * usuário inativado perde o acesso na hora, mesmo com token ainda válido.
 */
public record UsuarioLogado(UUID id, String nome, String email, Perfil perfil) {
}
