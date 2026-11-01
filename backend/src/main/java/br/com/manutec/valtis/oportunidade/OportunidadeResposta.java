package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.comum.OrigemRegistro;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma oportunidade como a fila mostra.
 *
 * `semDonoUrgente` (RF-43) e `semLembreteNaAgenda` vêm calculados do servidor: a tela só pinta.
 * O contato do síndico vai junto porque a fila existe para ligar para ele;
 * a rota é autenticada (RNF-23).
 */
public record OportunidadeResposta(
        UUID id,
        EscopoOportunidade escopo,
        StatusOportunidade status,
        boolean aberta,
        OrigemRegistro origem,
        LocalDate dataPrevista,
        LocalDate dataAlertaComercial,
        LocalDate dataLembreteFinal,
        Long diasParaVencimento,
        boolean semDonoUrgente,
        boolean semLembreteNaAgenda,
        UUID clienteId,
        String codigoCliente,
        String nomeCondominio,
        String bairro,
        String sindicoResponsavel,
        String telefoneContato,
        UUID valvulaId,
        String codigoValvula,
        String localizacaoInstalacao,
        String andares,
        Boolean requerFechamentoGeral,
        UUID responsavelId,
        String responsavelNome,
        OffsetDateTime assumidaEm,
        String notas,
        String motivoRecusa,
        OffsetDateTime encerradaEm,
        UUID bombaId,
        String codigoBomba,
        String descricaoBomba
) {
}
