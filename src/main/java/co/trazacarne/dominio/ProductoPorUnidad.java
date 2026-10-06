package co.trazacarne.dominio;

import co.trazacarne.excepcion.CantidadInvalidaException;

import java.math.BigDecimal;

/** Producto que se vende en cantidades enteras positivas (RN-06). */
public class ProductoPorUnidad extends Producto {

    public ProductoPorUnidad(String codigo, String nombre, String especie, String tipoCorte, BigDecimal precio) {
        super(codigo, nombre, especie, tipoCorte, precio);
    }

    // Polimorfismo: solo acepta números enteros (ej. 3 unidades, no 2.5).
    @Override
    public void validarCantidad(BigDecimal cantidad) {
        validarCantidadPositiva(cantidad);
        if (cantidad.stripTrailingZeros().scale() > 0) {
            throw new CantidadInvalidaException("Los productos por unidad requieren una cantidad entera.");
        }
    }
}
