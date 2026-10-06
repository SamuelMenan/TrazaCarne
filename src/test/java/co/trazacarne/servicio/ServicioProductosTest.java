package co.trazacarne.servicio;

import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.ProductoPorUnidad;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioProductosTest {

    private ServicioProductos servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioProductos(new RepositorioEnMemoria<>(Producto::getCodigo));
    }

    @Test
    void creaLaVarianteSegunLaFormaDeVenta() {
        servicio.registrar("P-001", "Carne molida", "Bovino", "Molida", "PESO", new BigDecimal("24000"));
        servicio.registrar("P-002", "Hamburguesa", "Bovino", "Preparado", "UNIDAD", new BigDecimal("6500"));

        assertTrue(servicio.consultar("P-001") instanceof ProductoPorPeso);
        assertTrue(servicio.consultar("P-002") instanceof ProductoPorUnidad);
    }

    @Test
    void formaDeVentaDesconocidaSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar("P-003", "Lomo", "Bovino", "Lomo",
                "LITRO", new BigDecimal("30000")));
    }

    @Test
    void codigoRepetidoSeRechazaSinReemplazarAlOriginal() {
        servicio.registrar("P-001", "Carne molida", "Bovino", "Molida", "PESO", new BigDecimal("24000"));

        assertThrows(IdentificadorDuplicadoException.class, () -> servicio.registrar("P-001", "Otro", "Porcino",
                "Lomo", "PESO", new BigDecimal("1000")));
        assertEquals("Carne molida", servicio.consultar("P-001").getNombre());
    }

    @Test
    void actualizarPrecioCambiaElPrecio() {
        servicio.registrar("P-001", "Carne molida", "Bovino", "Molida", "PESO", new BigDecimal("24000"));
        servicio.actualizarPrecio("P-001", new BigDecimal("25000"));

        assertEquals(new BigDecimal("25000.00"), servicio.consultar("P-001").getPrecio());
    }
}
