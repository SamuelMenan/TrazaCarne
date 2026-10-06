package co.trazacarne.dominio.estrategia;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.Producto;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** FEFO: primero sale el lote que vence antes; empate por código. */
public class EstrategiaFEFO implements EstrategiaAsignacion {

    /**
     * Reparte la cantidad pedida entre los lotes, empezando por el que vence primero.
     *
     * <p><b>FEFO</b> = First Expired, First Out. En una carnicería es la regla correcta porque
     * reduce la carne que se pierde por vencimiento (a diferencia de FIFO, que usa la fecha de llegada).
     *
     * <p>Ejemplo con una venta de 8 kg:
     * <pre>
     *   Lote   Vence   Disponible   Se toma
     *   L-001  10-oct     5 kg       5 kg   (vence primero, se agota)
     *   L-002  15-oct    20 kg       3 kg   (completa lo que faltaba)
     *   Resultado: [L-001 = 5 kg, L-002 = 3 kg]
     * </pre>
     *
     * <p>En cada vuelta se toma {@code min(pendiente, disponible del lote)}: todo el lote si alcanza,
     * o solo lo que falta. Si se recorren todos los lotes y aún queda pendiente, no hay stock suficiente.
     *
     * <p>Este método <b>no modifica</b> los lotes; solo devuelve el plan. El descuento real
     * lo hace el Inventario al registrar la venta.
     */
    @Override
    public List<AsignacionLote> asignar(Producto producto, BigDecimal cantidad, List<Lote> candidatos, LocalDate hoy) {
        if (producto == null || candidatos == null || hoy == null) {
            throw new ReglaNegocioException("La asignación requiere producto, lotes y fecha.");
        }
        producto.validarCantidad(cantidad);
        // 1. Filtrar: solo lotes del producto que se puedan vender hoy.
        List<Lote> elegibles = new ArrayList<>();
        Set<String> codigos = new HashSet<>();
        for (Lote lote : candidatos) {
            if (lote == null || !codigos.add(lote.getCodigo())) {
                throw new ReglaNegocioException("Los candidatos no pueden ser nulos ni repetir lotes.");
            }
            if (lote.getProducto().getCodigo().equals(producto.getCodigo()) && lote.puedeVenderse(hoy)) {
                elegibles.add(lote);
            }
        }
        // 2. Ordenar: el que vence primero va adelante; si vencen el mismo día, por código (orden predecible).
        elegibles.sort(Comparator.comparing(Lote::getFechaVencimiento).thenComparing(Lote::getCodigo));
        // 3. Tomar de cada lote lo que tenga hasta completar la cantidad.
        List<AsignacionLote> resultado = new ArrayList<>();
        BigDecimal pendiente = cantidad;
        for (Lote lote : elegibles) {
            if (pendiente.signum() == 0) { break; }
            BigDecimal tomada = pendiente.min(lote.getCantidadDisponible());
            resultado.add(new AsignacionLote(lote, tomada));
            pendiente = pendiente.subtract(tomada);
        }
        // Si sobra cantidad sin cubrir, no hay stock suficiente.
        if (pendiente.signum() > 0) {
            throw new StockInsuficienteException("No hay existencias vendibles suficientes de " + producto.getNombre() + ".");
        }
        return List.copyOf(resultado);
    }
}
