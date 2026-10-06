package co.trazacarne;

import co.trazacarne.consola.MenuConsola;
import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.Compra;
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
import co.trazacarne.servicio.dto.DatosVenta;
import co.trazacarne.servicio.dto.ItemVenta;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;

/** Crea las dependencias y arranca la consola o la demostración opcional. */
public class Main {

    public static void main(String[] args) {
        // Con el argumento --demo se ejecuta un ejemplo automático en vez del menú.
        boolean demostracion = args.length == 1 && args[0].equals("--demo");
        ZoneId zona = ZoneId.of("America/Bogota");
        // En la demo la fecha es fija para que el resultado siempre sea el mismo.
        Clock reloj = demostracion
                ? Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), zona)
                : Clock.system(zona);

        // 1. Repositorios: guardan los datos en memoria. Cada uno sabe cuál es el identificador de su registro.
        Repositorio<Cliente, String> repositorioClientes = new RepositorioEnMemoria<>(Cliente::getDocumento);
        Repositorio<Abastecedor, String> repositorioAbastecedores = new RepositorioEnMemoria<>(Abastecedor::getNit);
        Repositorio<Producto, String> repositorioProductos = new RepositorioEnMemoria<>(Producto::getCodigo);
        Repositorio<Compra, String> repositorioCompras = new RepositorioEnMemoria<>(Compra::getNumero);
        Repositorio<Venta, String> repositorioVentas = new RepositorioEnMemoria<>(Venta::getNumero);

        // 2. Servicios: reciben sus dependencias por el constructor (inyección de dependencias).
        ServicioClientes clientes = new ServicioClientes(repositorioClientes);
        ServicioAbastecedores abastecedores = new ServicioAbastecedores(repositorioAbastecedores);
        ServicioProductos productos = new ServicioProductos(repositorioProductos);
        // El inventario usa FEFO: primero sale el lote que vence antes.
        Inventario inventario = new Inventario(new EstrategiaFEFO());
        ServicioCompras compras = new ServicioCompras(repositorioCompras, abastecedores, productos, inventario, reloj);
        ServicioVentas ventas = new ServicioVentas(repositorioVentas, clientes, productos, inventario, reloj);
        ServicioInventario consultasInventario = new ServicioInventario(inventario, productos, reloj);
        ServicioTrazabilidad trazabilidad = new ServicioTrazabilidad(repositorioVentas, inventario);

        // 3. Arranque: demostración o menú interactivo.
        if (demostracion) {
            ejecutarDemostracion(clientes, abastecedores, productos, compras, ventas, consultasInventario, trazabilidad);
        } else {
            MenuConsola menu = new MenuConsola(new Scanner(System.in), System.out, clientes,
                    abastecedores, productos, compras, ventas, consultasInventario, trazabilidad);
            menu.iniciar();
        }
    }

    // Recorre el flujo completo: registrar, comprar, vender, ver trazabilidad y anular.
    private static void ejecutarDemostracion(ServicioClientes clientes, ServicioAbastecedores abastecedores,
                                            ServicioProductos productos, ServicioCompras compras,
                                            ServicioVentas ventas, ServicioInventario inventario,
                                            ServicioTrazabilidad trazabilidad) {
        System.out.println("TrazaCarne | Demostración con fecha fija 2026-10-05");
        // Datos base: un cliente, un matadero y un producto por peso.
        clientes.registrar("1001", "Ana");
        abastecedores.registrar("9001", "Matadero Central", TipoAbastecedor.MATADERO);
        productos.registrar("P-001", "Carne molida", "Bovino", "Molida",
                FormaVenta.POR_PESO, new BigDecimal("24000.00"));
        // Compra con dos lotes: L-001 (5 kg, vence antes) y L-002 (20 kg).
        compras.registrarCompra(new DatosCompra("C-001", "9001", List.of(
                new DatosLoteRecibido("L-001", "P-001", new BigDecimal("5"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 10)),
                new DatosLoteRecibido("L-002", "P-001", new BigDecimal("20"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 15)))));
        // Venta de 8 kg: por FEFO toma los 5 kg de L-001 y 3 kg de L-002.
        Venta venta = ventas.registrarVenta(new DatosVenta("V-001", "1001",
                List.of(new ItemVenta("P-001", new BigDecimal("8")))));
        for (var origen : trazabilidad.origenDeVenta(venta.getNumero())) {
            System.out.println("Venta " + origen.numeroVenta() + " | lote " + origen.codigoLote()
                    + " | cantidad: " + origen.cantidad().toPlainString() + " kg");
        }
        System.out.println("Total de venta: " + venta.calcularTotal().toPlainString() + " COP");
        System.out.println("Disponible después de vender: " + inventario.disponible("P-001").toPlainString() + " kg");
        // Al anular, las cantidades vuelven a sus lotes de origen.
        ventas.anularVenta("V-001");
        System.out.println("Estado de venta: " + venta.getEstado());
        System.out.println("L-001 restaurado: " + inventario.lote("L-001").getCantidadDisponible().toPlainString() + " kg");
        System.out.println("L-002 restaurado: " + inventario.lote("L-002").getCantidadDisponible().toPlainString() + " kg");
        System.out.println("Disponible después de anular: " + inventario.disponible("P-001").toPlainString() + " kg");
        System.out.println("Demostración completada.");
    }
}
