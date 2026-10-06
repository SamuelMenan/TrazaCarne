package co.trazacarne.dominio;

import co.trazacarne.excepcion.FechasInvalidasException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.StockInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);

    private static Producto molida() {
        return new ProductoPorPeso("P-001", "Carne molida", "Bovino", "Molida", new BigDecimal("24000"));
    }

    private static Compra compraDe(TipoAbastecedor tipo) {
        return new Compra("C-1", HOY, new Abastecedor("900", "Frigorífico", tipo));
    }

    @Test
    void loteValidoConservaOrigenYCantidad() {
        Compra compra = compraDe(TipoAbastecedor.MATADERO);
        Lote lote = compra.agregarLote("L-001", molida(), new BigDecimal("5"),
                HOY.minusDays(3), HOY.minusDays(2), HOY.plusDays(1));

        assertEquals("Frigorífico", lote.getAbastecedor().getNombre());
        assertEquals(0, new BigDecimal("5").compareTo(lote.getCantidadDisponible()));
    }

    @Test
    void mataderoSinFechaDeSacrificioSeRechaza() {
        Compra compra = compraDe(TipoAbastecedor.MATADERO);

        assertThrows(FechasInvalidasException.class, () -> compra.agregarLote("L-001", molida(),
                new BigDecimal("5"), null, HOY.minusDays(2), HOY.plusDays(1)));
        assertTrue(compra.getLotes().isEmpty());
    }

    @Test
    void procesamientoDespuesDelVencimientoSeRechaza() {
        Compra compra = compraDe(TipoAbastecedor.PROVEEDOR);

        assertThrows(FechasInvalidasException.class, () -> compra.agregarLote("L-001", molida(),
                new BigDecimal("5"), null, HOY.plusDays(2), HOY.plusDays(1)));
    }

    @Test
    void sacrificioDespuesDelProcesamientoSeRechaza() {
        Compra compra = compraDe(TipoAbastecedor.MATADERO);

        assertThrows(FechasInvalidasException.class, () -> compra.agregarLote("L-001", molida(),
                new BigDecimal("5"), HOY.minusDays(1), HOY.minusDays(2), HOY.plusDays(1)));
    }

    @Test
    void codigoRepetidoEnLaMismaCompraSeRechaza() {
        Compra compra = compraDe(TipoAbastecedor.PROVEEDOR);
        compra.agregarLote("L-001", molida(), new BigDecimal("5"), null, HOY.minusDays(2), HOY.plusDays(1));

        assertThrows(IdentificadorDuplicadoException.class, () -> compra.agregarLote("L-001", molida(),
                new BigDecimal("3"), null, HOY.minusDays(2), HOY.plusDays(1)));
        assertEquals(1, compra.getLotes().size());
    }

    @Test
    void descontarMasDeLoDisponibleSeRechazaYConservaLaCantidad() {
        Lote lote = compraDe(TipoAbastecedor.PROVEEDOR).agregarLote("L-001", molida(),
                new BigDecimal("5"), null, HOY.minusDays(2), HOY.plusDays(1));

        assertThrows(StockInsuficienteException.class, () -> lote.descontar(new BigDecimal("6")));
        assertEquals(0, new BigDecimal("5").compareTo(lote.getCantidadDisponible()));
    }

    @Test
    void estaVencidoDesdeSuFechaDeVencimiento() {
        Lote lote = compraDe(TipoAbastecedor.PROVEEDOR).agregarLote("L-001", molida(),
                new BigDecimal("5"), null, HOY.minusDays(2), LocalDate.of(2026, 10, 6));

        assertFalse(lote.estaVencido(LocalDate.of(2026, 10, 5)));
        assertTrue(lote.estaVencido(LocalDate.of(2026, 10, 6)));
    }
}
