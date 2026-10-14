package br.com.manutec.valtis.valvula;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo de criação e edição de válvula (RF-08). A estação vem na URL.
 *
 * `numeroValvula` é opcional na criação: vazio, o sistema usa o próximo da estação.
 * `periodicidadeMeses` vazio = 12 (RN-02).
 */
public record DadosValvula(
        @Pattern(regexp = "^[0-9]{1,3}$", message = "O número da válvula deve ser 01, 02, 03...")
        String numeroValvula,
        @NotNull(message = "Escolha a especificação.") UUID especificacaoId,
        @Size(max = 80) String numeroSerie,
        @PastOrPresent(message = "A data de instalação não pode estar no futuro.") LocalDate dataInstalacao,
        @Min(value = 1, message = "A periodicidade mínima é 1 mês.")
        @Max(value = 60, message = "A periodicidade máxima é 60 meses.")
        Integer periodicidadeMeses
) {
}
