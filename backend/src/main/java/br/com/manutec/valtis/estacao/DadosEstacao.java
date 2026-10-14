package br.com.manutec.valtis.estacao;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de criação e edição de estação (RF-07). O cliente vem na URL. */
public record DadosEstacao(
        @NotBlank(message = "Informe a localização da estação.")
        @Size(max = 200)
        String localizacaoInstalacao,
        @NotNull(message = "Informe o andar inicial.") Integer andarInicial,
        @NotNull(message = "Informe o andar final.") Integer andarFinal,
        // Boolean, e não boolean: o Jackson 3 recusa campo primitivo ausente. Ausente = não exige.
        Boolean requerFechamentoGeral
) {

    /** Espelha a constraint ck_estacao_andares, para a mensagem sair amigável. */
    @AssertTrue(message = "O andar final não pode ser abaixo do andar inicial.")
    public boolean isFaixaDeAndaresValida() {
        return andarInicial == null || andarFinal == null || andarFinal >= andarInicial;
    }
}
