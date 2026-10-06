package co.trazacarne.consola;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.dominio.Venta;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.servicio.ServicioAbastecedores;
import co.trazacarne.servicio.ServicioClientes;
import co.trazacarne.servicio.ServicioCompras;
import co.trazacarne.servicio.ServicioInventario;
import co.trazacarne.servicio.ServicioProductos;
import co.trazacarne.servicio.ServicioTrazabilidad;
import co.trazacarne.servicio.ServicioVentas;
import co.trazacarne.servicio.dto.DatosCompra;
import co.trazacarne.servicio.dto.DatosLoteRecibido;
import co.trazacarne.servicio.dto.DatosVenta;
import co.trazacarne.servicio.dto.ItemVenta;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;

/** Lee entradas, llama a los servicios y presenta sus resultados. */
// La consola no tiene reglas de negocio: solo pide datos, llama al servicio y muestra el resultado.
public class MenuConsola {

    // Scanner y PrintStream se reciben por constructor para poder probar el menú con texto simulado.
    private final Scanner entrada;
    private final PrintStream salida;
    private final ServicioClientes clientes;
    private final ServicioAbastecedores abastecedores;
    private final ServicioProductos productos;
    private final ServicioCompras compras;
    private final ServicioVentas ventas;
    private final ServicioInventario inventario;
    private final ServicioTrazabilidad trazabilidad;

    public MenuConsola(Scanner entrada, PrintStream salida, ServicioClientes clientes,
                       ServicioAbastecedores abastecedores, ServicioProductos productos,
                       ServicioCompras compras, ServicioVentas ventas,
                       ServicioInventario inventario, ServicioTrazabilidad trazabilidad) {
        this.entrada = Objects.requireNonNull(entrada);
        this.salida = Objects.requireNonNull(salida);
        this.clientes = Objects.requireNonNull(clientes);
        this.abastecedores = Objects.requireNonNull(abastecedores);
        this.productos = Objects.requireNonNull(productos);
        this.compras = Objects.requireNonNull(compras);
        this.ventas = Objects.requireNonNull(ventas);
        this.inventario = Objects.requireNonNull(inventario);
        this.trazabilidad = Objects.requireNonNull(trazabilidad);
    }

    // Menú principal: se repite hasta elegir 0 o hasta que se acabe la entrada.
    public void iniciar() {
        salida.println("TrazaCarne | Inventario y trazabilidad");
        salida.println("Los datos se conservan durante esta sesión en memoria.");
        try {
            boolean continuar = true;
            while (continuar) {
                salida.println("\n1. Clientes | 2. Abastecedores | 3. Productos");
                salida.println("4. Compras | 5. Ventas | 6. Inventario | 7. Trazabilidad | 0. Salir");
                switch (leerTexto("Opción: ")) {
                    case "1" -> menuClientes();
                    case "2" -> menuAbastecedores();
                    case "3" -> menuProductos();
                    case "4" -> menuCompras();
                    case "5" -> menuVentas();
                    case "6" -> menuInventario();
                    case "7" -> menuTrazabilidad();
                    case "0" -> continuar = false;
                    default -> salida.println("Opción desconocida. Inténtalo de nuevo.");
                }
            }
        } catch (FinEntradaException fin) {
            salida.println("\nEntrada finalizada.");
        }
        salida.println("Sesión finalizada.");
    }

    // --- Submenús (todos siguen el mismo patrón: mostrar opciones, leer y ejecutar) ---

    private void menuClientes() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nCLIENTES: 1. Registrar | 2. Consultar | 3. Listar | 4. Actualizar | 5. Desactivar | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> {
                            Cliente cliente = clientes.registrar(leerTexto("Documento: "), leerTexto("Nombre: "));
                            salida.println("Cliente registrado: " + cliente.getDocumento());
                        }
                        case "2" -> mostrarCliente(clientes.consultar(leerTexto("Documento: ")));
                        case "3" -> mostrarLista(clientes.listar(), this::mostrarCliente);
                        case "4" -> {
                            clientes.actualizar(leerTexto("Documento: "), leerTexto("Nuevo nombre: "));
                            salida.println("Cliente actualizado.");
                        }
                        case "5" -> {
                            clientes.desactivar(leerTexto("Documento: "));
                            salida.println("Cliente desactivado; su historial se conserva.");
                        }
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    private void menuAbastecedores() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nABASTECEDORES: 1. Registrar | 2. Consultar | 3. Listar | 4. Actualizar | 5. Desactivar | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> {
                            String nit = leerTexto("NIT: ");
                            String nombre = leerTexto("Nombre: ");
                            int tipo = leerEnteroEnRango("Tipo (1. Matadero, 2. Proveedor): ", 1, 2);
                            abastecedores.registrar(nit, nombre,
                                    tipo == 1 ? TipoAbastecedor.MATADERO : TipoAbastecedor.PROVEEDOR);
                            salida.println("Abastecedor registrado: " + nit);
                        }
                        case "2" -> mostrarAbastecedor(abastecedores.consultar(leerTexto("NIT: ")));
                        case "3" -> mostrarLista(abastecedores.listar(), this::mostrarAbastecedor);
                        case "4" -> {
                            abastecedores.actualizar(leerTexto("NIT: "), leerTexto("Nuevo nombre: "));
                            salida.println("Abastecedor actualizado.");
                        }
                        case "5" -> {
                            abastecedores.desactivar(leerTexto("NIT: "));
                            salida.println("Abastecedor desactivado; su historial se conserva.");
                        }
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    private void menuProductos() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nPRODUCTOS: 1. Registrar | 2. Consultar | 3. Listar | 4. Actualizar precio | 5. Desactivar | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> registrarProducto();
                        case "2" -> mostrarProducto(productos.consultar(leerTexto("Código: ")));
                        case "3" -> mostrarLista(productos.listar(), this::mostrarProducto);
                        case "4" -> {
                            productos.actualizarPrecio(leerTexto("Código: "), leerDecimal("Nuevo precio: "));
                            salida.println("Precio actualizado.");
                        }
                        case "5" -> {
                            productos.desactivar(leerTexto("Código: "));
                            salida.println("Producto desactivado; su historial se conserva.");
                        }
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    private void registrarProducto() {
        String codigo = leerTexto("Código: ");
        String nombre = leerTexto("Nombre: ");
        String especie = leerTexto("Especie: ");
        String corte = leerTexto("Tipo de corte: ");
        int forma = leerEnteroEnRango("Forma (1. Por peso en kg, 2. Por unidad): ", 1, 2);
        BigDecimal precio = leerDecimal("Precio por kg o unidad (sin separadores de miles): ");
        productos.registrar(codigo, nombre, especie, corte,
                forma == 1 ? FormaVenta.POR_PESO : FormaVenta.POR_UNIDAD, precio);
        salida.println("Producto registrado: " + codigo);
    }

    private void menuCompras() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nCOMPRAS: 1. Registrar | 2. Consultar | 3. Listar | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> registrarCompra();
                        case "2" -> mostrarCompra(compras.consultar(leerTexto("Número de compra: ")));
                        case "3" -> mostrarLista(compras.listar(), this::mostrarCompra);
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    // Pide los datos de la compra y de uno o varios lotes.
    private void registrarCompra() {
        String numero = leerTexto("Número de compra: ");
        String nit = leerTexto("NIT del abastecedor: ");
        Abastecedor abastecedor = abastecedores.consultarActivo(nit);
        List<DatosLoteRecibido> lotes = new ArrayList<>();
        boolean agregar = true;
        while (agregar) {
            String codigo = leerTexto("Código del lote: ");
            String producto = leerTexto("Código del producto: ");
            BigDecimal cantidad = leerDecimal("Cantidad recibida (kg o unidades): ");
            // La fecha de sacrificio solo es obligatoria si el abastecedor es matadero.
            LocalDate sacrificio = leerFecha("Fecha de sacrificio (AAAA-MM-DD"
                    + (abastecedor.esMatadero() ? "): " : "; Enter si no aplica): "), !abastecedor.esMatadero());
            LocalDate procesamiento = leerFecha("Fecha de procesamiento (AAAA-MM-DD): ", false);
            LocalDate vencimiento = leerFecha("Fecha de vencimiento (AAAA-MM-DD): ", false);
            lotes.add(new DatosLoteRecibido(codigo, producto, cantidad, sacrificio, procesamiento, vencimiento));
            agregar = leerSiNo("¿Agregar otro lote? (s/n): ");
        }
        Compra compra = compras.registrarCompra(new DatosCompra(numero, nit, lotes));
        salida.println("Compra registrada: " + compra.getNumero());
        mostrarCompra(compra);
    }

    private void menuVentas() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nVENTAS: 1. Registrar | 2. Consultar | 3. Listar | 4. Anular | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> registrarVenta();
                        case "2" -> mostrarVenta(ventas.consultar(leerTexto("Número de venta: ")));
                        case "3" -> mostrarLista(ventas.listar(), this::mostrarVenta);
                        case "4" -> {
                            Venta venta = ventas.anularVenta(leerTexto("Número de venta: "));
                            salida.println("Venta anulada: " + venta.getNumero());
                        }
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    // Pide el cliente y uno o varios productos; los lotes los elige el sistema.
    private void registrarVenta() {
        String numero = leerTexto("Número de venta: ");
        String documento = leerTexto("Documento del cliente: ");
        List<ItemVenta> items = new ArrayList<>();
        boolean agregar = true;
        while (agregar) {
            items.add(new ItemVenta(leerTexto("Código del producto: "), leerDecimal("Cantidad (kg o unidades): ")));
            agregar = leerSiNo("¿Agregar otro producto? (s/n): ");
        }
        Venta venta = ventas.registrarVenta(new DatosVenta(numero, documento, items));
        salida.println("Venta registrada: " + venta.getNumero());
        mostrarVenta(venta);
    }

    private void menuInventario() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nINVENTARIO: 1. Existencias por producto | 2. Todos los lotes | 3. Consultar lote");
            salida.println("4. Próximos a vencer | 5. Registrar pérdida | 6. Movimientos de un lote | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> {
                            String codigo = leerTexto("Código del producto: ");
                            salida.println("Disponible para venta: " + inventario.disponible(codigo).toPlainString());
                            mostrarLista(inventario.existencias(codigo), this::mostrarLote);
                        }
                        case "2" -> mostrarLista(inventario.lotes(), this::mostrarLote);
                        case "3" -> mostrarLote(inventario.lote(leerTexto("Código del lote: ")));
                        case "4" -> mostrarLista(inventario.proximosAVencer(
                                leerEnteroEnRango("Días (0 a 36500): ", 0, 36500)), this::mostrarLote);
                        case "5" -> {
                            inventario.registrarPerdida(leerTexto("Código del lote: "),
                                    leerDecimal("Cantidad perdida: "), leerTexto("Motivo: "));
                            salida.println("Pérdida registrada.");
                        }
                        case "6" -> mostrarLista(inventario.movimientos(leerTexto("Código del lote: ")),
                                movimiento -> salida.println(movimiento.getId() + " | " + movimiento.getFecha()
                                        + " | " + movimiento.getTipo() + " | cantidad: " + movimiento.getCantidad().toPlainString()
                                        + " | referencia: " + movimiento.getReferencia() + " | motivo: " + movimiento.getMotivo()));
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    private void menuTrazabilidad() {
        boolean continuar = true;
        while (continuar) {
            salida.println("\nTRAZABILIDAD: 1. Origen de una venta | 2. Destino de un lote | 0. Volver");
            String opcion = leerTexto("Opción: ");
            if (opcion.equals("0")) {
                continuar = false;
            } else {
                ejecutar(() -> {
                    switch (opcion) {
                        case "1" -> mostrarLista(trazabilidad.origenDeVenta(leerTexto("Número de venta: ")),
                                origen -> salida.println("Venta " + origen.numeroVenta() + " | " + origen.estado()
                                        + " | producto: " + origen.codigoProducto() + " " + origen.nombreProducto()
                                        + " | lote: " + origen.codigoLote() + " | cantidad: " + origen.cantidad().toPlainString()
                                        + " | abastecedor: " + origen.nitAbastecedor() + " " + origen.nombreAbastecedor()
                                        + " | sacrificio: " + origen.fechaSacrificio() + " | procesamiento: " + origen.fechaProcesamiento()
                                        + " | vencimiento: " + origen.fechaVencimiento()));
                        case "2" -> {
                            String lote = leerTexto("Código del lote: ");
                            boolean incluir = leerSiNo("¿Incluir ventas anuladas? (s/n): ");
                            mostrarLista(trazabilidad.ventasDelLote(lote, incluir),
                                    destino -> salida.println("Venta " + destino.numeroVenta() + " | " + destino.fecha()
                                            + " | " + destino.estado() + " | cliente: " + destino.documentoCliente()
                                            + " " + destino.nombreCliente() + " | cantidad: " + destino.cantidad().toPlainString()));
                        }
                        default -> salida.println("Opción desconocida.");
                    }
                });
            }
        }
    }

    /**
     * Ejecuta una opción del menú y atrapa los errores de negocio.
     *
     * <p>Cada submenú le pasa su código como lambda: {@code ejecutar(() -> { switch (opcion) {...} })}.
     * {@code Runnable} es una interfaz con un solo método, {@code run()}, que aquí ejecuta esa lambda.
     *
     * <p>Así el {@code try/catch} se escribe una sola vez y no en cada opción. Si, por ejemplo, se registra
     * un cliente con documento repetido, se muestra "No se pudo completar la operación: Ya existe..."
     * y el usuario vuelve al menú sin que el programa se cierre. Como todas las excepciones del sistema
     * heredan de {@link ReglaNegocioException}, un solo {@code catch} las cubre todas.
     */
    private void ejecutar(Runnable operacion) {
        try {
            operacion.run();
        } catch (ReglaNegocioException error) {
            salida.println("No se pudo completar la operación: " + error.getMessage());
        }
    }

    // --- Lectura de datos (repiten la pregunta hasta recibir un valor válido) ---

    private String leerTexto(String pregunta) {
        salida.print(pregunta);
        salida.flush();
        if (!entrada.hasNextLine()) {
            throw new FinEntradaException();
        }
        return entrada.nextLine().strip();
    }

    private BigDecimal leerDecimal(String pregunta) {
        while (true) {
            try {
                // Acepta coma o punto como separador decimal.
                return new BigDecimal(leerTexto(pregunta).replace(',', '.'));
            } catch (NumberFormatException error) {
                salida.println("Escribe un número válido, por ejemplo 1.250; sin separadores de miles.");
            }
        }
    }

    private int leerEnteroEnRango(String pregunta, int minimo, int maximo) {
        while (true) {
            try {
                int valor = Integer.parseInt(leerTexto(pregunta));
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
            } catch (NumberFormatException error) {
                // Un texto o decimal no representa una opción entera.
            }
            salida.println("Escribe un entero entre " + minimo + " y " + maximo + ".");
        }
    }

    private LocalDate leerFecha(String pregunta, boolean opcional) {
        while (true) {
            String texto = leerTexto(pregunta);
            if (opcional && texto.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException error) {
                salida.println("Escribe una fecha válida con formato AAAA-MM-DD, por ejemplo 2026-10-05.");
            }
        }
    }

    private boolean leerSiNo(String pregunta) {
        while (true) {
            String respuesta = leerTexto(pregunta);
            if (respuesta.equalsIgnoreCase("s")) {
                return true;
            }
            if (respuesta.equalsIgnoreCase("n")) {
                return false;
            }
            salida.println("Responde s o n.");
        }
    }

    // --- Presentación de resultados ---

    /**
     * Muestra cualquier lista, o "No hay registros." si está vacía.
     *
     * <p>{@code <T>} lo hace genérico: sirve para listas de clientes, lotes, ventas, etc.
     * {@code Consumer<T>} es "algo que recibe un T y hace algo con él"; aquí, imprimirlo.
     * Ejemplo: {@code mostrarLista(clientes.listar(), this::mostrarCliente)} imprime cada cliente
     * con el método {@code mostrarCliente}.
     */
    private <T> void mostrarLista(List<T> registros, Consumer<T> mostrar) {
        if (registros.isEmpty()) {
            salida.println("No hay registros.");
        }
        for (T registro : registros) {
            mostrar.accept(registro);
        }
    }

    private void mostrarCliente(Cliente cliente) {
        salida.println(cliente.getDocumento() + " | " + cliente.getNombre() + " | activo: " + cliente.estaActivo());
    }

    private void mostrarAbastecedor(Abastecedor abastecedor) {
        salida.println(abastecedor.getNit() + " | " + abastecedor.getNombre()
                + " | " + abastecedor.getTipo() + " | activo: " + abastecedor.estaActivo());
    }

    private void mostrarProducto(Producto producto) {
        salida.println(producto.getCodigo() + " | " + producto.getNombre() + " | especie: " + producto.getEspecie()
                + " | corte: " + producto.getTipoCorte() + " | precio: " + producto.getPrecio().toPlainString()
                + " COP | activo: " + producto.estaActivo());
    }

    private void mostrarCompra(Compra compra) {
        salida.println("Compra " + compra.getNumero() + " | " + compra.getFecha()
                + " | abastecedor: " + compra.getAbastecedor().getNit() + " " + compra.getAbastecedor().getNombre());
        mostrarLista(compra.getLotes(), this::mostrarLote);
    }

    private void mostrarLote(Lote lote) {
        salida.println("Lote " + lote.getCodigo() + " | producto: " + lote.getProducto().getCodigo()
                + " | recibido: " + lote.getCantidadRecibida().toPlainString()
                + " | restante: " + lote.getCantidadDisponible().toPlainString()
                + " | sacrificio: " + lote.getFechaSacrificio() + " | procesamiento: " + lote.getFechaProcesamiento()
                + " | vencimiento: " + lote.getFechaVencimiento());
    }

    private void mostrarVenta(Venta venta) {
        salida.println("Venta " + venta.getNumero() + " | " + venta.getFecha()
                + " | cliente: " + venta.getCliente().getDocumento() + " " + venta.getCliente().getNombre()
                + " | " + venta.getEstado() + " | total: " + venta.calcularTotal().toPlainString() + " COP");
        mostrarLista(trazabilidad.origenDeVenta(venta.getNumero()),
                origen -> salida.println("  Producto " + origen.codigoProducto() + " | lote " + origen.codigoLote()
                        + " | cantidad: " + origen.cantidad().toPlainString()));
    }

    /**
     * Señala únicamente que se agotó la entrada; no representa un error del negocio.
     *
     * <p>Pasa cuando no hay más líneas que leer (Ctrl+Z / Ctrl+D, o el texto simulado de las pruebas se acabó).
     * {@code leerTexto} la lanza y {@code iniciar()} la atrapa para cerrar la sesión de forma ordenada,
     * sin importar en qué submenú se estaba. No hereda de ReglaNegocioException a propósito, para que
     * {@code ejecutar} no la atrape como si fuera un error normal.
     */
    private static final class FinEntradaException extends RuntimeException {
    }
}
