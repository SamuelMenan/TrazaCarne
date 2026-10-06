package co.trazacarne.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hecho inmutable del historial de un lote (RN-10).
 * referencia: número de la compra o venta que lo originó (ajuste del plan, etapa 4); puede ser null.
 */
public final class MovimientoInventario {

    private final int id;
    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final Lote lote;
    private final BigDecimal cantidad;
    private final String motivo;
    private final String referencia;

    public MovimientoInventario(int id, LocalDateTime fecha, TipoMovimiento tipo, Lote lote,
                                BigDecimal cantidad, String motivo, String referencia) {
        this.id = id;
        this.fecha = Validar.obligatorio(fecha, "La fecha del movimiento");
        this.tipo = Validar.obligatorio(tipo, "El tipo de movimiento");
        this.lote = Validar.obligatorio(lote, "El lote del movimiento");
        this.cantidad = Validar.obligatorio(cantidad, "La cantidad del movimiento");
        this.motivo = motivo;
        this.referencia = referencia;
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public Lote getLote() {
        return lote;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReferencia() {
        return referencia;
    }
}
