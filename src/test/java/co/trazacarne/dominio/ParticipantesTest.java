package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantesTest {

    @Test
    void clienteNormalizaSuIdentidadYActualizaElNombreSinCambiarDocumento() {
        Cliente cliente = new Cliente(" 1001 ", " Ana ");

        cliente.actualizar(" Ana María ");

        assertAll(
                () -> assertEquals("1001", cliente.getDocumento()),
                () -> assertEquals("Ana María", cliente.getNombre()),
                () -> assertTrue(cliente.estaActivo())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void clienteRechazaDocumentoONombreAusentes(String texto) {
        assertAll(
                () -> assertThrows(ReglaNegocioException.class, () -> new Cliente(texto, "Ana")),
                () -> assertThrows(ReglaNegocioException.class, () -> new Cliente("1001", texto))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void clienteConservaNombreSiLaActualizacionEsInvalida(String texto) {
        Cliente cliente = new Cliente("1001", "Ana");

        assertThrows(ReglaNegocioException.class, () -> cliente.actualizar(texto));

        assertEquals("Ana", cliente.getNombre());
    }

    @Test
    void clienteDesactivadoConservaDatosYPermiteCorregirSuNombre() {
        Cliente cliente = new Cliente("1001", "Ana");

        cliente.desactivar();
        cliente.desactivar();
        cliente.actualizar("Ana María");

        assertAll(
                () -> assertFalse(cliente.estaActivo()),
                () -> assertEquals("1001", cliente.getDocumento()),
                () -> assertEquals("Ana María", cliente.getNombre())
        );
    }

    @ParameterizedTest
    @EnumSource(TipoAbastecedor.class)
    void abastecedorMantieneNitYTipoAlActualizarNombre(TipoAbastecedor tipo) {
        Abastecedor abastecedor = new Abastecedor(" 9001 ", " Origen ", tipo);

        abastecedor.actualizar(" Origen actualizado ");

        assertAll(
                () -> assertEquals("9001", abastecedor.getNit()),
                () -> assertEquals("Origen actualizado", abastecedor.getNombre()),
                () -> assertEquals(tipo, abastecedor.getTipo()),
                () -> assertEquals(tipo == TipoAbastecedor.MATADERO, abastecedor.esMatadero())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void abastecedorRechazaNitONombreAusentes(String texto) {
        assertAll(
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new Abastecedor(texto, "Origen", TipoAbastecedor.PROVEEDOR)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new Abastecedor("9001", texto, TipoAbastecedor.PROVEEDOR))
        );
    }

    @Test
    void abastecedorRequiereTipo() {
        assertThrows(ReglaNegocioException.class, () -> new Abastecedor("9001", "Origen", null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void abastecedorConservaNombreSiLaActualizacionEsInvalida(String texto) {
        Abastecedor abastecedor = new Abastecedor("9001", "Origen", TipoAbastecedor.MATADERO);

        assertThrows(ReglaNegocioException.class, () -> abastecedor.actualizar(texto));

        assertEquals("Origen", abastecedor.getNombre());
    }

    @Test
    void abastecedorDesactivadoConservaSuOrigen() {
        Abastecedor abastecedor = new Abastecedor("9001", "Origen", TipoAbastecedor.MATADERO);

        abastecedor.desactivar();
        abastecedor.desactivar();

        assertAll(
                () -> assertFalse(abastecedor.estaActivo()),
                () -> assertEquals("9001", abastecedor.getNit()),
                () -> assertTrue(abastecedor.esMatadero())
        );
    }
}
