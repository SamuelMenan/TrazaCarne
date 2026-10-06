package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Hecho inmutable: el historial no se borra al anular una venta. */
public final class MovimientoInventario {
    // Todos los campos son final: un movimiento nunca se modifica.
    private final long id;
    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final Lote lote;
    private final BigDecimal cantidad;
    // Número de la compra o venta que lo originó (o el lote, si es una pérdida).
    private final String referencia;
    private final String motivo;

    // Sin "public": solo el Inventario crea movimientos.
    MovimientoInventario(long id, LocalDateTime fecha, TipoMovimiento tipo, Lote lote,
                         BigDecimal cantidad, String referencia, String motivo) {
        if (id <= 0 || fecha == null || tipo == null || lote == null) {
            throw new ReglaNegocioException("El movimiento requiere identidad, fecha, tipo y lote.");
        }
        lote.getProducto().validarCantidad(cantidad);
        if (referencia == null || referencia.isBlank() || motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("El movimiento requiere referencia y motivo.");
        }
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.lote = lote;
        this.cantidad = cantidad;
        this.referencia = referencia.strip();
        this.motivo = motivo.strip();
    }

    // --- Getters ---

    public long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public TipoMovimiento getTipo() { return tipo; }
    public Lote getLote() { return lote; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getReferencia() { return referencia; }
    public String getMotivo() { return motivo; }
}
