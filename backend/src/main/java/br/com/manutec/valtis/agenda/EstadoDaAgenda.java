package br.com.manutec.valtis.agenda;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Como foi a última rodada do worker. Fica em memória: é diagnóstico, não registro.
 *
 * O ponto principal é `erroDeAutorizacao`: token revogado é ALERTA, não
 * retentativa (architecture.md §3). A tela de sincronização mostra em destaque.
 */
@Component
public class EstadoDaAgenda {

    public record Retrato(OffsetDateTime ultimaExecucao, OffsetDateTime ultimoSucesso, String erroDeAutorizacao,
                          OffsetDateTime erroDeAutorizacaoEm) {}

    private OffsetDateTime ultimaExecucao;
    private OffsetDateTime ultimoSucesso;
    private String erroDeAutorizacao;
    private OffsetDateTime erroDeAutorizacaoEm;

    public synchronized void registrarExecucao(OffsetDateTime quando) {
        this.ultimaExecucao = quando;
    }

    public synchronized void registrarSucesso(OffsetDateTime quando) {
        this.ultimoSucesso = quando;
        this.erroDeAutorizacao = null;
        this.erroDeAutorizacaoEm = null;
    }

    public synchronized void registrarErroDeAutorizacao(String mensagem, OffsetDateTime quando) {
        this.erroDeAutorizacao = mensagem;
        this.erroDeAutorizacaoEm = quando;
    }

    public synchronized Retrato retrato() {
        return new Retrato(ultimaExecucao, ultimoSucesso, erroDeAutorizacao, erroDeAutorizacaoEm);
    }
}
