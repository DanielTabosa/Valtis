package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.usuario.Perfil;
import br.com.manutec.valtis.usuario.UsuarioRepository;
import br.com.manutec.valtis.usuario.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Resolve o "ovo e a galinha": para cadastrar usuários é preciso estar logado
 * como administrador, mas no primeiro dia não existe nenhum.
 *
 * Se VALTIS_ADMIN_EMAIL e VALTIS_ADMIN_SENHA estiverem definidas e esse e-mail
 * ainda não existir, cria o administrador na partida. Depois do primeiro login,
 * troque a senha pela tela e remova as variáveis.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final ValtisPropriedades propriedades;
    private final UsuarioRepository repository;
    private final UsuarioService usuarios;

    public AdminInicial(ValtisPropriedades propriedades, UsuarioRepository repository, UsuarioService usuarios) {
        this.propriedades = propriedades;
        this.repository = repository;
        this.usuarios = usuarios;
    }

    @Override
    public void run(ApplicationArguments args) {
        ValtisPropriedades.AdminInicial admin = propriedades.seguranca().adminInicial();
        if (admin == null || !admin.configurado()) {
            return;
        }
        if (repository.existsByEmailIgnoreCase(UsuarioService.normalizar(admin.email()))) {
            return;
        }
        String nome = admin.nome() == null || admin.nome().isBlank() ? "Administrador" : admin.nome();
        usuarios.criar(nome, admin.email(), Perfil.ADMIN, admin.senha());
        log.info("Administrador inicial criado a partir das variáveis de ambiente.");
    }
}
