package co.trazacarne.flujo;

import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.TipoMovimiento;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.servicio.dto.DatosCompra;
import co.trazacarne.servicio.dto.DatosLoteRecibido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static co.trazacarne.flujo.Escenario.*;
import static org.junit.jupiter.api.Assertions.*;

class ComprasTest {
    private final Escenario e = new Escenario();

    @Test
    void recibirCompraCreaLotesConOrigenYMovimientosDeEntrada() {
        Compra compra = e.comprar("C-001", e.lote("L-001", "P-001", "5", 2),
                e.lote("L-002", "P-002", "12", 5));

        assertAll(
                () -> assertSame(compra, e.compras.consultar("C-001")),
                () -> assertSame(compra, e.inventario.consultarLote("L-001").getCompra()),
                () -> cantidad("5", e.consultas.disponible("P-001")),
                () -> cantidad("12", e.consultas.disponible("P-002")),
                () -> assertEquals(2, e.inventario.getMovimientos().size()),
                () -> assertTrue(e.inventario.getMovimientos().stream()
                        .allMatch(movimiento -> movimiento.getTipo() == TipoMovimiento.ENTRADA))
        );
    }

    @Test
    void mataderoExigeSacrificioYConservaEsaFechaComoOrigen() {
        DatosLoteRecibido sinSacrificio = e.lote("L-001", "P-001", "5", 2);

        assertThrows(ReglaNegocioException.class,
                () -> e.compras.registrarCompra(new DatosCompra("C-001", "9002", List.of(sinSacrificio))));
        assertSinCompraNiInventario();

        DatosLoteRecibido conSacrificio = new DatosLoteRecibido("L-001", "P-001", decimal("5"),
                HOY.minusDays(2), HOY.minusDays(1), HOY.plusDays(2));
        e.compras.registrarCompra(new DatosCompra("C-001", "9002", List.of(conSacrificio)));

        assertEquals(HOY.minusDays(2), e.inventario.consultarLote("L-001").getFechaSacrificio());
        cantidad("5", e.consultas.disponible("P-001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sacrificioPosterior", "procesamientoFuturo", "venceHoy", "vencido"})
    void fechaIncoherenteRechazaTodaLaCompra(String caso) {
        LocalDate sacrificio = null;
        LocalDate procesamiento = HOY.minusDays(1);
        LocalDate vencimiento = HOY.plusDays(2);
        switch (caso) {
            case "sacrificioPosterior" -> sacrificio = HOY;
            case "procesamientoFuturo" -> procesamiento = HOY.plusDays(1);
            case "venceHoy" -> vencimiento = HOY;
            case "vencido" -> vencimiento = HOY.minusDays(1);
            default -> fail("Caso de prueba desconocido.");
        }
        DatosLoteRecibido invalido = new DatosLoteRecibido("L-002", "P-001", decimal("5"),
                sacrificio, procesamiento, vencimiento);

        assertThrows(ReglaNegocioException.class,
                () -> e.comprar("C-001", e.lote("L-001", "P-001", "5", 3), invalido));

        assertSinCompraNiInventario();
    }

    @Test
    void loteRepetidoDentroDeCompraNoDejaEntradaParcial() {
        assertThrows(IdentificadorDuplicadoException.class,
                () -> e.comprar("C-001", e.lote(" L-001 ", "P-001", "5", 3),
                        e.lote("L-001", "P-002", "2", 4)));

        assertSinCompraNiInventario();
    }

    @Test
    void loteRegistradoEnOtraCompraNoModificaElOriginalNiInsertaElNuevo() {
        Compra original = e.comprar("C-001", e.lote("L-001", "P-001", "5", 3));

        assertThrows(IdentificadorDuplicadoException.class,
                () -> e.comprar("C-002", e.lote("L-002", "P-001", "9", 4),
                        e.lote("L-001", "P-001", "7", 5)));

        assertEquals(List.of(original), e.compras.listar());
        assertEquals(1, e.inventario.getLotes().size());
        assertEquals(1, e.inventario.getMovimientos().size());
        cantidad("5", e.consultas.disponible("P-001"));
    }

    @Test
    void abastecedorInactivoNoPuedeOriginarUnaCompraNueva() {
        e.abastecedores.desactivar("9001");

        assertThrows(RegistroInactivoException.class,
                () -> e.comprar("C-001", e.lote("L-001", "P-001", "5", 3)));

        assertSinCompraNiInventario();
    }

    @Test
    void productoInactivoEnSegundoLoteRechazaTambienElPrimero() {
        e.productos.desactivar("P-002");

        assertThrows(RegistroInactivoException.class,
                () -> e.comprar("C-001", e.lote("L-001", "P-001", "5", 3),
                        e.lote("L-002", "P-002", "2", 3)));

        assertSinCompraNiInventario();
    }

    @Test
    void recepcionRespetaLaCantidadEnteraDelProductoPorUnidad() {
        assertThrows(ReglaNegocioException.class,
                () -> e.comprar("C-001", e.lote("L-001", "P-002", "2.5", 3)));

        assertSinCompraNiInventario();
    }

    @Test
    void numeroDeCompraDuplicadoNoInsertaOtrosLotes() {
        e.comprar("C-001", e.lote("L-001", "P-001", "5", 3));

        assertThrows(IdentificadorDuplicadoException.class,
                () -> e.comprar(" C-001 ", e.lote("L-002", "P-001", "9", 4)));

        assertEquals(1, e.compras.listar().size());
        assertEquals(1, e.inventario.getLotes().size());
        assertEquals(1, e.inventario.getMovimientos().size());
        cantidad("5", e.consultas.disponible("P-001"));
    }

    @Test
    void registrarLaMismaCompraDosVecesNoDuplicaLaRecepcion() {
        Compra compra = e.comprar("C-001", e.lote("L-001", "P-001", "5", 3));

        assertThrows(ReglaNegocioException.class,
                () -> compra.registrar(e.inventario, LocalDateTime.of(HOY, java.time.LocalTime.of(11, 0))));

        cantidad("5", e.consultas.disponible("P-001"));
        assertEquals(1, e.inventario.getMovimientos().size());
    }

    @Test
    void datosCompraCopianLaListaRecibida() {
        List<DatosLoteRecibido> origen = new ArrayList<>();
        origen.add(e.lote("L-001", "P-001", "5", 3));
        DatosCompra datos = new DatosCompra("C-001", "9001", origen);
        origen.clear();

        e.compras.registrarCompra(datos);

        cantidad("5", e.consultas.disponible("P-001"));
        assertThrows(UnsupportedOperationException.class, () -> datos.lotes().clear());
    }

    private void assertSinCompraNiInventario() {
        assertAll(
                () -> assertTrue(e.compras.listar().isEmpty()),
                () -> assertTrue(e.inventario.getLotes().isEmpty()),
                () -> assertTrue(e.inventario.getMovimientos().isEmpty())
        );
    }
}
