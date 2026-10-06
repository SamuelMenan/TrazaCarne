package co.trazacarne.servicio;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioClientesTest {

    private ServicioClientes servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioClientes(new RepositorioEnMemoria<>(Cliente::getDocumento));
        servicio.registrar("123", "Ana");
    }

    @Test
    void documentoVacioSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar("  ", "Luis"));
    }

    @Test
    void documentoRepetidoSeRechazaSinReemplazarAlOriginal() {
        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrar("123", "Luis"));
        assertEquals("Ana", servicio.consultar("123").getNombre());
    }

    @Test
    void consultarUnClienteInexistenteExplicaElMotivo() {
        assertThrows(ReglaNegocioException.class, () -> servicio.consultar("999"));
    }

    @Test
    void actualizarCambiaElNombre() {
        servicio.actualizar("123", "Ana María");
        assertEquals("Ana María", servicio.consultar("123").getNombre());
    }

    @Test
    void desactivarConservaElRegistroYSigueContandoComoDuplicado() {
        servicio.desactivar("123");

        assertFalse(servicio.consultar("123").estaActivo());
        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrar("123", "Otra"));
    }
}
