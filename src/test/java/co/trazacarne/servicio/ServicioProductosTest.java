package co.trazacarne.servicio;

import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.Producto;
import co.trazacarne.excepcion.CantidadInvalidaException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioProductosTest {

    private final Repositorio<Producto, String> repositorio = new RepositorioEnMemoria<>(Producto::getCodigo);
    private final ServicioProductos servicio = new ServicioProductos(repositorio);

    @ParameterizedTest
    @EnumSource(FormaVenta.class)
    void registrarCreaLaVarianteConLaValidacionCorrespondiente(FormaVenta forma) {
        Producto producto = registrar(" P-001 ", forma);

        assertSame(producto, servicio.consultar(" P-001 "));
        assertSame(producto, servicio.consultarActivo("P-001"));
        if (forma == FormaVenta.POR_PESO) {
            assertDoesNotThrow(() -> producto.validarCantidad(new BigDecimal("1.250")));
        } else {
            assertThrows(CantidadInvalidaException.class,
                    () -> producto.validarCantidad(new BigDecimal("1.250")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplicadoNoCambiaProductoPrecioOFormaDeVenta(boolean desactivar) {
        Producto original = registrar("P-001", FormaVenta.POR_PESO);
        if (desactivar) {
            servicio.desactivar("P-001");
        }

        assertThrows(IdentificadorDuplicadoException.class,
                () -> servicio.registrar(" P-001 ", "Otro", "Bovino", "Preparado",
                        FormaVenta.POR_UNIDAD, new BigDecimal("5000")));

        assertAll(
                () -> assertSame(original, servicio.consultar("P-001")),
                () -> assertEquals(new BigDecimal("24000.00"), original.getPrecio()),
                () -> assertDoesNotThrow(() -> original.validarCantidad(new BigDecimal("1.250"))),
                () -> assertEquals(1, servicio.listar().size())
        );
    }

    @Test
    void actualizarPrecioConservaIdentidadYModificaLosCalculosPosteriores() {
        Producto original = registrar("P-001", FormaVenta.POR_PESO);

        servicio.actualizarPrecio(" P-001 ", new BigDecimal("20000"));

        assertSame(original, servicio.consultar("P-001"));
        assertEquals(new BigDecimal("25000.00"), original.calcularSubtotal(new BigDecimal("1.250")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.001"})
    void precioInvalidoConservaLosDatosDelProducto(String valor) {
        Producto producto = registrar("P-001", FormaVenta.POR_PESO);
        BigDecimal precio = valor == null ? null : new BigDecimal(valor);

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizarPrecio("P-001", precio));

        assertEquals(new BigDecimal("24000.00"), producto.getPrecio());
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void desactivarConservaLaConsultaHistoricaYRechazaElUsoActivo() {
        Producto producto = registrar("P-001", FormaVenta.POR_PESO);

        servicio.desactivar("P-001");
        servicio.desactivar("P-001");

        assertAll(
                () -> assertSame(producto, servicio.consultar("P-001")),
                () -> assertFalse(producto.estaActivo()),
                () -> assertEquals(List.of(producto), servicio.listar()),
                () -> assertThrows(RegistroInactivoException.class, () -> servicio.consultarActivo("P-001"))
        );
    }

    @Test
    void desconocidoNoSeInsertaAlConsultarActualizarODesactivar() {
        assertAll(
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultar("P-001")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultarActivo("P-001")),
                () -> assertThrows(RegistroNoEncontradoException.class,
                        () -> servicio.actualizarPrecio("P-001", new BigDecimal("100"))),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.desactivar("P-001"))
        );
        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void datosOFormaDeVentaInvalidosNoCreanRegistros() {
        assertAll(
                () -> assertThrows(ReglaNegocioException.class, () -> registrar(" ", FormaVenta.POR_PESO)),
                () -> assertThrows(ReglaNegocioException.class, () -> registrar("P-001", null)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> servicio.registrar("P-001", "Carne", "Bovino", "Molida",
                                FormaVenta.POR_PESO, BigDecimal.ZERO)),
                () -> assertThrows(ReglaNegocioException.class, () -> servicio.consultar(null))
        );
        assertTrue(servicio.listar().isEmpty());
    }

    private Producto registrar(String codigo, FormaVenta forma) {
        return servicio.registrar(codigo, "Carne", "Bovino", "Molida", forma, new BigDecimal("24000"));
    }
}
