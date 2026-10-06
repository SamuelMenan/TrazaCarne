package co.trazacarne;

import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.ProductoPorUnidad;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {
        System.out.println("TrazaCarne: demostración inicial de productos");

        Producto carneMolida = new ProductoPorPeso(
                "P-001", "Carne molida", "Bovino", "Molida", new BigDecimal("24000.00"));
        Producto hamburguesa = new ProductoPorUnidad(
                "P-002", "Hamburguesa", "Bovino", "Preparado", new BigDecimal("6500.00"));

        mostrarSubtotal(carneMolida, new BigDecimal("1.250"));
        mostrarSubtotal(hamburguesa, new BigDecimal("3"));
    }

    private static void mostrarSubtotal(Producto producto, BigDecimal cantidad) {
        BigDecimal subtotal = producto.calcularSubtotal(cantidad);
        System.out.println(producto.getNombre() + " | cantidad: " + cantidad.toPlainString()
                + " | subtotal: " + subtotal.toPlainString() + " COP");
    }
}
