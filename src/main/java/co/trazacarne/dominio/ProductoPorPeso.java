package co.trazacarne.dominio;

import co.trazacarne.excepcion.CantidadInvalidaException;

import java.math.BigDecimal;

/** Producto cuya cantidad se expresa en kilogramos, con hasta tres decimales. */
public class ProductoPorPeso extends Producto {

    public ProductoPorPeso(String codigo, String nombre, String especie, String tipoCorte, BigDecimal precio) {
        super(codigo, nombre, especie, tipoCorte, precio);
    }

    // Polimorfismo: acepta decimales (ej. 1.250 kg), pero máximo 3 (gramos).
    @Override
    public void validarCantidad(BigDecimal cantidad) {
        validarCantidadPositiva(cantidad);
        if (cantidad.stripTrailingZeros().scale() > 3) {
            throw new CantidadInvalidaException("El peso en kilogramos admite como máximo tres decimales.");
        }
    }
}
