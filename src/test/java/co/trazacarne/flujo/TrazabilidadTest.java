package co.trazacarne.flujo;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.EstadoVenta;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.servicio.dto.DatosVenta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.trazacarne.flujo.Escenario.*;
import static org.junit.jupiter.api.Assertions.*;

class TrazabilidadTest {
    private final Escenario e = new Escenario();

    @Test
    void origenReconstruyeProveedorProductoLotesYCantidadesDeUnaVenta() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2),
                e.lote("L-002", "P-001", "20", 5));
        e.vender("V-001", e.item("P-001", "8"));

        var origen = e.trazabilidad.origenDeVenta("V-001");

        assertEquals(List.of("L-001", "L-002"), origen.stream().map(o -> o.codigoLote()).toList());
        cantidad("5", origen.get(0).cantidad());
        cantidad("3", origen.get(1).cantidad());
        assertTrue(origen.stream().allMatch(o -> o.nitAbastecedor().equals("9001")
                && o.codigoProducto().equals("P-001") && o.estado() == EstadoVenta.REGISTRADA));
        assertEquals(HOY.plusDays(2), origen.getFirst().fechaVencimiento());
    }

    @Test
    void destinoIncluyeAnuladasCuandoSePideHistorialYLasExcluyeDelFiltroVigente() {
        e.comprar("C-001", e.lote("L-001", "P-001", "10", 3));
        e.vender("V-001", e.item("P-001", "3"));
        e.vender("V-002", e.item("P-001", "2"));
        e.ventas.anularVenta("V-001");

        var historial = e.trazabilidad.ventasDelLote("L-001", true);
        var vigentes = e.trazabilidad.ventasDelLote("L-001", false);

        assertEquals(2, historial.size());
        assertEquals(EstadoVenta.ANULADA,
                historial.stream().filter(d -> d.numeroVenta().equals("V-001")).findFirst().orElseThrow().estado());
        assertEquals(List.of("V-002"), vigentes.stream().map(d -> d.numeroVenta()).toList());
        cantidad("2", vigentes.getFirst().cantidad());
        assertEquals(EstadoVenta.ANULADA, e.trazabilidad.origenDeVenta("V-001").getFirst().estado());
    }

    @Test
    void clientesDelLoteConservaClientesHistoricosSinDuplicarlosPorVenta() {
        e.comprar("C-001", e.lote("L-001", "P-001", "20", 3));
        e.vender("V-001", e.item("P-001", "3"));
        e.vender("V-002", e.item("P-001", "2"));
        e.clientes.registrar("1002", "Luis");
        e.ventas.registrarVenta(new DatosVenta("V-003", "1002", List.of(e.item("P-001", "1"))));
        e.ventas.anularVenta("V-001");
        e.ventas.anularVenta("V-002");
        e.clientes.desactivar("1001");

        List<Cliente> clientes = e.trazabilidad.clientesDelLote("L-001");

        assertEquals(List.of("1001", "1002"), clientes.stream().map(Cliente::getDocumento).toList());
        assertFalse(clientes.getFirst().estaActivo());
    }

    @Test
    void consultarOrigenODestinoInexistenteNoInventaResultados() {
        assertThrows(RegistroNoEncontradoException.class, () -> e.trazabilidad.origenDeVenta("V-X"));
        assertThrows(RegistroNoEncontradoException.class, () -> e.trazabilidad.ventasDelLote("L-X", true));
        assertThrows(RegistroNoEncontradoException.class, () -> e.trazabilidad.clientesDelLote("L-X"));

        assertTrue(e.ventas.listar().isEmpty());
        assertTrue(e.inventario.getLotes().isEmpty());
    }
}
