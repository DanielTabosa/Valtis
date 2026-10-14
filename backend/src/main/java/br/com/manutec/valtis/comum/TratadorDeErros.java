package br.com.manutec.valtis.comum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz exceções em respostas HTTP padronizadas (RFC 9457, "problem details").
 *
 * Cada resposta de erro tem o mesmo formato, então o frontend mostra a mensagem
 * sem precisar adivinhar a estrutura. As mensagens nunca carregam dado pessoal
 * de cliente (AGENTS.md §8).
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Campos inválidos no corpo da requisição (@Valid). Devolve a lista campo → mensagem. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Há campos inválidos no formulário.");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoIlegivel(HttpMessageNotReadableException e) {
        // Em debug: a mensagem do Jackson pode citar o valor enviado, que pode ser dado pessoal.
        log.debug("Corpo ilegível: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.BAD_REQUEST, "Requisição com formato inválido. Confira datas e valores de lista.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName());
    }

    /**
     * Rede de segurança: uma constraint do banco barrou o que a validação do
     * serviço deixou passar (ex.: dois cadastros simultâneos com o mesmo código).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade no banco: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "O registro conflita com outro já existente. Atualize a tela e tente de novo.");
    }

    private ProblemDetail problema(HttpStatus status, String mensagem) {
        return ProblemDetail.forStatusAndDetail(status, mensagem);
    }
}
