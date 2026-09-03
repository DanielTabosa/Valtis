package br.com.manutec.valtis.valvula;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Projeção do Spring Data: uma interface cujos getters casam com os apelidos
 * (AS) da consulta. O Spring gera a implementação sozinho.
 */
public interface PainelProjecao {

    UUID getValvulaId();
    String getCodigoReferencia();
    String getNumeroValvula();
    Integer getPeriodicidadeMeses();
    String getNomeCondominio();
    String getBairro();
    String getLocalizacaoInstalacao();
    Integer getAndarInicial();
    Integer getAndarFinal();
    Boolean getRequerFechamentoGeral();
    String getEspecificacao();
    LocalDate getUltimaManutencao();
}
