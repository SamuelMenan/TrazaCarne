package co.trazacarne.servicio;

import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.MovimientoInventario;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Consultas históricas permiten productos inactivos; pérdidas requieren lote y motivo. */
public class ServicioInventario {
    private final Inventario inventario;
    private final ServicioProductos productos;
    private final Clock reloj;

    public ServicioInventario(Inventario inventario, ServicioProductos productos, Clock reloj) {
        this.inventario = Objects.requireNonNull(inventario);
        this.productos = Objects.requireNonNull(productos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    // Todos los lotes del producto, incluso vencidos o agotados.
    public List<Lote> existencias(String codigoProducto) {
        return inventario.lotesDe(productos.consultar(codigoProducto));
    }

    // Cantidad que se puede vender hoy.
    public BigDecimal disponible(String codigoProducto) {
        return inventario.disponible(productos.consultar(codigoProducto), LocalDate.now(reloj));
    }

    public List<Lote> lotes() { return inventario.getLotes(); }
    public Lote lote(String codigo) { return inventario.consultarLote(codigo); }

    public List<Lote> proximosAVencer(int dias) {
        return inventario.proximosAVencer(LocalDate.now(reloj), dias);
    }

    public void registrarPerdida(String codigoLote, BigDecimal cantidad, String motivo) {
        inventario.registrarPerdida(lote(codigoLote), cantidad, motivo, LocalDateTime.now(reloj));
    }

    // Historial de un lote: entradas, salidas, devoluciones y pérdidas.
    public List<MovimientoInventario> movimientos(String codigoLote) {
        Lote lote = lote(codigoLote);
        List<MovimientoInventario> resultado = new ArrayList<>();
        for (MovimientoInventario movimiento : inventario.getMovimientos()) {
            if (movimiento.getLote() == lote) { resultado.add(movimiento); }
        }
        return List.copyOf(resultado);
    }
}
