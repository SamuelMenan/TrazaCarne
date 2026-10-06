package co.trazacarne.dominio;

import java.math.BigDecimal;

/** Cantidad tomada de un lote; conserva el origen de lo vendido (RN-09). */
public class AsignacionLote {

    private final Lote lote;
    private final BigDecimal cantidad;

    public AsignacionLote(Lote lote, BigDecimal cantidad) {
        this.lote = Validar.obligatorio(lote, "El lote asignado");
        this.cantidad = Validar.obligatorio(cantidad, "La cantidad asignada");
    }

    public Lote getLote() {
        return lote;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }
}
