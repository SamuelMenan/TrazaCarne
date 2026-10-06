package co.trazacarne.dominio.estrategia;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Política para decidir de qué lotes sale una cantidad.
 * Ajuste del plan (etapa 5): devuelve un plan de asignaciones y NO descuenta existencias.
 * Lanza StockInsuficienteException si los candidatos no alcanzan.
 */
public interface EstrategiaAsignacion {

    List<AsignacionLote> asignar(BigDecimal cantidad, List<Lote> candidatos, LocalDate hoy);
}
