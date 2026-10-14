package br.com.manutec.valtis.servico;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lançamento de uma visita: um item por válvula atendida (RF-14, RF-15, D-18).
 *
 * A "replicação" do RF-15b acontece na TELA: o técnico preenche uma vez e a
 * tela copia para as outras válvulas. Aqui cada item chega completo e vira um
 * registro independente. Tudo numa transação: ou entram todos, ou nenhum.
 */
public record LancamentoRequisicao(
        @NotEmpty(message = "Escolha pelo menos uma válvula.")
        @Size(max = 50)
        List<@Valid Item> itens
) {

    public record Item(
            @NotNull(message = "Válvula não informada.") UUID valvulaId,
            /* RF-17: sempre informada, nunca assumida como hoje. */
            @NotNull(message = "Informe a data em que o serviço foi realizado.") LocalDate dataRealizada,
            @NotNull(message = "Escolha o tipo de manutenção.") TipoManutencao tipoManutencao,
            @NotNull(message = "Escolha o técnico responsável.") UUID tecnicoResponsavelId,
            @Size(max = 1000) String pecasSubstituidas,
            @DecimalMin(value = "0.0", message = "Pressão não pode ser negativa.")
            @DecimalMax(value = "9999.99", message = "Pressão fora da faixa.")
            BigDecimal pressaoEntrada,
            @DecimalMin(value = "0.0", message = "Pressão não pode ser negativa.")
            @DecimalMax(value = "9999.99", message = "Pressão fora da faixa.")
            BigDecimal pressaoSaida,
            @Size(max = 2000) String observacoes
    ) {
    }
}
