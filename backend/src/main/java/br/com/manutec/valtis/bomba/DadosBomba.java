package br.com.manutec.valtis.bomba;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo de criação e edição de bomba (RF-62). Quase tudo é opcional (RF-63);
 * a periodicidade é obrigatória e sem padrão até P-13.
 */
public record DadosBomba(
        @Size(max = 80) String fabricante,
        @Size(max = 80) String modelo,
        @DecimalMin(value = "0.01", message = "A potência deve ser maior que zero.")
        @Digits(integer = 4, fraction = 2) BigDecimal potenciaCv,
        @Min(value = 1, message = "A tensão deve ser maior que zero.") Integer tensaoVolts,
        @DecimalMin(value = "0.01", message = "A corrente deve ser maior que zero.")
        @Digits(integer = 4, fraction = 2) BigDecimal correnteAmperes,
        @Size(max = 60) String funcao,
        @PastOrPresent(message = "A data de instalação não pode estar no futuro.") LocalDate dataInstalacao,
        @NotNull(message = "Informe a periodicidade da manutenção.")
        @Min(value = 1, message = "A periodicidade mínima é 1 mês.")
        @Max(value = 60, message = "A periodicidade máxima é 60 meses.")
        Integer periodicidadeMeses,
        @PastOrPresent(message = "A última manutenção não pode estar no futuro.") LocalDate ultimaManutencao,
        /** Só na criação em um cliente: já instala a bomba neste conjunto. */
        UUID conjuntoId
) {
}
