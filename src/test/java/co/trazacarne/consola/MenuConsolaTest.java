package co.trazacarne.consola;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.EstadoVenta;
import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.dominio.Venta;
import co.trazacarne.dominio.estrategia.EstrategiaFEFO;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import co.trazacarne.servicio.ServicioAbastecedores;
import co.trazacarne.servicio.ServicioClientes;
import co.trazacarne.servicio.ServicioCompras;
import co.trazacarne.servicio.ServicioInventario;
import co.trazacarne.servicio.ServicioProductos;
import co.trazacarne.servicio.ServicioTrazabilidad;
import co.trazacarne.servicio.ServicioVentas;
import co.trazacarne.servicio.dto.DatosCompra;
import co.trazacarne.servicio.dto.DatosLoteRecibido;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MenuConsolaTest {

    private final Repositorio<Cliente, String> repositorioClientes = new RepositorioEnMemoria<>(Cliente::getDocumento);
    private final Repositorio<Abastecedor, String> repositorioAbastecedores = new RepositorioEnMemoria<>(Abastecedor::getNit);
    private final Repositorio<Producto, String> repositorioProductos = new RepositorioEnMemoria<>(Producto::getCodigo);
    private final Repositorio<Compra, String> repositorioCompras = new RepositorioEnMemoria<>(Compra::getNumero);
    private final Repositorio<Venta, String> repositorioVentas = new RepositorioEnMemoria<>(Venta::getNumero);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final ServicioClientes clientes = new ServicioClientes(repositorioClientes);
    private final ServicioAbastecedores abastecedores = new ServicioAbastecedores(repositorioAbastecedores);
    private final ServicioProductos productos = new ServicioProductos(repositorioProductos);
    private final Inventario inventario = new Inventario(new EstrategiaFEFO());
    private final ServicioCompras compras = new ServicioCompras(repositorioCompras, abastecedores, productos, inventario, reloj);
    private final ServicioVentas ventas = new ServicioVentas(repositorioVentas, clientes, productos, inventario, reloj);
    private final ServicioInventario consultaInventario = new ServicioInventario(inventario, productos, reloj);
    private final ServicioTrazabilidad trazabilidad = new ServicioTrazabilidad(repositorioVentas, inventario);

    @Test
    void flujoCompletoPorConsolaConservaTrazabilidadYMovimientos() {
        String salida = ejecutar("""
                1
                1
                1001
                Ana
                0
                2
                1
                9001
                Matadero Central
                1
                0
                3
                1
                P-001
                Carne molida
                Bovino
                Molida
                1
                24000.00
                0
                4
                1
                C-001
                9001
                L-001
                P-001
                5
                2026-10-01
                2026-10-02
                2026-10-10
                s
                L-002
                P-001
                20
                2026-10-01
                2026-10-02
                2026-10-15
                n
                0
                5
                1
                V-001
                1001
                P-001
                8
                n
                0
                7
                1
                V-001
                2
                L-001
                s
                0
                6
                1
                P-001
                6
                L-001
                0
                5
                4
                V-001
                0
                6
                5
                L-002
                2
                Daño del empaque
                6
                L-002
                4
                10
                0
                0
                """);

        assertAll(
                () -> assertEquals(EstadoVenta.ANULADA, ventas.consultar("V-001").getEstado()),
                () -> assertEquals(0, new BigDecimal("5").compareTo(consultaInventario.lote("L-001").getCantidadDisponible())),
                () -> assertEquals(0, new BigDecimal("18").compareTo(consultaInventario.lote("L-002").getCantidadDisponible())),
                () -> assertEquals(0, new BigDecimal("23").compareTo(consultaInventario.disponible("P-001"))),
                () -> assertEquals(3, consultaInventario.movimientos("L-001").size()),
                () -> assertEquals(4, consultaInventario.movimientos("L-002").size()),
                () -> assertEquals("1001", trazabilidad.ventasDelLote("L-001", true).getFirst().documentoCliente()),
                () -> assertTrue(salida.contains("total: 192000.00 COP")),
                () -> assertTrue(salida.contains("lote L-001 | cantidad: 5")),
                () -> assertTrue(salida.contains("lote L-002 | cantidad: 3")),
                () -> assertTrue(salida.contains("abastecedor: 9001 Matadero Central")),
                () -> assertTrue(salida.contains("Venta anulada: V-001")),
                () -> assertTrue(salida.contains("Pérdida registrada.")),
                () -> assertFalse(salida.contains("No se pudo completar")),
                () -> assertTrue(salida.endsWith("Sesión finalizada." + System.lineSeparator()))
        );
    }

    @Test
    void formatosInvalidosSePidenDeNuevoYFinDeEntradaCierraLaSesion() {
        String salida = ejecutar("""
                2
                1
                9001
                Proveedor
                texto
                3
                2
                0
                3
                1
                P-001
                Carne molida
                Bovino
                Molida
                texto
                0
                1
                dinero
                24000,00
                0
                4
                1
                C-001
                9001
                L-001
                P-001
                kilos
                5

                2026-02-30
                2026-10-02
                ayer
                2026-10-10
                tal vez
                n
                """);

        assertAll(
                () -> assertEquals(1, compras.listar().size()),
                () -> assertEquals(0, new BigDecimal("5").compareTo(consultaInventario.disponible("P-001"))),
                () -> assertTrue(salida.contains("Escribe un entero entre 1 y 2.")),
                () -> assertTrue(salida.contains("Escribe un número válido")),
                () -> assertTrue(salida.contains("Escribe una fecha válida")),
                () -> assertTrue(salida.contains("Responde s o n.")),
                () -> assertTrue(salida.contains("Entrada finalizada."))
        );
    }

    @Test
    void entradaAgotadaMientrasSeLlenaUnaCompraNoRegistraDatosParciales() {
        registrarCatalogo();

        String salida = ejecutar("4\n1\nC-001\n9001\nL-001\nP-001\n5\n2026-10-01\n");

        assertAll(
                () -> assertTrue(compras.listar().isEmpty()),
                () -> assertTrue(consultaInventario.lotes().isEmpty()),
                () -> assertTrue(salida.contains("Entrada finalizada."))
        );
    }

    @Test
    void rechazoDeNegocioPermiteOtraOperacionSinConsumirInventario() {
        registrarCatalogo();
        compras.registrarCompra(new DatosCompra("C-001", "9001", List.of(
                new DatosLoteRecibido("L-001", "P-001", new BigDecimal("10"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 10)))));

        String salida = ejecutar("""
                5
                1
                V-RECHAZADA
                1001
                P-001
                12
                n
                1
                V-001
                1001
                P-001
                8
                n
                0
                0
                """);

        assertAll(
                () -> assertEquals(1, ventas.listar().size()),
                () -> assertEquals("V-001", ventas.listar().getFirst().getNumero()),
                () -> assertEquals(0, new BigDecimal("2").compareTo(consultaInventario.disponible("P-001"))),
                () -> assertTrue(salida.contains("No se pudo completar la operación:")),
                () -> assertTrue(salida.contains("Venta registrada: V-001"))
        );
    }

    @Test
    void opcionesDeActualizarConsultarListarYDesactivarConservanLosCatalogos() {
        registrarCatalogo();

        String salida = ejecutar("""
                1
                4
                1001
                Ana María
                2
                1001
                3
                5
                1001
                0
                2
                4
                9001
                Matadero Norte
                2
                9001
                3
                5
                9001
                0
                3
                4
                P-001
                25000
                2
                P-001
                3
                5
                P-001
                2
                P-001
                0
                0
                """);

        assertAll(
                () -> assertEquals("Ana María", clientes.consultar("1001").getNombre()),
                () -> assertFalse(clientes.consultar("1001").estaActivo()),
                () -> assertEquals("Matadero Norte", abastecedores.consultar("9001").getNombre()),
                () -> assertFalse(abastecedores.consultar("9001").estaActivo()),
                () -> assertEquals(new BigDecimal("25000.00"), productos.consultar("P-001").getPrecio()),
                () -> assertFalse(productos.consultar("P-001").estaActivo()),
                () -> assertEquals(1, clientes.listar().size()),
                () -> assertEquals(1, abastecedores.listar().size()),
                () -> assertEquals(1, productos.listar().size()),
                () -> assertTrue(salida.contains("Cliente actualizado.")),
                () -> assertTrue(salida.contains("Abastecedor actualizado.")),
                () -> assertTrue(salida.contains("Precio actualizado.")),
                () -> assertFalse(salida.contains("No se pudo completar"))
        );
    }

    private void registrarCatalogo() {
        clientes.registrar("1001", "Ana");
        abastecedores.registrar("9001", "Matadero Central", TipoAbastecedor.MATADERO);
        productos.registrar("P-001", "Carne molida", "Bovino", "Molida",
                FormaVenta.POR_PESO, new BigDecimal("24000.00"));
    }

    private String ejecutar(String entradas) {
        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        MenuConsola menu = new MenuConsola(new Scanner(entradas),
                new PrintStream(resultado, true, StandardCharsets.UTF_8), clientes, abastecedores,
                productos, compras, ventas, consultaInventario, trazabilidad);

        assertDoesNotThrow(menu::iniciar);

        return resultado.toString(StandardCharsets.UTF_8);
    }
}
