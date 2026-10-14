package br.com.manutec.valtis.oportunidade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Registro de desfecho (RF-41).
 *
 * Na recusa, o sistema PERGUNTA se e quando voltar a lembrar (RN-09, D-05):
 * `lembrarNovamente` é obrigatório nesse caso, e `lembrarEm` quando a resposta é sim.
 */
public record DesfechoRequisicao(
        @NotNull(message = "Escolha o desfecho.") StatusOportunidade status,
        @Size(max = 2000) String notas,
        @Size(max = 200) String motivoRecusa,
        Boolean lembrarNovamente,
        LocalDate lembrarEm
) {
}
