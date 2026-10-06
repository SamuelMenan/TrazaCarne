package co.trazacarne.dominio;

import co.trazacarne.dominio.estrategia.EstrategiaAsignacion;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Dueño de los lotes y de su historial; única puerta para cambiar existencias. */
public class Inventario {

    private final List<Lote> lotes;
    private final List<MovimientoInventario> movimientos;
    private final EstrategiaAsignacion estrategia;
    private int siguienteIdMovimiento;

    public Inventario(EstrategiaAsignacion estrategia) {
        this.lotes = new ArrayList<>();
        this.movimientos = new ArrayList<>();
        this.estrategia = Validar.obligatorio(estrategia, "La estrategia de asignación");
        this.siguienteIdMovimiento = 1;
    }

    /** Incorpora todos los lotes de la compra o ninguno (RN-01, RN-10). */
    public void registrarEntrada(Compra compra) {
        for (Lote nuevo : compra.getLotes()) {
            if (buscarLote(nuevo.getCodigo()).isPresent()) {
                throw new IdentificadorDuplicadoException("El lote " + nuevo.getCodigo() + " ya existe.");
            }
        }
        for (Lote nuevo : compra.getLotes()) {
            lotes.add(nuevo);
            registrarMovimiento(compra.getFecha().atStartOfDay(), TipoMovimiento.ENTRADA, nuevo,
                    nuevo.getCantidadRecibida(), null, compra.getNumero());
        }
    }

    /** Existencias vendibles: excluye lotes vencidos (RN-07). */
    public BigDecimal disponible(Producto producto, LocalDate hoy) {
        return lotesDe(producto).stream()
                .filter(lote -> !lote.estaVencido(hoy))
                .map(Lote::getCantidadDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Prepara de qué lotes saldría una cantidad, sin cambiar nada (ajuste del plan, etapa 5). */
    public List<AsignacionLote> planificar(Producto producto, BigDecimal cantidad, LocalDate hoy) {
        producto.validarCantidad(cantidad);
        return estrategia.asignar(cantidad, lotesDe(producto), hoy);
    }

    public Optional<Lote> buscarLote(String codigo) {
        return lotes.stream().filter(lote -> lote.getCodigo().equals(codigo)).findFirst();
    }

    public List<Lote> lotesDe(Producto producto) {
        return lotes.stream().filter(lote -> lote.getProducto() == producto).toList();
    }

    public List<MovimientoInventario> movimientosDe(Lote lote) {
        return movimientos.stream().filter(m -> m.getLote() == lote).toList();
    }

    public List<Lote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }

    public List<MovimientoInventario> getMovimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    private void registrarMovimiento(LocalDateTime fecha, TipoMovimiento tipo, Lote lote,
                                     BigDecimal cantidad, String motivo, String referencia) {
        movimientos.add(new MovimientoInventario(siguienteIdMovimiento++, fecha, tipo, lote,
                cantidad, motivo, referencia));
    }
}
