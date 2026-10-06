package co.trazacarne.dominio;

import co.trazacarne.excepcion.CantidadInvalidaException;
import co.trazacarne.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoTest {

    @ParameterizedTest(name = "El peso {0} kg es válido")
    @ValueSource(strings = {"1.250", "0.001", "1.2340", "1E+2"})
    void pesoAdmiteCantidadesPositivasHastaTresDecimales(String valor) {
        Producto producto = productoPorPeso();

        assertDoesNotThrow(() -> producto.validarCantidad(decimal(valor)));
    }

    @ParameterizedTest(name = "El peso {0} kg se rechaza")
    @NullSource
    @ValueSource(strings = {"0", "-1", "-0.001", "1.2345", "0.0001"})
    void pesoRechazaCantidadesAusentesNoPositivasOConDemasiadaPrecision(String valor) {
        Producto producto = productoPorPeso();

        assertThrows(CantidadInvalidaException.class,
                () -> producto.validarCantidad(decimal(valor)));
    }

    @ParameterizedTest(name = "La cantidad {0} representa unidades enteras")
    @ValueSource(strings = {"1", "3.0", "3.000", "1E+2"})
    void unidadAdmiteEnterosAunqueSuRepresentacionTengaDecimales(String valor) {
        Producto producto = productoPorUnidad();

        assertDoesNotThrow(() -> producto.validarCantidad(decimal(valor)));
    }

    @ParameterizedTest(name = "La cantidad {0} no es válida por unidad")
    @NullSource
    @ValueSource(strings = {"0", "-1", "3.5", "0.001"})
    void unidadRechazaCantidadesAusentesNoPositivasOFraccionarias(String valor) {
        Producto producto = productoPorUnidad();

        assertThrows(CantidadInvalidaException.class,
                () -> producto.validarCantidad(decimal(valor)));
    }

    @Test
    void cadaProductoValidaSegunSuFormaDeVentaMedianteElContratoComun() {
        Producto porPeso = productoPorPeso();
        Producto porUnidad = productoPorUnidad();
        BigDecimal cantidad = decimal("1.500");

        assertAll(
                () -> assertDoesNotThrow(() -> porPeso.validarCantidad(cantidad)),
                () -> assertThrows(CantidadInvalidaException.class,
                        () -> porUnidad.validarCantidad(cantidad))
        );
    }

    @Test
    void constructorNormalizaLosTextosObligatorios() {
        Producto producto = new ProductoPorPeso(
                "  P-001  ", "\tCarne de res\t", "  Bovino\n", "  Lomo  ", decimal("18000"));

        assertAll(
                () -> assertEquals("P-001", producto.getCodigo()),
                () -> assertEquals("Carne de res", producto.getNombre()),
                () -> assertEquals("Bovino", producto.getEspecie()),
                () -> assertEquals("Lomo", producto.getTipoCorte())
        );
    }

    @ParameterizedTest(name = "El texto obligatorio [{0}] se rechaza")
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void constructorRechazaCualquierTextoObligatorioAusenteOEnBlanco(String texto) {
        BigDecimal precio = decimal("18000");

        assertAll(
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new ProductoPorPeso(texto, "Carne", "Bovino", "Lomo", precio)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new ProductoPorPeso("P-001", texto, "Bovino", "Lomo", precio)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new ProductoPorPeso("P-001", "Carne", texto, "Lomo", precio)),
                () -> assertThrows(ReglaNegocioException.class,
                        () -> new ProductoPorPeso("P-001", "Carne", "Bovino", texto, precio))
        );
    }

    @Test
    void constructorAdmiteCerosDecimalesAdicionalesYNormalizaElPrecio() {
        Producto producto = new ProductoPorPeso(
                "P-001", "Carne", "Bovino", "Lomo", decimal("12.3400"));

        assertEquals(decimal("12.34"), producto.getPrecio());
    }

    @ParameterizedTest(name = "El precio inicial {0} se rechaza")
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.001"})
    void constructorRechazaPreciosAusentesNoPositivosOConMasDeDosDecimales(String valor) {
        assertThrows(ReglaNegocioException.class,
                () -> new ProductoPorPeso("P-001", "Carne", "Bovino", "Lomo", decimal(valor)));
    }

    @Test
    void actualizarPrecioGuardaElNuevoValorConDosDecimales() {
        Producto producto = productoPorPeso();

        producto.actualizarPrecio(decimal("19.5000"));

        assertEquals(decimal("19.50"), producto.getPrecio());
    }

    @ParameterizedTest(name = "Actualizar con {0} conserva el precio anterior")
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.001"})
    void actualizarConPrecioInvalidoConservaElPrecioAnterior(String valor) {
        Producto producto = productoPorPeso();
        BigDecimal precioAnterior = producto.getPrecio();

        assertThrows(ReglaNegocioException.class,
                () -> producto.actualizarPrecio(decimal(valor)));

        assertEquals(precioAnterior, producto.getPrecio());
    }

    @Test
    void subtotalPorPesoMultiplicaLaCantidadPorElPrecio() {
        Producto producto = productoPorPeso();

        assertEquals(decimal("40500.00"), producto.calcularSubtotal(decimal("2.250")));
    }

    @Test
    void subtotalPorUnidadMultiplicaLasUnidadesPorElPrecio() {
        Producto producto = productoPorUnidad();

        assertEquals(decimal("54000.00"), producto.calcularSubtotal(decimal("3.000")));
    }

    @Test
    void subtotalRedondeaHaciaArribaCuandoElTercerDecimalEsCinco() {
        Producto producto = new ProductoPorPeso(
                "P-001", "Carne", "Bovino", "Lomo", decimal("10.04"));

        assertEquals(decimal("1.26"), producto.calcularSubtotal(decimal("0.125")));
    }

    @Test
    void subtotalRedondeaHaciaAbajoCuandoElTercerDecimalEsMenorQueCinco() {
        Producto producto = new ProductoPorPeso(
                "P-001", "Carne", "Bovino", "Lomo", decimal("10.03"));

        assertEquals(decimal("1.25"), producto.calcularSubtotal(decimal("0.125")));
    }

    @ParameterizedTest(name = "El subtotal por peso rechaza la cantidad {0}")
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.2345"})
    void subtotalPorPesoValidaLaCantidadAntesDeCalcular(String valor) {
        Producto producto = productoPorPeso();

        assertThrows(CantidadInvalidaException.class,
                () -> producto.calcularSubtotal(decimal(valor)));
    }

    @Test
    void subtotalPorUnidadAplicaLaValidacionPolimorficaDeEnteros() {
        Producto producto = productoPorUnidad();

        assertThrows(CantidadInvalidaException.class,
                () -> producto.calcularSubtotal(decimal("1.500")));
    }

    @Test
    void actualizarPrecioAfectaLosSubtotalesPosteriores() {
        Producto producto = productoPorPeso();

        producto.actualizarPrecio(decimal("20000"));

        assertEquals(decimal("25000.00"), producto.calcularSubtotal(decimal("1.250")));
    }

    @Test
    void desactivarCambiaElEstadoDelProducto() {
        Producto producto = productoPorPeso();
        assertTrue(producto.estaActivo());

        producto.desactivar();

        assertFalse(producto.estaActivo());
    }

    private static Producto productoPorPeso() {
        return new ProductoPorPeso("P-001", "Carne", "Bovino", "Lomo", decimal("18000"));
    }

    private static Producto productoPorUnidad() {
        return new ProductoPorUnidad("P-002", "Pieza", "Bovino", "Costilla", decimal("18000"));
    }

    private static BigDecimal decimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
