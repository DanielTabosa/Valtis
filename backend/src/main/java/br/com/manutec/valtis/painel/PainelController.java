package br.com.manutec.valtis.painel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painelService;

    public PainelController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping
    public List<LinhaPainel> listar() {
        return painelService.listar();
    }

    @GetMapping("/resumo")
    public Map<String, Long> resumo() {
        return painelService.resumo();
    }
}
