package co.trazacarne.dominio;

import co.trazacarne.excepcion.CantidadInvalidaException;
import co.trazacarne.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Datos comunes del producto y contrato de cantidad según su forma de venta (RN-06). */
public abstract class Producto {

    private final String codigo;
    private final String nombre;
    private final String especie;
    private final String tipoCorte;
    private BigDecimal precio;
    private boolean activo;

    protected Producto(String codigo, String nombre, String especie, String tipoCorte, BigDecimal precio) {
        this.codigo = validarTexto(codigo, "El código del producto");
        this.nombre = validarTexto(nombre, "El nombre del producto");
        this.especie = validarTexto(especie, "La especie");
        this.tipoCorte = validarTexto(tipoCorte, "El tipo de corte");
        this.precio = validarPrecio(precio);
        this.activo = true;
    }

    /** Rechaza cantidades nulas, no positivas o incompatibles con la forma de venta. */
    public abstract void validarCantidad(BigDecimal cantidad);

    public BigDecimal calcularSubtotal(BigDecimal cantidad) {
        validarCantidad(cantidad);
        return precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }

    public void actualizarPrecio(BigDecimal nuevoPrecio) {
        this.precio = validarPrecio(nuevoPrecio);
    }

    public void desactivar() {
        this.activo = false;
    }

    protected final void validarCantidadPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new CantidadInvalidaException("La cantidad debe ser mayor que cero.");
        }
    }

    private static String validarTexto(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return texto.strip();
    }

    private static BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new ReglaNegocioException("El precio debe ser mayor que cero.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("El precio admite como máximo dos decimales.");
        }
        return precio.setScale(2, RoundingMode.UNNECESSARY);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public String getTipoCorte() {
        return tipoCorte;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean estaActivo() {
        return activo;
    }
}
