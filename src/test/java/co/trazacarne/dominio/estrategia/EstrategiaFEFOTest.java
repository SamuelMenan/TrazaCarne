package co.trazacarne.dominio.estrategia;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.StockInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstrategiaFEFOTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);
    private final EstrategiaAsignacion fefo = new EstrategiaFEFO();
    private Lote lote001;
    private Lote lote002;

    @BeforeEach
    void preparar() {
        Producto molida = new ProductoPorPeso("P-001", "Carne molida", "Bovino", "Molida", new BigDecimal("24000"));
        Compra compra = new Compra("C-1", HOY, new Abastecedor("900", "Frigorífico", TipoAbastecedor.PROVEEDOR));
        // Se agregan al revés a propósito: FEFO ordena por vencimiento, no por orden de llegada.
        lote002 = compra.agregarLote("L-002", molida, new BigDecimal("20"), null, HOY.minusDays(1), LocalDate.of(2026, 10, 15));
        lote001 = compra.agregarLote("L-001", molida, new BigDecimal("5"), null, HOY.minusDays(2), LocalDate.of(2026, 10, 6));
    }

    @Test
    void ochoKilosSalenCincoDelQueVencePrimeroYTresDelSiguiente() {
        List<AsignacionLote> plan = fefo.asignar(new BigDecimal("8"), List.of(lote002, lote001), HOY);

        assertEquals("L-001", plan.get(0).getLote().getCodigo());
        assertEquals(0, new BigDecimal("5").compareTo(plan.get(0).getCantidad()));
        assertEquals("L-002", plan.get(1).getLote().getCodigo());
        assertEquals(0, new BigDecimal("3").compareTo(plan.get(1).getCantidad()));
    }

    @Test
    void planificarNoCambiaLasCantidades() {
        fefo.asignar(new BigDecimal("8"), List.of(lote002, lote001), HOY);

        assertEquals(0, new BigDecimal("5").compareTo(lote001.getCantidadDisponible()));
        assertEquals(0, new BigDecimal("20").compareTo(lote002.getCantidadDisponible()));
    }

    @Test
    void losLotesVencidosNoParticipan() {
        List<AsignacionLote> plan = fefo.asignar(new BigDecimal("8"), List.of(lote002, lote001),
                LocalDate.of(2026, 10, 6));

        assertEquals(1, plan.size());
        assertEquals("L-002", plan.get(0).getLote().getCodigo());
    }

    @Test
    void vencimientosIgualesSeDesempatanPorCodigo() {
        Producto res = new ProductoPorPeso("P-009", "Costilla", "Bovino", "Costilla", new BigDecimal("20000"));
        Compra compra = new Compra("C-9", HOY, new Abastecedor("901", "Proveedor", TipoAbastecedor.PROVEEDOR));
        Lote b = compra.agregarLote("B", res, new BigDecimal("2"), null, HOY.minusDays(1), HOY.plusDays(3));
        Lote a = compra.agregarLote("A", res, new BigDecimal("2"), null, HOY.minusDays(1), HOY.plusDays(3));

        List<AsignacionLote> plan = fefo.asignar(new BigDecimal("1"), List.of(b, a), HOY);

        assertEquals("A", plan.get(0).getLote().getCodigo());
    }

    @Test
    void stockInsuficienteSeRechaza() {
        assertThrows(StockInsuficienteException.class,
                () -> fefo.asignar(new BigDecimal("26"), List.of(lote002, lote001), HOY));
    }
}
