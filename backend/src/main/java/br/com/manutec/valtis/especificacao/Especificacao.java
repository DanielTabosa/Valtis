package br.com.manutec.valtis.especificacao;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Catálogo técnico reutilizável: marca + modelo + diâmetro + tipo de registro.
 * Cadastra-se a combinação uma vez; várias válvulas apontam para ela.
 */
@Entity
@Table(name = "especificacao")
public class Especificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    /** Texto, não número: existe 1.1/2 e 3/4. */
    @Column(name = "diametro_polegadas", nullable = false)
    private String diametroPolegadas;

    @Column(name = "tipo_registro", nullable = false)
    private String tipoRegistro;

    @Column(name = "descricao_completa", nullable = false)
    private String descricaoCompleta;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Especificacao() {
    }

    public Especificacao(String marca, String modelo, String diametroPolegadas, String tipoRegistro) {
        this.marca = marca;
        this.modelo = modelo;
        this.diametroPolegadas = diametroPolegadas;
        this.tipoRegistro = tipoRegistro;
        this.descricaoCompleta = montarDescricao(marca, modelo, diametroPolegadas, tipoRegistro);
    }

    /** Ex.: Bermad 720 - 2" Esfera */
    private static String montarDescricao(String marca, String modelo, String diametro, String tipo) {
        return "%s %s - %s\" %s".formatted(marca, modelo, diametro, tipo);
    }

    public UUID getId() { return id; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getDiametroPolegadas() { return diametroPolegadas; }
    public String getTipoRegistro() { return tipoRegistro; }
    public String getDescricaoCompleta() { return descricaoCompleta; }
    public boolean isAtivo() { return ativo; }
}
