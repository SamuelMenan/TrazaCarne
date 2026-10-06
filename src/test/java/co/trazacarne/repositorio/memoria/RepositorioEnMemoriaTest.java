package co.trazacarne.repositorio.memoria;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.repositorio.Repositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioEnMemoriaTest {

    private Repositorio<Cliente, String> repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria<>(Cliente::getDocumento);
        repositorio.guardar(new Cliente("123", "Ana"));
    }

    @Test
    void buscarUnIdExistenteLoEncuentra() {
        assertEquals("Ana", repositorio.buscarPorId("123").orElseThrow().getNombre());
        assertTrue(repositorio.existe("123"));
    }

    @Test
    void buscarUnIdInexistenteDevuelveVacio() {
        assertTrue(repositorio.buscarPorId("999").isEmpty());
        assertFalse(repositorio.existe("999"));
    }

    @Test
    void listarDevuelveUnaCopia() {
        repositorio.listar().clear();

        assertEquals(1, repositorio.listar().size());
    }
}
