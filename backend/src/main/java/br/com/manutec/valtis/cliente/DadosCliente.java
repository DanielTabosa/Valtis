package br.com.manutec.valtis.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e edição de cliente (RF-05).
 * O código de referência não está aqui: quem gera é o sistema (RF-09).
 */
public record DadosCliente(
        @NotBlank(message = "Informe o nome do condomínio.")
        @Size(max = 200)
        String nomeCondominio,
        @Size(max = 300) String endereco,
        @Size(max = 100) String bairro,
        @Size(max = 100) String cidade,
        @Size(max = 200) String sindicoResponsavel,
        @Size(max = 40) String telefoneContato,
        @Email(message = "E-mail inválido.")
        @Size(max = 200)
        String emailContato,
        TipoAtendimento tipoAtendimento,
        StatusContrato statusContrato
) {
}
