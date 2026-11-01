package br.com.manutec.valtis.importacao;

import br.com.manutec.valtis.comum.ModoDeOperacao;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Importação de clientes da base antiga (RF-51 a RF-58). Só administrador. */
@RestController
@RequestMapping("/api/importacao/clientes")
@PreAuthorize("hasRole('ADMIN')")
public class ImportacaoController {

    public record Linhas(@NotNull List<LinhaImportacao> linhas) {}

    private final ImportacaoService service;
    private final ModoDeOperacao modo;

    public ImportacaoController(ImportacaoService service, ModoDeOperacao modo) {
        this.service = service;
        this.modo = modo;
    }

    /** RF-77, P-25: em externa a base histórica entra pelo Manutec Clientes e pelo Jiraya. */
    private void exigirModoProprio() {
        modo.exigirCadastroProprio("a importação de clientes", ModoDeOperacao.MANUTEC_CLIENTES);
    }

    /** Lê o arquivo e devolve a prévia. Nada é gravado. */
    @PostMapping(path = "/analise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacaoService.Analise analisar(@RequestParam("arquivo") MultipartFile arquivo) {
        exigirModoProprio();
        try {
            return service.analisar(arquivo.getOriginalFilename(), arquivo.getBytes());
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível receber o arquivo. Tente de novo.");
        }
    }

    /** Valida de novo as linhas corrigidas na tela. */
    @PostMapping("/revalidacao")
    public List<ImportacaoService.LinhaAnalisada> revalidar(@Valid @RequestBody Linhas corpo) {
        exigirModoProprio();
        return service.revalidar(corpo.linhas());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoService.Resultado importar(@Valid @RequestBody Linhas corpo) {
        exigirModoProprio();
        return service.importar(corpo.linhas());
    }
}
