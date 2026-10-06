package co.trazacarne.dominio;

/** Tipos de movimiento que quedan en el historial del inventario. */
public enum TipoMovimiento {
    // ENTRADA = compra, SALIDA = venta, DEVOLUCION = venta anulada, PERDIDA = merma o daño.
    ENTRADA, SALIDA, DEVOLUCION, PERDIDA
}
