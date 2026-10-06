package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.dominio.TipoMovimiento;
import co.trazacarne.dominio.estrategia.EstrategiaFEFO;
import co.trazacarne.excepcion.CantidadInvalidaException;
import co.trazacarne.excepcion.FechasInvalidasException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioComprasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);
    private Inventario inventario;
    private Repositorio<Compra, String> repoCompras;
    private ServicioAbastecedores abastecedores;
    private ServicioCompras servicio;

    @BeforeEach
    void preparar() {
        Repositorio<Abastecedor, String> repoAbastecedores = new RepositorioEnMemoria<>(Abastecedor::getNit);
        Repositorio<Producto, String> repoProductos = new RepositorioEnMemoria<>(Producto::getCodigo);
        repoCompras = new RepositorioEnMemoria<>(Compra::getNumero);
        inventario = new Inventario(new EstrategiaFEFO());
        servicio = new ServicioCompras(repoCompras, repoAbastecedores, repoProductos, inventario);

        abastecedores = new ServicioAbastecedores(repoAbastecedores);
        abastecedores.registrar("900", "Frigorífico", TipoAbastecedor.MATADERO);
        new ServicioProductos(repoProductos).registrar("P-001", "Carne molida", "Bovino", "Molida", "PESO",
                new BigDecimal("24000"));
    }

    private static LoteRecibido lote(String codigo, String cantidad, LocalDate sacrificio) {
        return new LoteRecibido(codigo, "P-001", new BigDecimal(cantidad), sacrificio, HOY.minusDays(1), HOY.plusDays(5));
    }

    @Test
    void compraValidaDejaLotesYMovimientosDeEntrada() {
        servicio.registrarCompra("C-1", "900", HOY,
                List.of(lote("L-001", "5", HOY.minusDays(2)), lote("L-002", "20", HOY.minusDays(2))));

        assertEquals(2, inventario.getLotes().size());
        assertEquals(2, inventario.getMovimientos().size());
        assertEquals(TipoMovimiento.ENTRADA, inventario.getMovimientos().get(0).getTipo());
        assertEquals("C-1", inventario.getMovimientos().get(0).getReferencia());
        assertTrue(repoCompras.existe("C-1"));
    }

    @Test
    void compraVaciaSeRechaza() {
        assertThrows(CantidadInvalidaException.class, () -> servicio.registrarCompra("C-1", "900", HOY, List.of()));
    }

    @Test
    void unLoteInvalidoRechazaTodaLaCompraSinCambios() {
        assertThrows(FechasInvalidasException.class, () -> servicio.registrarCompra("C-1", "900", HOY,
                List.of(lote("L-001", "5", HOY.minusDays(2)), lote("L-002", "20", null))));

        assertTrue(inventario.getLotes().isEmpty());
        assertTrue(inventario.getMovimientos().isEmpty());
        assertFalse(repoCompras.existe("C-1"));
    }

    @Test
    void codigoDeLoteYaRegistradoEnOtraCompraSeRechaza() {
        servicio.registrarCompra("C-1", "900", HOY, List.of(lote("L-001", "5", HOY.minusDays(2))));

        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrarCompra("C-2", "900", HOY,
                List.of(lote("L-002", "4", HOY.minusDays(2)), lote("L-001", "3", HOY.minusDays(2)))));
        assertEquals(1, inventario.getLotes().size());
        assertFalse(repoCompras.existe("C-2"));
    }

    @Test
    void segundoRegistroDeLaMismaCompraSeRechaza() {
        servicio.registrarCompra("C-1", "900", HOY, List.of(lote("L-001", "5", HOY.minusDays(2))));

        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrarCompra("C-1", "900", HOY,
                List.of(lote("L-009", "5", HOY.minusDays(2)))));
        assertEquals(1, inventario.getLotes().size());
    }

    @Test
    void abastecedorInactivoNoPuedeVender() {
        abastecedores.desactivar("900");

        assertThrows(RegistroInactivoException.class, () -> servicio.registrarCompra("C-1", "900", HOY,
                List.of(lote("L-001", "5", HOY.minusDays(2)))));
    }
}
