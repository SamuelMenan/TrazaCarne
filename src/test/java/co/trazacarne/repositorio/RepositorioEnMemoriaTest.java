package co.trazacarne.repositorio;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioEnMemoriaTest {

    @Test
    void guardarDuplicadoConservaElRegistroOriginalInclusoInactivo() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();
        Cliente original = new Cliente("1001", "Ana");
        original.desactivar();
        repositorio.guardar(original);

        assertThrows(IdentificadorDuplicadoException.class,
                () -> repositorio.guardar(new Cliente("1001", "Otro nombre")));

        assertAll(
                () -> assertSame(original, repositorio.buscarPorId("1001").orElseThrow()),
                () -> assertEquals(1, repositorio.listar().size()),
                () -> assertFalse(repositorio.buscarPorId("1001").orElseThrow().estaActivo())
        );
    }

    @Test
    void buscarIdentificadorDesconocidoDevuelveOptionalVacio() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();

        assertAll(
                () -> assertTrue(repositorio.buscarPorId("desconocido").isEmpty()),
                () -> assertFalse(repositorio.existe("desconocido"))
        );
    }

    @Test
    void actualizarDesconocidoNoLoInserta() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();

        assertThrows(RegistroNoEncontradoException.class,
                () -> repositorio.actualizar(new Cliente("1001", "Ana")));

        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void actualizarExistenteConservaCantidadDeRegistros() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();
        repositorio.guardar(new Cliente("1001", "Ana"));
        Cliente actualizado = new Cliente("1001", "Ana María");

        repositorio.actualizar(actualizado);

        assertAll(
                () -> assertSame(actualizado, repositorio.buscarPorId("1001").orElseThrow()),
                () -> assertEquals(1, repositorio.listar().size())
        );
    }

    @Test
    void listarConservaOrdenEInactivosYNoPermiteAlterarLaColeccion() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();
        Cliente primero = new Cliente("1001", "Ana");
        primero.desactivar();
        Cliente segundo = new Cliente("1002", "Luis");
        repositorio.guardar(primero);
        repositorio.guardar(segundo);
        List<Cliente> lista = repositorio.listar();

        assertEquals(List.of(primero, segundo), lista);
        assertThrows(UnsupportedOperationException.class, lista::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> lista.add(new Cliente("1003", "Marta")));

        repositorio.guardar(new Cliente("1003", "Marta"));
        assertEquals(2, lista.size());
        assertEquals(3, repositorio.listar().size());
    }

    @Test
    void cadaInstanciaTieneSusPropiosDatos() {
        Repositorio<Cliente, String> primero = repositorioClientes();
        Repositorio<Cliente, String> segundo = repositorioClientes();
        primero.guardar(new Cliente("1001", "Ana"));

        assertFalse(segundo.existe("1001"));
    }

    @Test
    void identificadorPuedeTenerUnTipoDistintoDeString() {
        record Registro(Long id, String nombre) { }
        Repositorio<Registro, Long> repositorio = new RepositorioEnMemoria<>(Registro::id);
        Registro registro = new Registro(1L, "Dato");

        repositorio.guardar(registro);

        assertSame(registro, repositorio.buscarPorId(1L).orElseThrow());
    }

    @Test
    void entradasNulasSeRechazanSinModificarLosDatos() {
        Repositorio<Cliente, String> repositorio = repositorioClientes();

        assertAll(
                () -> assertThrows(ReglaNegocioException.class, () -> repositorio.guardar(null)),
                () -> assertThrows(ReglaNegocioException.class, () -> repositorio.actualizar(null)),
                () -> assertThrows(ReglaNegocioException.class, () -> repositorio.buscarPorId(null)),
                () -> assertThrows(ReglaNegocioException.class, () -> repositorio.existe(null))
        );
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void identificadorExtraidoNoPuedeSerNuloOBlanco() {
        Repositorio<String, String> identificadorNulo = new RepositorioEnMemoria<>(dato -> null);
        Repositorio<String, String> identificadorBlanco = new RepositorioEnMemoria<>(dato -> " ");

        assertAll(
                () -> assertThrows(ReglaNegocioException.class, () -> identificadorNulo.guardar("dato")),
                () -> assertThrows(ReglaNegocioException.class, () -> identificadorBlanco.guardar("dato"))
        );
    }

    private static Repositorio<Cliente, String> repositorioClientes() {
        return new RepositorioEnMemoria<>(Cliente::getDocumento);
    }
}
