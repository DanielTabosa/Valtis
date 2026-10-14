package br.com.manutec.valtis.comum;

import jakarta.persistence.AttributeConverter;

/**
 * Converte um enum para o texto gravado no banco e de volta.
 * Cada enum do domínio declara a sua subclasse, de uma linha.
 */
public abstract class ConversorDeEnum<E extends Enum<E> & ValorDeBanco> implements AttributeConverter<E, String> {

    private final Class<E> tipo;

    protected ConversorDeEnum(Class<E> tipo) {
        this.tipo = tipo;
    }

    @Override
    public String convertToDatabaseColumn(E atributo) {
        return atributo == null ? null : atributo.valor();
    }

    @Override
    public E convertToEntityAttribute(String coluna) {
        return coluna == null ? null : ValorDeBanco.deValor(tipo, coluna);
    }
}
