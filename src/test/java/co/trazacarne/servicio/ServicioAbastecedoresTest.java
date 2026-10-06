package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioAbastecedoresTest {

    private final Repositorio<Abastecedor, String> repositorio = new RepositorioEnMemoria<>(Abastecedor::getNit);
    private final ServicioAbastecedores servicio = new ServicioAbastecedores(repositorio);

    @ParameterizedTest
    @EnumSource(TipoAbastecedor.class)
    void registrarYConsultarConservanElTipoDeOrigen(TipoAbastecedor tipo) {
        Abastecedor abastecedor = servicio.registrar(" 9001 ", " Origen ", tipo);

        assertAll(
                () -> assertSame(abastecedor, servicio.consultar(" 9001 ")),
                () -> assertSame(abastecedor, servicio.consultarActivo("9001")),
                () -> assertEquals(tipo, abastecedor.getTipo()),
                () -> assertEquals(List.of(abastecedor), servicio.listar())
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nitDuplicadoNoReemplazaOrigenActivoOInactivo(boolean desactivar) {
        Abastecedor original = servicio.registrar("9001", "Origen", TipoAbastecedor.MATADERO);
        if (desactivar) {
            servicio.desactivar("9001");
        }

        assertThrows(IdentificadorDuplicadoException.class,
                () -> servicio.registrar(" 9001 ", "Otro origen", TipoAbastecedor.PROVEEDOR));

        assertAll(
                () -> assertSame(original, servicio.consultar("9001")),
                () -> assertTrue(original.esMatadero()),
                () -> assertEquals(1, servicio.listar().size())
        );
    }

    @Test
    void actualizarNombreConservaLaIdentidadYElTipo() {
        Abastecedor original = servicio.registrar("9001", "Origen", TipoAbastecedor.MATADERO);

        servicio.actualizar(" 9001 ", " Origen actualizado ");

        assertAll(
                () -> assertSame(original, servicio.consultar("9001")),
                () -> assertEquals("Origen actualizado", original.getNombre()),
                () -> assertTrue(original.esMatadero())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void actualizarNombreInvalidoConservaElRegistro(String nombre) {
        Abastecedor abastecedor = servicio.registrar("9001", "Origen", TipoAbastecedor.PROVEEDOR);

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar("9001", nombre));

        assertEquals("Origen", abastecedor.getNombre());
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void desactivarConservaElOrigenYRechazaElUsoActivo() {
        Abastecedor abastecedor = servicio.registrar("9001", "Origen", TipoAbastecedor.MATADERO);

        servicio.desactivar("9001");
        servicio.desactivar("9001");

        assertAll(
                () -> assertSame(abastecedor, servicio.consultar("9001")),
                () -> assertFalse(abastecedor.estaActivo()),
                () -> assertEquals(List.of(abastecedor), servicio.listar()),
                () -> assertThrows(RegistroInactivoException.class, () -> servicio.consultarActivo("9001"))
        );
    }

    @Test
    void desconocidoNoSeInsertaAlConsultarActualizarODesactivar() {
        assertAll(
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultar("9001")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultarActivo("9001")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.actualizar("9001", "Origen")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.desactivar("9001"))
        );
        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void datosInvalidosNoCreanRegistros() {
        assertAll(
                () -> assertThrows(ReglaNegocioException.class,
                        () -> servicio.registrar(null, "Origen", TipoAbastecedor.PROVEEDOR)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> servicio.registrar("9001", " ", TipoAbastecedor.PROVEEDOR)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> servicio.registrar("9001", "Origen", null)),
                () -> assertThrows(ReglaNegocioException.class, () -> servicio.consultar(" "))
        );
        assertTrue(servicio.listar().isEmpty());
    }
}
