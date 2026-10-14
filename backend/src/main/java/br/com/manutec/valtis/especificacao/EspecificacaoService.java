package br.com.manutec.valtis.especificacao;

import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.Textos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Catálogo de especificações técnicas (RF-06). */
@Service
public class EspecificacaoService {

    private final EspecificacaoRepository repository;

    public EspecificacaoService(EspecificacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Especificacao> listar(boolean incluirInativas) {
        return incluirInativas
                ? repository.findAllByOrderByMarcaAscModeloAscDiametroPolegadasAsc()
                : repository.findByAtivoTrueOrderByMarcaAscModeloAscDiametroPolegadasAsc();
    }

    @Transactional
    public Especificacao criar(DadosEspecificacao dados) {
        DadosEspecificacao limpos = limpar(dados);
        garantirQueNaoExiste(limpos, null);
        return repository.save(new Especificacao(
                limpos.marca(), limpos.modelo(), limpos.diametroPolegadas(), limpos.tipoRegistro()));
    }

    @Transactional
    public Especificacao atualizar(UUID id, DadosEspecificacao dados) {
        Especificacao especificacao = buscar(id);
        DadosEspecificacao limpos = limpar(dados);
        garantirQueNaoExiste(limpos, id);
        especificacao.atualizar(limpos.marca(), limpos.modelo(), limpos.diametroPolegadas(), limpos.tipoRegistro());
        return especificacao;
    }

    @Transactional
    public void inativar(UUID id) {
        buscar(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        buscar(id).reativar();
    }

    public Especificacao buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Especificação não encontrada."));
    }

    private void garantirQueNaoExiste(DadosEspecificacao dados, UUID idAtual) {
        repository.findByMarcaIgnoreCaseAndModeloIgnoreCaseAndDiametroPolegadasAndTipoRegistro(
                        dados.marca(), dados.modelo(), dados.diametroPolegadas(), dados.tipoRegistro())
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflitoException("Já existe a especificação " + existente.getDescricaoCompleta() + ".");
                });
    }

    private DadosEspecificacao limpar(DadosEspecificacao dados) {
        return new DadosEspecificacao(
                Textos.limpar(dados.marca()),
                Textos.limpar(dados.modelo()),
                Textos.limpar(dados.diametroPolegadas()),
                dados.tipoRegistro());
    }
}
