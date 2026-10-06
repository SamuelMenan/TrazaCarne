package co.trazacarne.dominio;

import co.trazacarne.excepcion.FechasInvalidasException;
import co.trazacarne.excepcion.StockInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cantidad de un producto recibida en una compra, con su origen y fechas fijas (RN-03, RN-04). */
public class Lote {

    private final String codigo;
    private final Compra compra;
    private final Producto producto;
    private final BigDecimal cantidadRecibida;
    private BigDecimal cantidadDisponible;
    private final LocalDate fechaSacrificio;
    private final LocalDate fechaProcesamiento;
    private final LocalDate fechaVencimiento;

    /** Visibilidad de paquete (~ en el diagrama): solo Compra.agregarLote crea lotes. */
    Lote(String codigo, Compra compra, Producto producto, BigDecimal cantidad,
         LocalDate fechaSacrificio, LocalDate fechaProcesamiento, LocalDate fechaVencimiento) {
        this.codigo = Validar.texto(codigo, "El código del lote");
        this.compra = Validar.obligatorio(compra, "La compra de origen");
        this.producto = Validar.obligatorio(producto, "El producto del lote");
        producto.validarCantidad(cantidad);
        validarFechas(compra.getAbastecedor(), fechaSacrificio, fechaProcesamiento, fechaVencimiento);
        this.cantidadRecibida = cantidad;
        this.cantidadDisponible = cantidad;
        this.fechaSacrificio = fechaSacrificio;
        this.fechaProcesamiento = fechaProcesamiento;
        this.fechaVencimiento = fechaVencimiento;
    }

    private static void validarFechas(Abastecedor abastecedor, LocalDate sacrificio,
                                      LocalDate procesamiento, LocalDate vencimiento) {
        if (procesamiento == null || vencimiento == null) {
            throw new FechasInvalidasException("Las fechas de procesamiento y vencimiento son obligatorias.");
        }
        if (!procesamiento.isBefore(vencimiento)) {
            throw new FechasInvalidasException("El procesamiento debe ser anterior al vencimiento.");
        }
        if (abastecedor.esMatadero() && sacrificio == null) {
            throw new FechasInvalidasException("Un lote de matadero exige fecha de sacrificio.");
        }
        if (sacrificio != null && sacrificio.isAfter(procesamiento)) {
            throw new FechasInvalidasException("El sacrificio no puede ser posterior al procesamiento.");
        }
    }

    /** Descuenta existencias; nunca deja la cantidad negativa (RN-07). */
    public void descontar(BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        if (cantidad.compareTo(cantidadDisponible) > 0) {
            throw new StockInsuficienteException(
                    "El lote " + codigo + " solo tiene " + cantidadDisponible.toPlainString() + ".");
        }
        cantidadDisponible = cantidadDisponible.subtract(cantidad);
    }

    /** Devuelve existencias al lote (anulación, RN-11). */
    public void reponer(BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        cantidadDisponible = cantidadDisponible.add(cantidad);
    }

    /** Decisión documentada: el lote está vencido desde su fecha de vencimiento. */
    public boolean estaVencido(LocalDate hoy) {
        return !hoy.isBefore(fechaVencimiento);
    }

    public Abastecedor getAbastecedor() {
        return compra.getAbastecedor();
    }

    public String getCodigo() {
        return codigo;
    }

    public Compra getCompra() {
        return compra;
    }

    public Producto getProducto() {
        return producto;
    }

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }

    public BigDecimal getCantidadDisponible() {
        return cantidadDisponible;
    }

    public LocalDate getFechaSacrificio() {
        return fechaSacrificio;
    }

    public LocalDate getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }
}
