package co.trazacarne.flujo;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.TipoMovimiento;
import co.trazacarne.dominio.estrategia.EstrategiaFEFO;
import co.trazacarne.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.trazacarne.flujo.Escenario.*;
import static org.junit.jupiter.api.Assertions.*;

class InventarioFefoTest {
    private final Escenario e = new Escenario();

    @Test
    void fefoOrdenaPorVencimientoYCodigoSinDescontarTodavia() {
        e.comprar("C-001", e.lote("L-Z", "P-001", "20", 10),
                e.lote("L-B", "P-001", "5", 2), e.lote("L-A", "P-001", "3", 2));

        List<AsignacionLote> plan = e.inventario.planificar(e.carne, decimal("10"), HOY);

        assertEquals(List.of("L-A", "L-B", "L-Z"),
                plan.stream().map(asignacion -> asignacion.getLote().getCodigo()).toList());
        cantidad("3", plan.get(0).getCantidad());
        cantidad("5", plan.get(1).getCantidad());
        cantidad("2", plan.get(2).getCantidad());
        cantidad("28", e.consultas.disponible("P-001"));
        assertEquals(3, e.inventario.getMovimientos().size());
    }

    @Test
    void estrategiaEsPuraNoReordenaLaListaNiModificaElInventario() {
        e.comprar("C-001", e.lote("L-TARDE", "P-001", "20", 5),
                e.lote("L-PRONTO", "P-001", "5", 2));
        List<Lote> recibidos = e.inventario.getLotes();
        List<String> ordenOriginal = recibidos.stream().map(Lote::getCodigo).toList();

        List<AsignacionLote> plan = new EstrategiaFEFO().asignar(e.carne, decimal("8"), recibidos, HOY);

        assertEquals("L-PRONTO", plan.getFirst().getLote().getCodigo());
        assertEquals(ordenOriginal, recibidos.stream().map(Lote::getCodigo).toList());
        cantidad("5", e.inventario.consultarLote("L-PRONTO").getCantidadDisponible());
        cantidad("20", e.inventario.consultarLote("L-TARDE").getCantidadDisponible());
        assertEquals(2, e.inventario.getMovimientos().size());
    }

    @Test
    void stockInsuficienteNoProduceAsignacionParcialNiMovimientos() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));

        assertThrows(ReglaNegocioException.class,
                () -> e.inventario.planificar(e.carne, decimal("6"), HOY));

        cantidad("5", e.consultas.disponible("P-001"));
        assertEquals(1, e.inventario.getMovimientos().size());
    }

    @Test
    void alLlegarElVencimientoConservaExistenciasPeroExcluyeCantidadVendible() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 1),
                e.lote("L-002", "P-001", "20", 5));
        var manana = e.servicioInventario(reloj(HOY.plusDays(1)));

        cantidad("25", saldo(manana.existencias("P-001")));
        cantidad("20", manana.disponible("P-001"));
        List<AsignacionLote> plan = e.inventario.planificar(e.carne, decimal("8"), HOY.plusDays(1));
        assertEquals(List.of("L-002"), plan.stream().map(a -> a.getLote().getCodigo()).toList());
    }

    @Test
    void proximosAVencerExcluyeVencidosYAgotadosYRespetaElLimiteDeDias() {
        e.comprar("C-001", e.lote("L-VENCIDO", "P-001", "5", 1),
                e.lote("L-AGOTADO", "P-001", "3", 2),
                e.lote("L-PRONTO", "P-001", "7", 3),
                e.lote("L-TARDE", "P-001", "20", 5));
        e.consultas.registrarPerdida("L-AGOTADO", decimal("3"), "Daño");

        List<Lote> proximos = e.servicioInventario(reloj(HOY.plusDays(1))).proximosAVencer(2);

        assertEquals(List.of("L-PRONTO"), proximos.stream().map(Lote::getCodigo).toList());
    }

    @Test
    void perdidaReduceElLoteYDejaUnMovimientoConTipoPerdida() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));

        e.consultas.registrarPerdida("L-001", decimal("1.250"), "Daño en refrigeración");

        cantidad("3.750", saldo(e.consultas.existencias("P-001")));
        assertEquals(2, e.consultas.movimientos("L-001").size());
        assertEquals(TipoMovimiento.PERDIDA, e.consultas.movimientos("L-001").getLast().getTipo());
    }

    @Test
    void perdidaMayorAlDisponibleOMotivoVacioNoCambiaSaldoNiHistorial() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));

        assertThrows(ReglaNegocioException.class,
                () -> e.consultas.registrarPerdida("L-001", decimal("6"), "Daño"));
        assertThrows(ReglaNegocioException.class,
                () -> e.consultas.registrarPerdida("L-001", decimal("1"), " "));

        cantidad("5", saldo(e.consultas.existencias("P-001")));
        assertEquals(1, e.consultas.movimientos("L-001").size());
    }

    @Test
    void movimientoAnteriorALaRecepcionSeRechazaSinCambios() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));

        assertThrows(ReglaNegocioException.class,
                () -> e.servicioInventario(reloj(HOY.minusDays(1)))
                        .registrarPerdida("L-001", decimal("1"), "Daño"));

        cantidad("5", saldo(e.consultas.existencias("P-001")));
        assertEquals(1, e.inventario.getMovimientos().size());
    }

    @Test
    void listasPublicasNoPermitenBorrarLotesNiMovimientosDelInventario() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 2));

        assertThrows(UnsupportedOperationException.class, () -> e.inventario.getLotes().clear());
        assertThrows(UnsupportedOperationException.class, () -> e.inventario.getMovimientos().clear());

        cantidad("5", e.consultas.disponible("P-001"));
        assertEquals(1, e.inventario.getMovimientos().size());
    }
}
