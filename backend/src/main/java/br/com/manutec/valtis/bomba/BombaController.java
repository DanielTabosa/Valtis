package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.recorrencia.StatusPainel;
import br.com.manutec.valtis.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Bombas, patrimônio e movimentações. Técnico faz tudo, como com válvulas (P-19). */
@RestController
public class BombaController {

    private final BombaService service;
    private final BombaConsultaService consulta;
    private final MovimentacaoService movimentacao;
    private final Clock relogio;

    public BombaController(BombaService service, BombaConsultaService consulta, MovimentacaoService movimentacao,
                           Clock relogio) {
        this.service = service;
        this.consulta = consulta;
        this.movimentacao = movimentacao;
        this.relogio = relogio;
    }

    @GetMapping("/api/bombas/painel")
    public BombaConsultaService.PainelBombas painel(@RequestParam(required = false) StatusPainel status,
                                                    @RequestParam(required = false) String busca) {
        return consulta.painel(status, busca);
    }

    @GetMapping("/api/clientes/{clienteId}/bombas")
    public List<BombaResposta> doCliente(@PathVariable UUID clienteId) {
        return consulta.doCliente(clienteId);
    }

    @PostMapping("/api/clientes/{clienteId}/bombas")
    @ResponseStatus(HttpStatus.CREATED)
    public BombaResposta criarDoCondominio(@PathVariable UUID clienteId, @Valid @RequestBody DadosBomba dados,
                                           @AuthenticationPrincipal UsuarioLogado eu) {
        Bomba bomba = service.criarDoCondominio(clienteId, dados);
        // Atalho da tela: cadastrar e já colocar na posição. Entra na data de instalação, ou hoje.
        if (dados.conjuntoId() != null) {
            LocalDate inicio = dados.dataInstalacao() != null ? dados.dataInstalacao() : LocalDate.now(relogio);
            movimentacao.instalar(bomba.getId(),
                    new MovimentacaoService.DadosInstalacao(dados.conjuntoId(), null, inicio, null, null), eu.id());
        }
        return service.resposta(bomba);
    }

    @GetMapping("/api/patrimonio/bombas")
    public List<BombaResposta> patrimonio() {
        return consulta.patrimonio();
    }

    @PostMapping("/api/patrimonio/bombas")
    @ResponseStatus(HttpStatus.CREATED)
    public BombaResposta criarPatrimonial(@Valid @RequestBody DadosBomba dados) {
        return service.resposta(service.criarPatrimonial(dados));
    }

    @GetMapping("/api/bombas/{id}")
    public BombaConsultaService.BombaDetalhe detalhar(@PathVariable UUID id) {
        return consulta.detalhar(id);
    }

    @PutMapping("/api/bombas/{id}")
    public BombaResposta atualizar(@PathVariable UUID id, @Valid @RequestBody DadosBomba dados) {
        return service.atualizar(id, dados);
    }

    @PostMapping("/api/bombas/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }

    @PostMapping("/api/bombas/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable UUID id) {
        service.reativar(id);
    }

    @DeleteMapping("/api/bombas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }

    @PostMapping("/api/bombas/{id}/instalacao")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoService.Resultado instalar(@PathVariable UUID id,
                                                  @Valid @RequestBody MovimentacaoService.DadosInstalacao dados,
                                                  @AuthenticationPrincipal UsuarioLogado eu) {
        return movimentacao.instalar(id, dados, eu.id());
    }

    @PostMapping("/api/ocupacoes/{id}/encerramento")
    public OcupacaoResposta encerrar(@PathVariable UUID id,
                                     @Valid @RequestBody MovimentacaoService.DadosEncerramento dados) {
        return movimentacao.encerrar(id, dados);
    }
}
