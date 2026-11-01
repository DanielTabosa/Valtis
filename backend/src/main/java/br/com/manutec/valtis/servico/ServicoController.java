package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    /** RN-26: motivo obrigatório. */
    public record InativacaoRequisicao(@NotBlank(message = "Informe o motivo.") @Size(max = 1000) String motivo) {}

    private final LancamentoService lancamento;
    private final InativacaoDeServicoService inativacao;

    public ServicoController(LancamentoService lancamento, InativacaoDeServicoService inativacao) {
        this.lancamento = lancamento;
        this.inativacao = inativacao;
    }

    @PostMapping("/{id}/inativacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void inativar(@PathVariable UUID id, @Valid @RequestBody InativacaoRequisicao requisicao,
                         @AuthenticationPrincipal UsuarioLogado eu) {
        inativacao.inativar(id, requisicao.motivo(), eu.id());
    }

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LancamentoService.ResultadoItem> lancar(@Valid @RequestBody LancamentoRequisicao requisicao,
                                                        @AuthenticationPrincipal UsuarioLogado eu) {
        return lancamento.lancar(requisicao, eu.id());
    }
}
