package br.com.manutec.valtis.usuario;

import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Gestão de usuários (RF-03) e troca de senha. */
@Service
public class UsuarioService {

    public record UsuarioResposta(UUID id, String nome, String email, Perfil perfil, boolean ativo) {

        public static UsuarioResposta de(Usuario u) {
            return new UsuarioResposta(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo());
        }
    }

    /** Versão enxuta para listas de escolha, como "técnico responsável" no lançamento. */
    public record UsuarioOpcao(UUID id, String nome, Perfil perfil) {}

    private final UsuarioRepository repository;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder codificador) {
        this.repository = repository;
        this.codificador = codificador;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResposta> listar() {
        return repository.findAllByOrderByNome().stream().map(UsuarioResposta::de).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioOpcao> ativos() {
        return repository.findByAtivoTrueOrderByNome().stream()
                .map(u -> new UsuarioOpcao(u.getId(), u.getNome(), u.getPerfil()))
                .toList();
    }

    @Transactional
    public UsuarioResposta criar(String nome, String email, Perfil perfil, String senha) {
        String emailNormalizado = normalizar(email);
        if (repository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }
        Usuario usuario = new Usuario(nome.strip(), emailNormalizado, codificador.encode(senha), perfil);
        return UsuarioResposta.de(repository.save(usuario));
    }

    @Transactional
    public UsuarioResposta atualizar(UUID id, String nome, Perfil perfil, UUID quemAltera) {
        Usuario usuario = buscar(id);
        if (id.equals(quemAltera) && perfil != usuario.getPerfil()) {
            throw new RegraDeNegocioException("Você não pode mudar o seu próprio perfil.");
        }
        usuario.alterar(nome.strip(), perfil);
        return UsuarioResposta.de(usuario);
    }

    /** O administrador define uma senha nova para alguém que esqueceu a sua. */
    @Transactional
    public void redefinirSenha(UUID id, String novaSenha) {
        buscar(id).trocarSenha(codificador.encode(novaSenha));
    }

    @Transactional
    public void trocarPropriaSenha(UUID id, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarAtivo(id);
        if (!codificador.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A senha atual não confere.");
        }
        usuario.trocarSenha(codificador.encode(novaSenha));
    }

    /** Inativar corta o acesso na hora: o token é conferido contra o banco a cada requisição. */
    @Transactional
    public void inativar(UUID id, UUID quemInativa) {
        if (id.equals(quemInativa)) {
            throw new RegraDeNegocioException("Você não pode inativar a si mesmo.");
        }
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public Usuario buscarAtivo(UUID id) {
        Usuario usuario = buscar(id);
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Usuário inativo.");
        }
        return usuario;
    }

    private Usuario buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    public static String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
