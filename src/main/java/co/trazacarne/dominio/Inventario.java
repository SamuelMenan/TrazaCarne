package co.trazacarne.dominio;

import co.trazacarne.dominio.estrategia.EstrategiaAsignacion;
import co.trazacarne.excepcion.FechasInvalidasException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.excepcion.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Único punto que modifica existencias y registra los movimientos asociados. */
public class Inventario {
    // Lotes por código; compras y ventas ya aplicadas, para no aplicarlas dos veces.
    private final Map<String, Lote> lotes = new LinkedHashMap<>();
    private final Map<String, Compra> compras = new LinkedHashMap<>();
    private final Map<String, Venta> ventas = new LinkedHashMap<>();
    // Historial de todo lo que entra y sale; solo se le agregan registros.
    private final List<MovimientoInventario> movimientos = new ArrayList<>();
    // Patrón Strategy: decide de qué lotes sale cada venta (hoy se usa FEFO).
    private final EstrategiaAsignacion estrategia;

    public Inventario(EstrategiaAsignacion estrategia) {
        this.estrategia = Objects.requireNonNull(estrategia, "La estrategia de asignación es obligatoria.");
    }

    // --- Entradas (compras) ---

    /**
     * Agrega al inventario todos los lotes de una compra.
     *
     * <p>Funciona en dos fases para que la operación sea "todo o nada":
     * <ol>
     *   <li><b>Validar:</b> revisa la fecha, que la compra no se haya registrado antes, que el
     *       abastecedor esté activo y, lote por lote, que el código no exista y el producto esté activo.
     *       Mientras tanto prepara los movimientos de ENTRADA en una lista aparte, sin tocar nada.</li>
     *   <li><b>Aplicar:</b> solo si todo pasó, guarda los lotes, los movimientos y la compra.</li>
     * </ol>
     *
     * <p>Ejemplo: si una compra trae L-001 (válido) y L-002 (código repetido), se lanza el error
     * en L-002 y L-001 tampoco queda guardado. Así el inventario nunca queda a medias.
     *
     * <p>Sin {@code public}: solo se llama desde {@link Compra#registrar}.
     */
    void registrarEntrada(Compra compra, LocalDateTime instante) {
        validarInstante(instante);
        if (!compra.getFecha().equals(instante.toLocalDate())) {
            throw new FechasInvalidasException("La recepción debe registrarse en la fecha de la compra.");
        }
        if (compras.containsKey(compra.getNumero())) {
            throw new IdentificadorDuplicadoException("La compra ya tiene entradas registradas.");
        }
        if (!compra.getAbastecedor().estaActivo()) {
            throw new RegistroInactivoException("El abastecedor está inactivo.");
        }
        List<MovimientoInventario> entradas = new ArrayList<>();
        for (Lote lote : compra.getLotes()) {
            if (lotes.containsKey(lote.getCodigo())) {
                throw new IdentificadorDuplicadoException("Ya existe el lote " + lote.getCodigo() + ".");
            }
            if (!lote.getProducto().estaActivo()) {
                throw new RegistroInactivoException("El producto está inactivo.");
            }
            entradas.add(movimiento(entradas.size(), instante, TipoMovimiento.ENTRADA, lote,
                    lote.getCantidadRecibida(), compra.getNumero(), "Recepción de compra"));
        }
        // Solo se modifica el inventario cuando todos los lotes pasaron las validaciones.
        for (Lote lote : compra.getLotes()) { lotes.put(lote.getCodigo(), lote); }
        movimientos.addAll(entradas);
        compras.put(compra.getNumero(), compra);
    }

    // --- Salidas (ventas) ---

    /**
     * Calcula de qué lotes saldría una cantidad, <b>sin descontar nada</b>.
     *
     * <p>Delega en la estrategia (FEFO). Se separa "planificar" de "registrar" para que una venta
     * con varios productos pueda prepararse completa y, si algún producto no tiene stock, falle
     * antes de haber descontado los demás.
     */
    public List<AsignacionLote> planificar(Producto producto, BigDecimal cantidad, LocalDate hoy) {
        return estrategia.asignar(producto, cantidad, getLotes(), hoy);
    }

    /**
     * Descuenta de los lotes lo que se vendió. Sigue el mismo esquema de dos fases que la entrada.
     *
     * <ol>
     *   <li>{@link #cantidadesDe} agrupa cuánto se toma de cada lote en toda la venta.</li>
     *   <li><b>Validar:</b> para cada lote revisa que pertenezca a este inventario, que se pueda vender
     *       hoy y que tenga stock suficiente. Prepara los movimientos de SALIDA.</li>
     *   <li><b>Aplicar:</b> descuenta en cada lote, guarda los movimientos y marca la venta como aplicada.</li>
     * </ol>
     *
     * <p>Se vuelve a validar el stock aunque FEFO ya lo revisó al planificar, por seguridad:
     * entre planificar y registrar el lote pudo haber cambiado (por ejemplo, una pérdida).
     */
    void registrarSalida(Venta venta, LocalDateTime instante) {
        validarInstante(instante);
        if (!venta.getFecha().equals(instante.toLocalDate())) {
            throw new FechasInvalidasException("El registro debe coincidir con la fecha de venta.");
        }
        if (ventas.containsKey(venta.getNumero())) {
            throw new IdentificadorDuplicadoException("La venta ya tiene salidas registradas.");
        }
        Map<Lote, BigDecimal> cantidades = cantidadesDe(venta);
        List<MovimientoInventario> salidas = new ArrayList<>();
        for (Map.Entry<Lote, BigDecimal> entrada : cantidades.entrySet()) {
            Lote lote = entrada.getKey();
            exigirLoteRegistrado(lote);
            if (!lote.puedeVenderse(instante.toLocalDate())) {
                throw new ReglaNegocioException("El lote " + lote.getCodigo() + " no está disponible para venta.");
            }
            if (entrada.getValue().compareTo(lote.getCantidadDisponible()) > 0) {
                throw new StockInsuficienteException("Stock insuficiente en el lote " + lote.getCodigo() + ".");
            }
            salidas.add(movimiento(salidas.size(), instante, TipoMovimiento.SALIDA, lote,
                    entrada.getValue(), venta.getNumero(), "Registro de venta"));
        }
        for (Map.Entry<Lote, BigDecimal> entrada : cantidades.entrySet()) {
            entrada.getKey().descontar(entrada.getValue());
        }
        movimientos.addAll(salidas);
        ventas.put(venta.getNumero(), venta);
    }

    /**
     * Revierte una venta: devuelve a cada lote exactamente lo que se le descontó.
     *
     * <p>Condiciones:
     * <ul>
     *   <li>La venta debe haber sido registrada en <b>este</b> inventario
     *       ({@code ventas.get(numero) != venta} compara que sea el mismo objeto, no solo el mismo número).</li>
     *   <li>La anulación no puede tener fecha anterior a la venta.</li>
     *   <li>Ningún lote puede quedar con más de lo que recibió.</li>
     * </ul>
     *
     * <p>No se borran los movimientos de SALIDA: se agregan movimientos de DEVOLUCION.
     * Así el historial muestra que se vendió y luego se anuló.
     */
    void devolver(Venta venta, LocalDateTime instante) {
        validarInstante(instante);
        if (instante.toLocalDate().isBefore(venta.getFecha()) || ventas.get(venta.getNumero()) != venta) {
            throw new ReglaNegocioException("La devolución debe corresponder a una venta de este inventario.");
        }
        Map<Lote, BigDecimal> cantidades = cantidadesDe(venta);
        List<MovimientoInventario> devoluciones = new ArrayList<>();
        for (Map.Entry<Lote, BigDecimal> entrada : cantidades.entrySet()) {
            Lote lote = entrada.getKey();
            exigirLoteRegistrado(lote);
            if (lote.getCantidadDisponible().add(entrada.getValue()).compareTo(lote.getCantidadRecibida()) > 0) {
                throw new ReglaNegocioException("La devolución supera lo recibido en el lote " + lote.getCodigo() + ".");
            }
            devoluciones.add(movimiento(devoluciones.size(), instante, TipoMovimiento.DEVOLUCION, lote,
                    entrada.getValue(), venta.getNumero(), "Anulación de venta"));
        }
        for (Map.Entry<Lote, BigDecimal> entrada : cantidades.entrySet()) {
            entrada.getKey().reponer(entrada.getValue());
        }
        movimientos.addAll(devoluciones);
    }

    // Merma o daño de un lote; siempre exige un motivo.
    public void registrarPerdida(Lote lote, BigDecimal cantidad, String motivo, LocalDateTime instante) {
        validarInstante(instante);
        exigirLoteRegistrado(lote);
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("El motivo de la pérdida es obligatorio.");
        }
        lote.getProducto().validarCantidad(cantidad);
        if (cantidad.compareTo(lote.getCantidadDisponible()) > 0) {
            throw new StockInsuficienteException("La pérdida supera las existencias del lote " + lote.getCodigo() + ".");
        }
        MovimientoInventario perdida = movimiento(0, instante, TipoMovimiento.PERDIDA,
                lote, cantidad, lote.getCodigo(), motivo);
        lote.descontar(cantidad);
        movimientos.add(perdida);
    }

    // --- Consultas ---

    // Solo cuenta lo que se puede vender hoy (no vencido y con existencias).
    public BigDecimal disponible(Producto producto, LocalDate hoy) {
        if (producto == null || hoy == null) {
            throw new ReglaNegocioException("La consulta requiere producto y fecha.");
        }
        BigDecimal cantidad = BigDecimal.ZERO;
        for (Lote lote : lotesDe(producto)) {
            if (lote.puedeVenderse(hoy)) { cantidad = cantidad.add(lote.getCantidadDisponible()); }
        }
        return cantidad;
    }

    public List<Lote> lotesDe(Producto producto) {
        if (producto == null) { throw new ReglaNegocioException("El producto es obligatorio."); }
        List<Lote> resultado = new ArrayList<>();
        for (Lote lote : lotes.values()) {
            if (lote.getProducto().getCodigo().equals(producto.getCodigo())) { resultado.add(lote); }
        }
        return List.copyOf(resultado);
    }

    // Lotes con existencias que vencen dentro de los próximos "dias", del más urgente al menos.
    public List<Lote> proximosAVencer(LocalDate hoy, int dias) {
        if (hoy == null || dias < 0) {
            throw new ReglaNegocioException("La consulta requiere fecha y días no negativos.");
        }
        List<Lote> resultado = new ArrayList<>();
        for (Lote lote : lotes.values()) {
            long distancia = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());
            if (!lote.estaVencido(hoy) && distancia <= dias && lote.getCantidadDisponible().signum() > 0) {
                resultado.add(lote);
            }
        }
        resultado.sort(Comparator.comparing(Lote::getFechaVencimiento).thenComparing(Lote::getCodigo));
        return List.copyOf(resultado);
    }

    public Lote consultarLote(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("El código de lote es obligatorio.");
        }
        Lote lote = lotes.get(codigo.strip());
        if (lote == null) { throw new RegistroNoEncontradoException("No existe el lote " + codigo.strip() + "."); }
        return lote;
    }

    public List<Lote> getLotes() { return List.copyOf(lotes.values()); }
    public List<MovimientoInventario> getMovimientos() { return List.copyOf(movimientos); }

    // --- Métodos auxiliares ---

    /**
     * Suma cuánto se toma de cada lote en toda la venta.
     *
     * <p>{@code merge(lote, cantidad, BigDecimal::add)} significa: si el lote no está en el mapa,
     * lo agrega con esa cantidad; si ya está, le suma la cantidad nueva.
     * Hoy cada lote aparece una vez por venta, pero así queda protegido si en el futuro no fuera así.
     */
    private Map<Lote, BigDecimal> cantidadesDe(Venta venta) {
        Map<Lote, BigDecimal> resultado = new LinkedHashMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            for (AsignacionLote asignacion : detalle.getAsignaciones()) {
                resultado.merge(asignacion.getLote(), asignacion.getCantidad(), BigDecimal::add);
            }
        }
        return resultado;
    }

    private void exigirLoteRegistrado(Lote lote) {
        if (lote == null || lotes.get(lote.getCodigo()) != lote) {
            throw new RegistroNoEncontradoException("El lote no pertenece a este inventario.");
        }
    }

    /**
     * Garantiza que el historial esté en orden cronológico.
     *
     * <p>Falla si la fecha es nula o si es anterior a la del último movimiento registrado.
     * Ejemplo: si el último movimiento fue el 5 de octubre a las 10:00, no se acepta una
     * operación del 5 de octubre a las 9:00. Si todavía no hay movimientos, cualquier fecha sirve.
     */
    private void validarInstante(LocalDateTime instante) {
        if (instante == null || !movimientos.isEmpty()
                && instante.isBefore(movimientos.get(movimientos.size() - 1).getFecha())) {
            throw new FechasInvalidasException("La operación requiere fecha y no puede preceder al último movimiento.");
        }
    }

    /**
     * Crea un movimiento con id consecutivo.
     *
     * <p>Por qué existe {@code desplazamiento}: en la fase de validación los movimientos se preparan en una
     * lista aparte y todavía no están en {@code movimientos}. Si ya hay 10 movimientos y una compra
     * trae 3 lotes, los ids deben ser 11, 12 y 13; el desplazamiento (0, 1, 2) es la posición
     * dentro de esa lista temporal: id = 10 + desplazamiento + 1.
     */
    private MovimientoInventario movimiento(int desplazamiento, LocalDateTime instante, TipoMovimiento tipo,
                                             Lote lote, BigDecimal cantidad, String referencia, String motivo) {
        return new MovimientoInventario((long) movimientos.size() + desplazamiento + 1,
                instante, tipo, lote, cantidad, referencia, motivo);
    }
}
