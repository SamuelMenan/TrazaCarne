package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.VentaYaAnuladaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Venta a un cliente; nace en borrador y solo descuenta inventario al registrarse. */
public class Venta {
    private final String numero;
    private final LocalDate fecha;
    private final Cliente cliente;
    private final List<DetalleVenta> detalles;
    // Único dato que cambia: BORRADOR -> REGISTRADA -> ANULADA.
    private EstadoVenta estado = EstadoVenta.BORRADOR;

    // Cada producto aparece una sola vez en la venta.
    public Venta(String numero, LocalDate fecha, Cliente cliente, List<DetalleVenta> detalles) {
        if (numero == null || numero.isBlank() || fecha == null || cliente == null) {
            throw new ReglaNegocioException("La venta requiere número, fecha y cliente.");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new ReglaNegocioException("La venta requiere al menos un detalle.");
        }
        Set<String> productos = new HashSet<>();
        for (DetalleVenta detalle : detalles) {
            if (detalle == null || !productos.add(detalle.getProducto().getCodigo())) {
                throw new ReglaNegocioException("Los detalles no pueden ser nulos ni repetir productos.");
            }
        }
        this.numero = numero.strip();
        this.fecha = fecha;
        this.cliente = cliente;
        this.detalles = List.copyOf(detalles);
    }

    // Revisa que cliente y productos sigan activos y luego descuenta las existencias.
    public void registrar(Inventario inventario, LocalDateTime instante) {
        if (estado != EstadoVenta.BORRADOR || inventario == null) {
            throw new ReglaNegocioException("Solo una venta en borrador puede registrarse en un inventario.");
        }
        if (!cliente.estaActivo()) {
            throw new RegistroInactivoException("El cliente está inactivo.");
        }
        for (DetalleVenta detalle : detalles) {
            if (!detalle.getProducto().estaActivo()) {
                throw new RegistroInactivoException("El producto " + detalle.getProducto().getCodigo() + " está inactivo.");
            }
        }
        inventario.registrarSalida(this, instante);
        estado = EstadoVenta.REGISTRADA;
    }

    // La venta no se borra: cambia a ANULADA y las cantidades vuelven a sus lotes.
    public void anular(Inventario inventario, LocalDateTime instante) {
        if (estado == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta " + numero + " ya fue anulada.");
        }
        if (estado != EstadoVenta.REGISTRADA || inventario == null) {
            throw new ReglaNegocioException("Solo una venta registrada puede anularse.");
        }
        inventario.devolver(this, instante);
        estado = EstadoVenta.ANULADA;
    }

    // Suma de los subtotales de cada detalle.
    public BigDecimal calcularTotal() {
        BigDecimal total = new BigDecimal("0.00");
        for (DetalleVenta detalle : detalles) {
            total = total.add(detalle.calcularSubtotal());
        }
        return total;
    }

    // --- Getters ---

    public String getNumero() { return numero; }
    public LocalDate getFecha() { return fecha; }
    public Cliente getCliente() { return cliente; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public EstadoVenta getEstado() { return estado; }
}
