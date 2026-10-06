package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Precio y cantidades de origen se congelan al preparar la venta. */
public final class DetalleVenta {
    private final Producto producto;
    private final BigDecimal cantidad;
    // Se guarda el precio del momento: si el producto cambia de precio, la venta no se altera.
    private final BigDecimal precioUnitario;
    // De qué lotes sale la cantidad vendida.
    private final List<AsignacionLote> asignaciones;

    /**
     * Una línea de la venta: un producto, su cantidad y los lotes de donde sale.
     *
     * <p>Valida que el plan de lotes sea coherente:
     * <ul>
     *   <li>Todos los lotes son del mismo producto del detalle (no se puede vender res con un lote de cerdo).</li>
     *   <li>Ningún lote se repite ({@code codigos.add} devuelve false si el código ya estaba en el Set).</li>
     *   <li>La suma de las asignaciones es igual a la cantidad del detalle.
     *       Se usa {@code compareTo} y no {@code equals} porque con BigDecimal
     *       {@code new BigDecimal("8.0").equals(new BigDecimal("8"))} es falso (distinta escala), pero {@code compareTo} los ve iguales.</li>
     * </ul>
     *
     * <p>El precio se copia del producto en este momento; si después cambia, la venta conserva el original.
     */
    public DetalleVenta(Producto producto, BigDecimal cantidad, List<AsignacionLote> asignaciones) {
        if (producto == null || asignaciones == null || asignaciones.isEmpty()) {
            throw new ReglaNegocioException("El detalle requiere producto y asignaciones de origen.");
        }
        producto.validarCantidad(cantidad);
        // Cada lote debe ser del mismo producto, sin repetirse, y entre todos sumar la cantidad pedida.
        BigDecimal suma = BigDecimal.ZERO;
        Set<String> codigos = new HashSet<>();
        for (AsignacionLote asignacion : asignaciones) {
            if (asignacion == null) {
                throw new ReglaNegocioException("Las asignaciones no pueden ser nulas.");
            }
            Lote lote = asignacion.getLote();
            if (!lote.getProducto().getCodigo().equals(producto.getCodigo()) || !codigos.add(lote.getCodigo())) {
                throw new ReglaNegocioException("Los lotes del detalle deben ser del producto y no repetirse.");
            }
            suma = suma.add(asignacion.getCantidad());
        }
        if (suma.compareTo(cantidad) != 0) {
            throw new ReglaNegocioException("Las asignaciones deben sumar la cantidad del detalle.");
        }
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        this.asignaciones = List.copyOf(asignaciones);
    }

    // Precio guardado por cantidad, redondeado a 2 decimales.
    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }

    public Producto getProducto() { return producto; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public List<AsignacionLote> getAsignaciones() { return asignaciones; }
}
