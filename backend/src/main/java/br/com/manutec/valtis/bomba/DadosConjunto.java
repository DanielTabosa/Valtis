package br.com.manutec.valtis.bomba;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Corpo de criação e edição de conjunto de bombeamento (RF-61). O cliente vem na URL. */
public record DadosConjunto(
        @NotNull(message = "Escolha o tipo do conjunto.") UUID tipoId,
        @NotBlank(message = "Informe a localização do conjunto.") @Size(max = 200) String localizacao,
        @Size(max = 1000) String observacoes
) {
}
