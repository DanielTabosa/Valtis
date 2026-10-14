package br.com.manutec.valtis.painel;

import br.com.manutec.valtis.recorrencia.StatusPainel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painelService;

    public PainelController(PainelService painelService) {
        this.painelService = painelService;
    }

    /** Linhas e resumo numa resposta só: o resumo respeita os mesmos filtros (RF-38, RF-39). */
    @GetMapping
    public PainelService.Painel montar(@RequestParam(required = false) String busca,
                                       @RequestParam(required = false) StatusPainel status,
                                       @RequestParam(required = false) String bairro,
                                       @RequestParam(required = false) String tipoAtendimento,
                                       @RequestParam(required = false) UUID clienteId) {
        return painelService.montar(new FiltroPainel(busca, status, bairro, tipoAtendimento, clienteId));
    }
}
