package br.com.manutec.valtis.valvula;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ValvulaRepository extends JpaRepository<Valvula, UUID> {

    List<Valvula> findByEstacaoIdAndAtivoTrue(UUID estacaoId);

    boolean existsByCodigoReferencia(String codigoReferencia);

    /**
     * Dados crus do painel: uma linha por válvula ativa, com a data do último
     * serviço (nula quando nunca houve manutenção → status SEM REGISTRO).
     *
     * A consulta NÃO calcula status nem dias restantes. Isso é regra de negócio
     * (RN-04) e mora na camada de casos de uso, não no SQL.
     */
    // Apelidos entre aspas: sem elas o Postgres rebaixa tudo para minúsculas
    // ("valvulaId" viraria "valvulaid") e a projeção não encontra as colunas.
    @Query(value = """
            SELECT  v.id                     AS "valvulaId",
                    v.codigo_referencia      AS "codigoReferencia",
                    v.numero_valvula         AS "numeroValvula",
                    v.periodicidade_meses    AS "periodicidadeMeses",
                    c.nome_condominio        AS "nomeCondominio",
                    c.bairro                 AS "bairro",
                    e.localizacao_instalacao AS "localizacaoInstalacao",
                    e.andar_inicial          AS "andarInicial",
                    e.andar_final            AS "andarFinal",
                    e.requer_fechamento_geral AS "requerFechamentoGeral",
                    esp.descricao_completa   AS "especificacao",
                    MAX(s.data_realizada)    AS "ultimaManutencao"
            FROM valvula v
                     JOIN estacao e        ON e.id = v.estacao_id
                     JOIN cliente c        ON c.id = e.cliente_id
                     JOIN especificacao esp ON esp.id = v.especificacao_id
                     LEFT JOIN servico s   ON s.valvula_id = v.id
                                          AND s.status = 'concluido'
            WHERE v.ativo = TRUE
              AND e.ativo = TRUE
              AND c.ativo = TRUE
            GROUP BY v.id, v.codigo_referencia, v.numero_valvula, v.periodicidade_meses,
                     c.nome_condominio, c.bairro, e.localizacao_instalacao,
                     e.andar_inicial, e.andar_final, e.requer_fechamento_geral,
                     esp.descricao_completa
            """, nativeQuery = true)
    List<PainelProjecao> buscarDadosDoPainel();
}
