package co.trazacarne.servicio;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioClientesTest {

    private final Repositorio<Cliente, String> repositorio = new RepositorioEnMemoria<>(Cliente::getDocumento);
    private final ServicioClientes servicio = new ServicioClientes(repositorio);

    @Test
    void registrarYConsultarUsanElDocumentoNormalizado() {
        Cliente cliente = servicio.registrar(" 1001 ", " Ana ");

        assertAll(
                () -> assertSame(cliente, servicio.consultar(" 1001 ")),
                () -> assertSame(cliente, servicio.consultarActivo("1001")),
                () -> assertEquals(List.of(cliente), servicio.listar())
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplicadoNoReemplazaClienteActivoOInactivo(boolean desactivar) {
        Cliente original = servicio.registrar("1001", "Ana");
        if (desactivar) {
            servicio.desactivar("1001");
        }

        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrar(" 1001 ", "Luis"));

        assertAll(
                () -> assertSame(original, servicio.consultar("1001")),
                () -> assertEquals("Ana", original.getNombre()),
                () -> assertEquals(1, servicio.listar().size())
        );
    }

    @Test
    void actualizarConservaDocumentoYReferenciasExistentes() {
        Cliente original = servicio.registrar("1001", "Ana");

        servicio.actualizar(" 1001 ", " Ana María ");

        assertAll(
                () -> assertSame(original, servicio.consultar("1001")),
                () -> assertEquals("Ana María", original.getNombre()),
                () -> assertEquals("1001", original.getDocumento())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void actualizarNombreInvalidoConservaElRegistro(String nombre) {
        Cliente cliente = servicio.registrar("1001", "Ana");

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar("1001", nombre));

        assertEquals("Ana", cliente.getNombre());
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void desactivarConservaLaConsultaHistoricaYRechazaElUsoActivo() {
        Cliente cliente = servicio.registrar("1001", "Ana");

        servicio.desactivar("1001");
        servicio.desactivar("1001");

        assertAll(
                () -> assertSame(cliente, servicio.consultar("1001")),
                () -> assertFalse(cliente.estaActivo()),
                () -> assertEquals(List.of(cliente), servicio.listar()),
                () -> assertThrows(RegistroInactivoException.class, () -> servicio.consultarActivo("1001"))
        );
    }

    @Test
    void identificadorDesconocidoNoSeInsertaAlConsultarActualizarODesactivar() {
        assertAll(
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultar("1001")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.consultarActivo("1001")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.actualizar("1001", "Ana")),
                () -> assertThrows(RegistroNoEncontradoException.class, () -> servicio.desactivar("1001"))
        );
        assertTrue(servicio.listar().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void documentoAusenteSeRechazaSinGuardar(String documento) {
        assertAll(
                () -> assertThrows(ReglaNegocioException.class, () -> servicio.registrar(documento, "Ana")),
                () -> assertThrows(ReglaNegocioException.class, () -> servicio.consultar(documento))
        );
        assertTrue(servicio.listar().isEmpty());
    }
}
