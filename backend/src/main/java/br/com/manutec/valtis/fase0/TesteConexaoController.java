package br.com.manutec.valtis.fase0;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Prova que a aplicação lê do banco de verdade.
 *
 * Código de Fase 0 — será removido antes da Fase 1.
 */
@RestController
@RequestMapping("/api")
public class TesteConexaoController {

    private final TesteConexaoRepository repository;

    // Injeção por construtor: o Spring passa o repositório pronto ao criar
    // este controller. O campo é 'final', então nada consegue trocá-lo depois.
    public TesteConexaoController(TesteConexaoRepository repository) {
        this.repository = repository;
    }

    // DTO de saída. A API nunca devolve a entidade direto — ver explicação no chat.
    public record TesteConexaoResponse(Long id, String mensagem, OffsetDateTime criadoEm) {}

    @GetMapping("/teste-conexao")
    public List<TesteConexaoResponse> listar() {
        return repository.findAll()
                .stream()
                .map(t -> new TesteConexaoResponse(t.getId(), t.getMensagem(), t.getCriadoEm()))
                .toList();
    }
}
