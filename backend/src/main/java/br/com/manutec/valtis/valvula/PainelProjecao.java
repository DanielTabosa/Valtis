package br.com.manutec.valtis.valvula;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Projeção do Spring Data: uma interface cujos getters casam com os apelidos
 * (AS) da consulta. O Spring gera a implementação sozinho.
 */
public interface PainelProjecao {

    UUID getValvulaId();
    UUID getEstacaoId();
    UUID getClienteId();
    String getCodigoReferencia();
    String getNumeroValvula();
    String getNumeroSerie();
    Integer getPeriodicidadeMeses();
    String getNomeCondominio();
    String getBairro();
    String getTipoAtendimento();
    String getLocalizacaoInstalacao();
    Integer getAndarInicial();
    Integer getAndarFinal();
    Boolean getRequerFechamentoGeral();
    String getEspecificacao();
    LocalDate getUltimaManutencao();
    String getUltimoTecnico();
}
