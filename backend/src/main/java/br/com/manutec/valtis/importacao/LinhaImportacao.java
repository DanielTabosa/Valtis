package br.com.manutec.valtis.importacao;

/**
 * Uma linha da planilha, já com as colunas no lugar certo, ainda como texto.
 *
 * Fica em texto de propósito: é o que a tela mostra e deixa corrigir (RF-56).
 * Quem converte e valida é sempre o servidor, na análise e de novo na gravação.
 */
public record LinhaImportacao(
        int linha,
        String nomeCondominio,
        String endereco,
        String bairro,
        String cidade,
        String sindicoResponsavel,
        String telefoneContato,
        String emailContato,
        String tipoAtendimento,
        String ultimaManutencao
) {
}
