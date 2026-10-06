package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioAbastecedoresTest {

    private ServicioAbastecedores servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioAbastecedores(new RepositorioEnMemoria<>(Abastecedor::getNit));
        servicio.registrar("900", "Frigorífico", TipoAbastecedor.MATADERO);
    }

    @Test
    void registraYConsultaConSuTipo() {
        assertTrue(servicio.consultar("900").esMatadero());
    }

    @Test
    void nitRepetidoSeRechazaSinReemplazarAlOriginal() {
        assertThrows(IdentificadorDuplicadoException.class,
                () -> servicio.registrar("900", "Otro", TipoAbastecedor.PROVEEDOR));
        assertEquals("Frigorífico", servicio.consultar("900").getNombre());
    }

    @Test
    void tipoAusenteSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar("901", "Sin tipo", null));
    }

    @Test
    void desactivarConservaElRegistro() {
        servicio.desactivar("900");
        assertFalse(servicio.consultar("900").estaActivo());
    }
}
