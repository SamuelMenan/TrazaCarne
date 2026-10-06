package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;
import java.math.BigDecimal;

/** Cantidad de un detalle cuyo origen es un lote concreto. */
public final class AsignacionLote {
    private final Lote lote;
    private final BigDecimal cantidad;

    // Es la pieza clave de la trazabilidad: une una parte de la venta con su lote de origen.
    public AsignacionLote(Lote lote, BigDecimal cantidad) {
        if (lote == null) {
            throw new ReglaNegocioException("La asignación requiere un lote.");
        }
        // La cantidad debe ser válida para el producto del lote (peso o unidad).
        lote.getProducto().validarCantidad(cantidad);
        this.lote = lote;
        this.cantidad = cantidad;
    }

    public Lote getLote() { return lote; }
    public BigDecimal getCantidad() { return cantidad; }
}
