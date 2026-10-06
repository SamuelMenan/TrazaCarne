package co.trazacarne.dominio;

import co.trazacarne.dominio.estrategia.EstrategiaFEFO;
import co.trazacarne.excepcion.CantidadInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);
    private Inventario inventario;
    private Producto molida;

    @BeforeEach
    void preparar() {
        inventario = new Inventario(new EstrategiaFEFO());
        molida = new ProductoPorPeso("P-001", "Carne molida", "Bovino", "Molida", new BigDecimal("24000"));
        Compra compra = new Compra("C-1", HOY, new Abastecedor("900", "Frigorífico", TipoAbastecedor.MATADERO));
        compra.agregarLote("L-001", molida, new BigDecimal("5"), HOY.minusDays(3), HOY.minusDays(2), LocalDate.of(2026, 10, 6));
        compra.agregarLote("L-002", molida, new BigDecimal("20"), HOY.minusDays(3), HOY.minusDays(1), LocalDate.of(2026, 10, 15));
        inventario.registrarEntrada(compra);
    }

    @Test
    void hay25KgVendiblesElCincoDeOctubre() {
        assertEquals(0, new BigDecimal("25").compareTo(inventario.disponible(molida, HOY)));
    }

    @Test
    void elLoteVencidoNoSumaAunqueConserveSuCantidad() {
        LocalDate seisDeOctubre = LocalDate.of(2026, 10, 6);

        assertEquals(0, new BigDecimal("20").compareTo(inventario.disponible(molida, seisDeOctubre)));
        assertEquals(0, new BigDecimal("5").compareTo(
                inventario.buscarLote("L-001").orElseThrow().getCantidadDisponible()));
    }

    @Test
    void planificarOchoKilosNoCambiaExistenciasNiMovimientos() {
        List<AsignacionLote> plan = inventario.planificar(molida, new BigDecimal("8"), HOY);

        assertEquals(2, plan.size());
        assertEquals(0, new BigDecimal("25").compareTo(inventario.disponible(molida, HOY)));
        assertEquals(2, inventario.getMovimientos().size());
    }

    @Test
    void planificarValidaLaCantidadSegunElProducto() {
        assertThrows(CantidadInvalidaException.class, () -> inventario.planificar(molida, new BigDecimal("1.2345"), HOY));
    }

    @Test
    void cadaLoteTieneSuMovimientoDeEntrada() {
        Lote lote = inventario.buscarLote("L-002").orElseThrow();

        assertEquals(1, inventario.movimientosDe(lote).size());
        assertEquals(TipoMovimiento.ENTRADA, inventario.movimientosDe(lote).get(0).getTipo());
    }
}
