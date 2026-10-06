package co.trazacarne.dominio.estrategia;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.Producto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Patrón Strategy: permite cambiar la regla para elegir lotes sin tocar el Inventario. */
public interface EstrategiaAsignacion {
    /** Prepara una asignación completa sin modificar existencias. */
    List<AsignacionLote> asignar(Producto producto, BigDecimal cantidad, List<Lote> candidatos, LocalDate hoy);
}
