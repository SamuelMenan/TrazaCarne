package co.trazacarne.dominio.estrategia;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Lote;
import co.trazacarne.excepcion.StockInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** First Expired, First Out: primero sale el lote que vence primero (RN-08). */
public class EstrategiaFEFO implements EstrategiaAsignacion {

    @Override
    public List<AsignacionLote> asignar(BigDecimal cantidad, List<Lote> candidatos, LocalDate hoy) {
        List<Lote> ordenados = candidatos.stream()
                .filter(lote -> !lote.estaVencido(hoy))                         // RN-07
                .filter(lote -> lote.getCantidadDisponible().signum() > 0)
                .sorted(Comparator.comparing(Lote::getFechaVencimiento)
                        .thenComparing(Lote::getCodigo))                        // desempate reproducible
                .toList();

        List<AsignacionLote> plan = new ArrayList<>();
        BigDecimal faltante = cantidad;
        for (Lote lote : ordenados) {
            if (faltante.signum() == 0) {
                break;
            }
            BigDecimal tomar = lote.getCantidadDisponible().min(faltante);
            plan.add(new AsignacionLote(lote, tomar));
            faltante = faltante.subtract(tomar);
        }
        if (faltante.signum() > 0) {
            throw new StockInsuficienteException(
                    "Existencias insuficientes: faltan " + faltante.toPlainString() + ".");
        }
        return plan;
    }
}
