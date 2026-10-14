package br.com.manutec.valtis.especificacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DadosEspecificacao(
        @NotBlank(message = "Informe a marca.") @Size(max = 80) String marca,
        @NotBlank(message = "Informe o modelo.") @Size(max = 80) String modelo,
        @NotBlank(message = "Informe o diâmetro.") @Size(max = 20) String diametroPolegadas,
        @NotNull(message = "Informe o tipo de registro.") TipoRegistro tipoRegistro
) {
}
