package br.com.manutec.valtis.comum;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-70: a tela mostra o modo ativo e esconde o que não vale nele. */
@RestController
public class SistemaController {

    public record Sistema(String modoCadastros) {}

    private final ModoDeOperacao modo;

    public SistemaController(ModoDeOperacao modo) {
        this.modo = modo;
    }

    @GetMapping("/api/sistema")
    public Sistema sistema() {
        return new Sistema(modo.isExterna() ? "externa" : "propria");
    }
}
