package br.com.manutec.valtis.bomba;

import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Movimentações de bomba (RF-66): entrar numa posição, sair dela.
 * Aplica RN-17 a RN-20 sobre as ocupações (D-27).
 */
@Service
public class MovimentacaoService {

    /** Corpo de "colocar a bomba nesta posição". */
    public record DadosInstalacao(
            @NotNull(message = "Escolha o conjunto.") UUID conjuntoId,
            // Aceita vazio, como DadosValvula: a tela pode mandar "" em vez de omitir.
            @Pattern(regexp = "^([0-9]{1,3})?$", message = "A posição deve ser 01, 02, 03...") String numeroPosicao,
            @NotNull(message = "Informe a data de entrada.")
            @PastOrPresent(message = "A data de entrada não pode estar no futuro.") LocalDate inicio,
            UUID bombaSubstituidaId,
            @Size(max = 1000) String observacoes
    ) {}

    /** Corpo de "tirar a bomba da posição" (retirada ou devolução, conforme a natureza). */
    public record DadosEncerramento(
            @NotNull(message = "Informe a data de saída.")
            @PastOrPresent(message = "A data de saída não pode estar no futuro.") LocalDate fim,
            @Size(max = 1000) String observacoes
    ) {}

    /** O que foi registrado e, se houver, o aviso de RN-19. */
    public record Resultado(OcupacaoResposta ocupacao, String aviso) {}

    private final OcupacaoRepository ocupacaoRepository;
    private final BombaService bombaService;
    private final ConjuntoService conjuntoService;
    private final UsuarioService usuarios;

    public MovimentacaoService(OcupacaoRepository ocupacaoRepository, BombaService bombaService,
                               ConjuntoService conjuntoService, UsuarioService usuarios) {
        this.ocupacaoRepository = ocupacaoRepository;
        this.bombaService = bombaService;
        this.conjuntoService = conjuntoService;
        this.usuarios = usuarios;
    }

    @Transactional
    public Resultado instalar(UUID bombaId, DadosInstalacao dados, UUID registradoPorId) {
        Bomba bomba = bombaService.buscar(bombaId);
        ConjuntoBombeamento conjunto = conjuntoService.buscar(dados.conjuntoId());
        Usuario registradoPor = usuarios.buscarAtivo(registradoPorId);
        validarEntrada(bomba, conjunto);

        // RN-17: quem já está numa posição não entra em outra. Para o patrimônio, é "não disponível".
        List<OcupacaoBomba> abertas = ocupacaoRepository.abertasDaBomba(bombaId);
        if (!abertas.isEmpty()) {
            OcupacaoBomba atual = abertas.getFirst();
            throw new RegraDeNegocioException((bomba.isPatrimonioManutec()
                    ? "A bomba %s está emprestada a %s desde %s. Registre a devolução antes de emprestá-la de novo."
                    : "A bomba %s está na posição %s de %s desde %s. Registre a saída antes.")
                    .formatted(bomba.getCodigoReferencia(),
                            bomba.isPatrimonioManutec() ? atual.getConjunto().getCliente().getNomeCondominio()
                                    : atual.getNumeroPosicao(),
                            bomba.isPatrimonioManutec() ? atual.getInicio() : atual.getConjunto().getLocalizacao(),
                            atual.getInicio()));
        }

        Bomba substituida = dados.bombaSubstituidaId() == null ? null : bombaService.buscar(dados.bombaSubstituidaId());
        OcupacaoBomba ocupacaoDaSubstituida = substituida == null ? null : ocupacaoAbertaNoConjunto(substituida, conjunto);

        String posicao = dados.numeroPosicao() != null && !dados.numeroPosicao().isBlank()
                ? formatarPosicao(Integer.parseInt(dados.numeroPosicao().strip()))
                : ocupacaoDaSubstituida != null
                        ? ocupacaoDaSubstituida.getNumeroPosicao()
                        : formatarPosicao(ocupacaoRepository.maiorPosicaoNoConjunto(conjunto.getId()) + 1);

        // Substituir a bomba do próprio condomínio é retirá-la (ela foi para conserto).
        // Substituir uma emprestada NÃO a devolve: a devolução é sempre explícita (RN-18), e a
        // original pode voltar antes de a emprestada sair (RN-19).
        if (ocupacaoDaSubstituida != null && ocupacaoDaSubstituida.getNatureza() == NaturezaOcupacao.INSTALACAO) {
            ocupacaoDaSubstituida.encerrar(dados.inicio(), "Substituída por " + bomba.getCodigoReferencia());
        }

        OcupacaoBomba ocupacao = ocupacaoRepository.save(new OcupacaoBomba(bomba, conjunto, posicao, dados.inicio(),
                substituida, Textos.limpar(dados.observacoes()), registradoPor));
        ocupacaoRepository.flush();

        return new Resultado(OcupacaoResposta.de(ocupacao), avisoDePosicaoDupla(conjunto, posicao, ocupacao));
    }

    @Transactional
    public OcupacaoResposta encerrar(UUID ocupacaoId, DadosEncerramento dados) {
        OcupacaoBomba ocupacao = ocupacaoRepository.buscarCompleta(ocupacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocupação não encontrada."));
        ocupacao.encerrar(dados.fim(), Textos.limpar(dados.observacoes()));
        return OcupacaoResposta.de(ocupacao);
    }

    private static void validarEntrada(Bomba bomba, ConjuntoBombeamento conjunto) {
        if (!bomba.isAtivo()) {
            throw new RegraDeNegocioException("A bomba %s está inativa.".formatted(bomba.getCodigoReferencia()));
        }
        if (!conjunto.isAtivo() || !conjunto.getCliente().isAtivo()) {
            throw new RegraDeNegocioException("O conjunto ou o cliente está inativo.");
        }
        // Bomba de condomínio não vai para o conjunto de outro condomínio. Quem circula é o patrimônio.
        if (!bomba.isPatrimonioManutec() && !bomba.getCliente().getId().equals(conjunto.getCliente().getId())) {
            throw new RegraDeNegocioException("A bomba %s pertence a %s e não pode ser instalada em %s."
                    .formatted(bomba.getCodigoReferencia(), bomba.getCliente().getNomeCondominio(),
                            conjunto.getCliente().getNomeCondominio()));
        }
    }

    private OcupacaoBomba ocupacaoAbertaNoConjunto(Bomba substituida, ConjuntoBombeamento conjunto) {
        return ocupacaoRepository.abertasDaBomba(substituida.getId()).stream()
                .filter(o -> o.getConjunto().getId().equals(conjunto.getId()))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("A bomba %s não está neste conjunto agora."
                        .formatted(substituida.getCodigoReferencia())));
    }

    /** RN-19: duas abertas na mesma posição é permitido, mas quem registra precisa saber. */
    private String avisoDePosicaoDupla(ConjuntoBombeamento conjunto, String posicao, OcupacaoBomba nova) {
        List<String> outras = ocupacaoRepository.abertasDosConjuntos(List.of(conjunto.getId())).stream()
                .filter(o -> o.getNumeroPosicao().equals(posicao) && !o.getId().equals(nova.getId()))
                .map(o -> o.getBomba().getCodigoReferencia())
                .toList();
        if (outras.isEmpty()) {
            return null;
        }
        return "A posição %s também está ocupada por %s. Quando ela sair, registre a %s."
                .formatted(posicao, String.join(", ", outras),
                        nova.getNatureza() == NaturezaOcupacao.EMPRESTIMO ? "retirada dela" : "devolução dela");
    }

    private static String formatarPosicao(int numero) {
        if (numero < 1) {
            throw new RegraDeNegocioException("A posição começa em 01.");
        }
        return "%02d".formatted(numero);
    }
}
