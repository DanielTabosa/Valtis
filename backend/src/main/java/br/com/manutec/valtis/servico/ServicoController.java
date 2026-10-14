package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final LancamentoService lancamento;

    public ServicoController(LancamentoService lancamento) {
        this.lancamento = lancamento;
    }

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LancamentoService.ResultadoItem> lancar(@Valid @RequestBody LancamentoRequisicao requisicao,
                                                        @AuthenticationPrincipal UsuarioLogado eu) {
        return lancamento.lancar(requisicao, eu.id());
    }
}
