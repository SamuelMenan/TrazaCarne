package co.trazacarne.dominio;

import co.trazacarne.excepcion.FechasInvalidasException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.StockInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Origen y fechas fijos; solo el inventario aplica descuentos y reposiciones. */
public class Lote {
    private final String codigo;
    // La compra guarda el origen del lote (abastecedor y fecha de recepción).
    private final Compra compra;
    private final Producto producto;
    // Recibida es fija; disponible baja con ventas y pérdidas y sube con anulaciones.
    private final BigDecimal cantidadRecibida;
    private BigDecimal cantidadDisponible;
    private final LocalDate fechaSacrificio;
    private final LocalDate fechaProcesamiento;
    private final LocalDate fechaVencimiento;

    /**
     * Crea un lote validando sus reglas de fechas. Sin {@code public}: los lotes se crean desde
     * {@link Compra#agregarLote}, no desde fuera del dominio.
     *
     * <p>Línea de tiempo que debe cumplirse:
     * <pre>
     *   sacrificio  &lt;=  procesamiento  &lt;=  recepción (fecha de compra)  &lt;  vencimiento
     * </pre>
     * <ul>
     *   <li>Sacrificio: obligatorio si el abastecedor es matadero; opcional si es proveedor.</li>
     *   <li>Procesamiento: no puede ser posterior a la recepción (no se recibe algo aún sin procesar)
     *       y debe ser antes del vencimiento.</li>
     *   <li>Vencimiento: debe ser posterior a la recepción; no se aceptan lotes que lleguen vencidos.</li>
     * </ul>
     *
     * <p>Al crearse, la cantidad disponible es igual a la recibida.
     */
    Lote(String codigo, Compra compra, Producto producto, BigDecimal cantidad,
         LocalDate sacrificio, LocalDate procesamiento, LocalDate vencimiento) {
        if (codigo == null || codigo.isBlank() || compra == null || producto == null) {
            throw new ReglaNegocioException("El lote requiere código, compra y producto.");
        }
        if (!producto.estaActivo()) {
            throw new RegistroInactivoException("El producto está inactivo.");
        }
        producto.validarCantidad(cantidad);
        if (procesamiento == null || vencimiento == null) {
            throw new FechasInvalidasException("Procesamiento y vencimiento son obligatorios.");
        }
        if (compra.getAbastecedor().esMatadero() && sacrificio == null) {
            throw new FechasInvalidasException("Un lote de matadero requiere fecha de sacrificio.");
        }
        if (sacrificio != null && sacrificio.isAfter(procesamiento)) {
            throw new FechasInvalidasException("El sacrificio no puede ser posterior al procesamiento.");
        }
        if (!procesamiento.isBefore(vencimiento) || procesamiento.isAfter(compra.getFecha())) {
            throw new FechasInvalidasException("El procesamiento debe preceder al vencimiento y no superar la recepción.");
        }
        if (!vencimiento.isAfter(compra.getFecha())) {
            throw new FechasInvalidasException("No se puede recibir un lote vencido.");
        }
        this.codigo = codigo.strip();
        this.compra = compra;
        this.producto = producto;
        this.cantidadRecibida = cantidad;
        this.cantidadDisponible = cantidad;
        this.fechaSacrificio = sacrificio;
        this.fechaProcesamiento = procesamiento;
        this.fechaVencimiento = vencimiento;
    }

    // Acceso de paquete: el inventario registra los movimientos al cambiar cantidades.
    void descontar(BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        if (cantidad.compareTo(cantidadDisponible) > 0) {
            throw new StockInsuficienteException("El lote " + codigo + " no tiene cantidad suficiente.");
        }
        cantidadDisponible = cantidadDisponible.subtract(cantidad);
    }

    // Se usa al anular una venta; nunca puede superar lo recibido.
    void reponer(BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        BigDecimal resultado = cantidadDisponible.add(cantidad);
        if (resultado.compareTo(cantidadRecibida) > 0) {
            throw new ReglaNegocioException("La reposición supera lo recibido en el lote " + codigo + ".");
        }
        cantidadDisponible = resultado;
    }

    // El día del vencimiento ya cuenta como vencido.
    public boolean estaVencido(LocalDate hoy) {
        if (hoy == null) {
            throw new FechasInvalidasException("La fecha de consulta es obligatoria.");
        }
        return !fechaVencimiento.isAfter(hoy);
    }

    // Vendible = no vencido, ya recibido, producto activo y con existencias.
    public boolean puedeVenderse(LocalDate hoy) {
        return !estaVencido(hoy) && !compra.getFecha().isAfter(hoy)
                && producto.estaActivo() && cantidadDisponible.signum() > 0;
    }

    // --- Getters ---

    public String getCodigo() { return codigo; }
    public Compra getCompra() { return compra; }
    public Abastecedor getAbastecedor() { return compra.getAbastecedor(); }
    public Producto getProducto() { return producto; }
    public BigDecimal getCantidadRecibida() { return cantidadRecibida; }
    public BigDecimal getCantidadDisponible() { return cantidadDisponible; }
    public LocalDate getFechaSacrificio() { return fechaSacrificio; }
    public LocalDate getFechaProcesamiento() { return fechaProcesamiento; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
}
