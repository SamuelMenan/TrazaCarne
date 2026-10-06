package co.trazacarne.servicio;

import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.DetalleVenta;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.Venta;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.servicio.dto.DatosVenta;
import co.trazacarne.servicio.dto.ItemVenta;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registra y anula ventas, eligiendo los lotes de origen con la estrategia del inventario. */
public class ServicioVentas {
    private final Repositorio<Venta, String> repositorio;
    private final ServicioClientes clientes;
    private final ServicioProductos productos;
    private final Inventario inventario;
    private final Clock reloj;

    public ServicioVentas(Repositorio<Venta, String> repositorio, ServicioClientes clientes,
                          ServicioProductos productos, Inventario inventario, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.clientes = Objects.requireNonNull(clientes);
        this.productos = Objects.requireNonNull(productos);
        this.inventario = Objects.requireNonNull(inventario);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /**
     * Registra una venta completa. Es el flujo más importante del sistema.
     *
     * <ol>
     *   <li><b>Validar datos:</b> que haya productos, que el número no exista y que el cliente esté activo.</li>
     *   <li><b>Agrupar:</b> si el usuario pidió el mismo producto en dos líneas (2 kg + 1 kg de P-001),
     *       se juntan en una sola (3 kg). Se usan dos mapas: uno guarda el producto y otro la suma.
     *       {@code LinkedHashMap} conserva el orden en que se pidieron.</li>
     *   <li><b>Planificar:</b> para cada producto, el inventario (con FEFO) dice de qué lotes sale.
     *       Si un producto no tiene stock, la venta falla aquí y <b>nada</b> se ha descontado.</li>
     *   <li><b>Registrar:</b> la venta pasa de BORRADOR a REGISTRADA y el inventario descuenta todo
     *       de una sola vez. Luego se guarda en el repositorio.</li>
     * </ol>
     *
     * <p>La fecha sale del {@code reloj} inyectado, no de {@code LocalDateTime.now()} directo,
     * para poder controlar la fecha en pruebas y en la demostración.
     */
    public Venta registrarVenta(DatosVenta datos) {
        if (datos == null || datos.items() == null || datos.items().isEmpty()) {
            throw new ReglaNegocioException("La venta requiere al menos un producto.");
        }
        String numero = validarNumero(datos.numero());
        if (repositorio.existe(numero)) {
            throw new IdentificadorDuplicadoException("Ya existe la venta " + numero + ".");
        }
        Cliente cliente = clientes.consultarActivo(datos.documentoCliente());
        LocalDateTime instante = LocalDateTime.now(reloj);
        // 1. Agrupar: si el mismo producto viene en varias líneas, se suman sus cantidades.
        Map<String, Producto> porCodigo = new LinkedHashMap<>();
        Map<String, BigDecimal> cantidades = new LinkedHashMap<>();
        for (ItemVenta item : datos.items()) {
            Producto producto = productos.consultarActivo(item.codigoProducto());
            // Validar cada línea evita que una cantidad negativa compense otra positiva.
            producto.validarCantidad(item.cantidad());
            porCodigo.put(producto.getCodigo(), producto);
            cantidades.merge(producto.getCodigo(), item.cantidad(), BigDecimal::add);
        }
        // 2. Planificar: para cada producto, FEFO decide de qué lotes sale.
        List<DetalleVenta> detalles = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entrada : cantidades.entrySet()) {
            Producto producto = porCodigo.get(entrada.getKey());
            List<AsignacionLote> asignaciones = inventario.planificar(producto, entrada.getValue(), instante.toLocalDate());
            detalles.add(new DetalleVenta(producto, entrada.getValue(), asignaciones));
        }
        // 3. Registrar: preparar todos los productos no descuenta ninguna existencia;
        //    el descuento ocurre aquí, de una sola vez.
        Venta venta = new Venta(numero, instante.toLocalDate(), cliente, detalles);
        venta.registrar(inventario, instante);
        repositorio.guardar(venta);
        return venta;
    }

    // La venta queda ANULADA (no se borra) y el stock vuelve a sus lotes.
    public Venta anularVenta(String numero) {
        Venta venta = consultar(numero);
        venta.anular(inventario, LocalDateTime.now(reloj));
        repositorio.actualizar(venta);
        return venta;
    }

    public Venta consultar(String numero) {
        String id = validarNumero(numero);
        return repositorio.buscarPorId(id).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe la venta " + id + "."));
    }

    public List<Venta> listar() { return repositorio.listar(); }

    // El número no puede venir vacío; se le quitan los espacios sobrantes.
    private String validarNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new ReglaNegocioException("El número de venta es obligatorio.");
        }
        return numero.strip();
    }
}
