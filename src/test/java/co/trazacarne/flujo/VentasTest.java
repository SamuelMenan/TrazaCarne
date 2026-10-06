package co.trazacarne.flujo;

import co.trazacarne.dominio.*;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.servicio.dto.DatosVenta;
import co.trazacarne.servicio.dto.ItemVenta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static co.trazacarne.flujo.Escenario.*;
import static org.junit.jupiter.api.Assertions.*;

class VentasTest {
    private final Escenario e = new Escenario();

    @Test
    void ventaDeOchoKilosDescuentaCincoYTresDeSusLotesOriginales() {
        comprarDosLotes();

        Venta venta = e.vender("V-001", e.item("P-001", "8"));

        assertEquals(EstadoVenta.REGISTRADA, venta.getEstado());
        cantidad("0", e.inventario.consultarLote("L-001").getCantidadDisponible());
        cantidad("17", e.inventario.consultarLote("L-002").getCantidadDisponible());
        List<AsignacionLote> asignaciones = venta.getDetalles().getFirst().getAsignaciones();
        cantidad("5", asignaciones.get(0).getCantidad());
        cantidad("3", asignaciones.get(1).getCantidad());
        cantidad("192000", venta.calcularTotal());
        assertEquals(2, e.inventario.getMovimientos().stream()
                .filter(m -> m.getTipo() == TipoMovimiento.SALIDA).count());
    }

    @Test
    void faltaDeStockEnSegundoProductoNoDescuentaElPrimero() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2),
                e.lote("L-002", "P-002", "2", 5));

        assertThrows(ReglaNegocioException.class,
                () -> e.vender("V-001", e.item("P-001", "3"), e.item("P-002", "3")));

        assertAll(
                () -> cantidad("5", e.consultas.disponible("P-001")),
                () -> cantidad("2", e.consultas.disponible("P-002")),
                () -> assertEquals(2, e.inventario.getMovimientos().size()),
                () -> assertTrue(e.ventas.listar().isEmpty())
        );
    }

    @Test
    void dosLineasDeSeisKilosSeCompruebanComoDoceContraDiezDisponibles() {
        e.comprar("C-001", e.lote("L-001", "P-001", "10", 2));

        assertThrows(ReglaNegocioException.class,
                () -> e.vender("V-001", e.item("P-001", "6"), e.item(" P-001 ", "6")));

        assertRechazoConSaldo("10", 1);
    }

    @Test
    void lineasRepetidasValidasSeUnificanSinDuplicarLaAsignacion() {
        e.comprar("C-001", e.lote("L-001", "P-001", "10", 2));

        Venta venta = e.vender("V-001", e.item("P-001", "1.250"), e.item("P-001", "2.250"));

        assertEquals(1, venta.getDetalles().size());
        cantidad("3.500", venta.getDetalles().getFirst().getCantidad());
        cantidad("6.500", e.consultas.disponible("P-001"));
        assertEquals(1, venta.getDetalles().getFirst().getAsignaciones().size());
        cantidad("84000", venta.calcularTotal());
    }

    @Test
    void cantidadNegativaNoPuedeCompensarseConOtraLineaPositiva() {
        e.comprar("C-001", e.lote("L-001", "P-001", "10", 2));

        assertThrows(ReglaNegocioException.class,
                () -> e.vender("V-001", e.item("P-001", "-2"), e.item("P-001", "5")));

        assertRechazoConSaldo("10", 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clienteOProductoInactivoImpideUnaVentaNueva(boolean desactivarProducto) {
        e.comprar("C-001", e.lote("L-001", "P-001", "10", 2));
        if (desactivarProducto) {
            e.productos.desactivar("P-001");
        } else {
            e.clientes.desactivar("1001");
        }

        assertThrows(RegistroInactivoException.class,
                () -> e.vender("V-001", e.item("P-001", "3")));

        assertRechazoConSaldo("10", 1);
    }

    @Test
    void ventaConservaElPrecioHistoricoAunqueCambieElProducto() {
        comprarDosLotes();
        Venta venta = e.vender("V-001", e.item("P-001", "8"));

        e.productos.actualizarPrecio("P-001", decimal("30000"));

        cantidad("24000", venta.getDetalles().getFirst().getPrecioUnitario());
        cantidad("192000", venta.calcularTotal());
        cantidad("240000", e.carne.calcularSubtotal(decimal("8")));
        e.ventas.anularVenta("V-001");
        cantidad("192000", venta.calcularTotal());
    }

    @Test
    void anulacionReponeCadaLoteYDejaMovimientosDeDevolucion() {
        comprarDosLotes();
        Venta venta = e.vender("V-001", e.item("P-001", "8"));

        e.ventas.anularVenta("V-001");

        assertEquals(EstadoVenta.ANULADA, venta.getEstado());
        cantidad("5", e.inventario.consultarLote("L-001").getCantidadDisponible());
        cantidad("20", e.inventario.consultarLote("L-002").getCantidadDisponible());
        assertEquals(2, e.inventario.getMovimientos().stream()
                .filter(m -> m.getTipo() == TipoMovimiento.DEVOLUCION).count());
        assertSame(venta, e.ventas.consultar("V-001"));
    }

    @Test
    void segundaAnulacionNoDuplicaCantidadesNiMovimientos() {
        comprarDosLotes();
        e.vender("V-001", e.item("P-001", "8"));
        e.ventas.anularVenta("V-001");
        int movimientos = e.inventario.getMovimientos().size();

        assertThrows(ReglaNegocioException.class, () -> e.ventas.anularVenta("V-001"));

        cantidad("25", e.consultas.disponible("P-001"));
        assertEquals(movimientos, e.inventario.getMovimientos().size());
    }

    @Test
    void anulacionTardiaDevuelveLotesVencidosAunqueClienteYProductoEstenInactivos() {
        comprarDosLotes();
        e.vender("V-001", e.item("P-001", "8"));
        e.clientes.desactivar("1001");
        e.productos.desactivar("P-001");
        var relojPosterior = reloj(HOY.plusDays(10));

        Venta anulada = e.servicioVentas(relojPosterior).anularVenta("V-001");

        assertEquals(EstadoVenta.ANULADA, anulada.getEstado());
        cantidad("5", e.inventario.consultarLote("L-001").getCantidadDisponible());
        cantidad("20", e.inventario.consultarLote("L-002").getCantidadDisponible());
        cantidad("25", saldo(e.servicioInventario(relojPosterior).existencias("P-001")));
        cantidad("0", e.servicioInventario(relojPosterior).disponible("P-001"));
    }

    @Test
    void numeroDeVentaDuplicadoNoDescuentaStockUnaSegundaVez() {
        comprarDosLotes();
        Venta original = e.vender("V-001", e.item("P-001", "8"));
        int movimientos = e.inventario.getMovimientos().size();

        assertThrows(IdentificadorDuplicadoException.class,
                () -> e.vender(" V-001 ", e.item("P-001", "2")));

        cantidad("17", e.consultas.disponible("P-001"));
        assertEquals(movimientos, e.inventario.getMovimientos().size());
        assertEquals(List.of(original), e.ventas.listar());
    }

    @Test
    void planAnteriorSeRevalidaSiOtraVentaYaConsumioLaCantidad() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));
        List<AsignacionLote> plan = e.inventario.planificar(e.carne, decimal("3"), HOY);
        DetalleVenta detalle = new DetalleVenta(e.carne, decimal("3"), plan);
        Venta pendiente = new Venta("V-PENDIENTE", HOY, e.clientes.consultar("1001"), List.of(detalle));
        e.vender("V-001", e.item("P-001", "4"));
        int movimientos = e.inventario.getMovimientos().size();

        assertThrows(ReglaNegocioException.class,
                () -> pendiente.registrar(e.inventario, LocalDateTime.of(HOY, LocalTime.of(10, 0))));

        assertEquals(EstadoVenta.BORRADOR, pendiente.getEstado());
        cantidad("1", e.consultas.disponible("P-001"));
        assertEquals(movimientos, e.inventario.getMovimientos().size());
    }

    @Test
    void datosVentaYDetalleProtegenSusListasContraCambiosExternos() {
        comprarDosLotes();
        List<ItemVenta> origen = new ArrayList<>();
        origen.add(e.item("P-001", "8"));
        DatosVenta datos = new DatosVenta("V-001", "1001", origen);
        origen.clear();

        Venta venta = e.ventas.registrarVenta(datos);

        cantidad("8", venta.getDetalles().getFirst().getCantidad());
        assertThrows(UnsupportedOperationException.class, () -> datos.items().clear());
        assertThrows(UnsupportedOperationException.class, () -> venta.getDetalles().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> venta.getDetalles().getFirst().getAsignaciones().clear());
    }

    private void comprarDosLotes() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2),
                e.lote("L-002", "P-001", "20", 5));
    }

    private void assertRechazoConSaldo(String saldo, int movimientos) {
        cantidad(saldo, e.inventario.consultarLote("L-001").getCantidadDisponible());
        assertEquals(movimientos, e.inventario.getMovimientos().size());
        assertTrue(e.ventas.listar().isEmpty());
    }
}
