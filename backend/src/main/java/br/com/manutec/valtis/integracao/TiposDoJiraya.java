package br.com.manutec.valtis.integracao;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O nome atual de cada tipo de equipamento do Jiraya (jiraya.md, rota 1).
 *
 * O corpo do equipamento traz o nome do tipo do momento em que ele mudou pela
 * última vez. Se o tipo é renomeado depois ("Registro" → "VRP"), o equipamento
 * não muda de versão e o corpo guardado fica com o nome velho. Por isso a
 * classificação (RF-72) usa o nome daqui quando o tipo é conhecido.
 *
 * Fica em memória: a lista é relida no começo de toda rodada, antes de aplicar
 * qualquer evento. Até a primeira leitura, vale o nome que veio no corpo.
 */
@Component
public class TiposDoJiraya {

    private volatile Map<Long, String> nomes = Map.of();

    void atualizar(List<FonteExterna.TipoDeEquipamento> tipos) {
        this.nomes = tipos.stream()
                .filter(t -> t.nome() != null && !t.nome().isBlank())
                .collect(Collectors.toUnmodifiableMap(FonteExterna.TipoDeEquipamento::id,
                        FonteExterna.TipoDeEquipamento::nome, (primeiro, segundo) -> segundo));
    }

    String nomeAtual(Contratos.TipoDoJiraya tipo) {
        if (tipo == null) {
            return null;
        }
        return nomes.getOrDefault(tipo.id(), tipo.nome());
    }
}
