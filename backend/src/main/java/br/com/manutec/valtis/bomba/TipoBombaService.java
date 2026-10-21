package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Catálogo de tipos de bombeamento (RF-60). Só o admin mexe; todos leem. */
@Service
public class TipoBombaService {

    private final TipoBombaRepository repository;

    public TipoBombaService(TipoBombaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TipoBomba> listar(boolean incluirInativos) {
        return incluirInativos ? repository.findAllByOrderByNome() : repository.findByAtivoTrueOrderByNome();
    }

    @Transactional
    public TipoBomba criar(String nome) {
        String limpo = nomeValido(nome);
        garantirNomeLivre(limpo, null);
        return repository.save(new TipoBomba(limpo));
    }

    @Transactional
    public TipoBomba renomear(UUID id, String nome) {
        TipoBomba tipo = buscar(id);
        String limpo = nomeValido(nome);
        garantirNomeLivre(limpo, id);
        tipo.renomear(limpo);
        return tipo;
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public TipoBomba buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de bomba não encontrado."));
    }

    private static String nomeValido(String nome) {
        String limpo = Textos.limpar(nome);
        if (limpo == null) {
            throw new RegraDeNegocioException("Informe o nome do tipo.");
        }
        return limpo;
    }

    private void garantirNomeLivre(String nome, UUID idAtual) {
        repository.findByNomeIgnoreCase(nome)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflitoException("Já existe o tipo \"%s\"%s."
                            .formatted(existente.getNome(), existente.isAtivo() ? "" : ", inativo"));
                });
    }
}
