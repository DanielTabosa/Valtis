package br.com.manutec.valtis.valvula;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValvulaRepository extends JpaRepository<Valvula, UUID> {

    boolean existsByEstacaoIdAndNumeroValvula(UUID estacaoId, String numeroValvula);

    Optional<Valvula> findByEstacaoIdAndNumeroValvula(UUID estacaoId, String numeroValvula);

    /** Válvulas de várias estações de uma vez, já com a especificação, para a tela do cliente. */
    @Query("""
            SELECT v FROM Valvula v LEFT JOIN FETCH v.especificacao
            WHERE v.estacao.id IN :estacoes
            ORDER BY v.numeroValvula
            """)
    List<Valvula> buscarDasEstacoes(@Param("estacoes") Collection<UUID> estacoes);

    /** Válvula com estação, cliente e especificação carregados numa consulta só. */
    @Query("""
            SELECT v FROM Valvula v
            JOIN FETCH v.estacao e JOIN FETCH e.cliente LEFT JOIN FETCH v.especificacao
            WHERE v.id = :id
            """)
    Optional<Valvula> buscarCompleta(@Param("id") UUID id);

    /**
     * Maior sequencial já usado nos códigos COND-001-VRP-NN do cliente (RN-14).
     * Inclui válvulas inativas: código não se reaproveita.
     */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(v.codigo_referencia FROM '-VRP-([0-9]+)$') AS INTEGER)), 0)
            FROM valvula v
                     JOIN estacao e ON e.id = v.estacao_id
            WHERE e.cliente_id = :clienteId
            """, nativeQuery = true)
    int maiorSequencialDoCliente(@Param("clienteId") UUID clienteId);

    /** Maior número de válvula (01, 02...) já usado na estação, inclusive inativas. */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(numero_valvula AS INTEGER)), 0)
            FROM valvula
            WHERE estacao_id = :estacaoId
              AND numero_valvula ~ '^[0-9]+$'
            """, nativeQuery = true)
    int maiorNumeroNaEstacao(@Param("estacaoId") UUID estacaoId);

    /**
     * Dados crus do painel: uma linha por válvula ativa, com a data do último
     * serviço concluído (nula quando nunca houve manutenção → SEM REGISTRO).
     *
     * A consulta NÃO calcula status nem dias restantes. Isso é regra de negócio
     * (RN-04) e mora na camada de casos de uso, não no SQL.
     */
    // Apelidos entre aspas: sem elas o Postgres rebaixa tudo para minúsculas
    // ("valvulaId" viraria "valvulaid") e a projeção não encontra as colunas.
    @Query(value = """
            SELECT  v.id                      AS "valvulaId",
                    e.id                      AS "estacaoId",
                    c.id                      AS "clienteId",
                    v.codigo_referencia       AS "codigoReferencia",
                    v.numero_valvula          AS "numeroValvula",
                    v.numero_serie            AS "numeroSerie",
                    v.periodicidade_meses     AS "periodicidadeMeses",
                    c.nome_condominio         AS "nomeCondominio",
                    c.bairro                  AS "bairro",
                    c.tipo_atendimento        AS "tipoAtendimento",
                    e.localizacao_instalacao  AS "localizacaoInstalacao",
                    e.andar_inicial           AS "andarInicial",
                    e.andar_final             AS "andarFinal",
                    e.requer_fechamento_geral AS "requerFechamentoGeral",
                    COALESCE(esp.descricao_completa,
                             NULLIF(CONCAT_WS(' ', v.fabricante, v.modelo), ''),
                             'especificação a completar') AS "especificacao",
                    MAX(s.data_realizada)     AS "ultimaManutencao",
                    -- Quem fez o último serviço ativo (RF-38): usuário do Valtis ou técnico do Jiraya.
                    (SELECT COALESCE(u.nome, ult.tecnico_externo)
                     FROM servico ult
                              LEFT JOIN usuario u ON u.id = ult.tecnico_responsavel_id
                     WHERE ult.valvula_id = v.id
                       AND ult.status = 'concluido'
                       AND ult.ativo = TRUE
                     ORDER BY ult.data_realizada DESC, ult.criado_em DESC
                     LIMIT 1)                 AS "ultimoTecnico"
            FROM valvula v
                     JOIN estacao e         ON e.id = v.estacao_id
                     JOIN cliente c         ON c.id = e.cliente_id
                     LEFT JOIN especificacao esp ON esp.id = v.especificacao_id
                     LEFT JOIN servico s    ON s.valvula_id = v.id
                                           AND s.status = 'concluido'
                                           AND s.ativo = TRUE
            WHERE v.ativo = TRUE
              AND e.ativo = TRUE
              AND c.ativo = TRUE
            GROUP BY v.id, e.id, c.id, esp.id
            """, nativeQuery = true)
    List<PainelProjecao> buscarDadosDoPainel();

    Optional<Valvula> findByJirayaEquipamentoId(Long jirayaEquipamentoId);
}
