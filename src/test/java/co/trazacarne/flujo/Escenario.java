package co.trazacarne.flujo;

import co.trazacarne.dominio.*;
import co.trazacarne.dominio.estrategia.EstrategiaFEFO;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import co.trazacarne.servicio.*;
import co.trazacarne.servicio.dto.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Datos pequeños y reloj fijo: cada prueba recibe un sistema nuevo e independiente. */
final class Escenario {
    static final LocalDate HOY = LocalDate.of(2026, 10, 5);
    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    static final Clock RELOJ = reloj(HOY);

    final Repositorio<Abastecedor, String> abastecedoresRepo = new RepositorioEnMemoria<>(Abastecedor::getNit);
    final Repositorio<Cliente, String> clientesRepo = new RepositorioEnMemoria<>(Cliente::getDocumento);
    final Repositorio<Producto, String> productosRepo = new RepositorioEnMemoria<>(Producto::getCodigo);
    final Repositorio<Compra, String> comprasRepo = new RepositorioEnMemoria<>(Compra::getNumero);
    final Repositorio<Venta, String> ventasRepo = new RepositorioEnMemoria<>(Venta::getNumero);
    final ServicioAbastecedores abastecedores = new ServicioAbastecedores(abastecedoresRepo);
    final ServicioClientes clientes = new ServicioClientes(clientesRepo);
    final ServicioProductos productos = new ServicioProductos(productosRepo);
    final Inventario inventario = new Inventario(new EstrategiaFEFO());
    final ServicioCompras compras = new ServicioCompras(comprasRepo, abastecedores, productos, inventario, RELOJ);
    final ServicioVentas ventas = servicioVentas(RELOJ);
    final ServicioInventario consultas = servicioInventario(RELOJ);
    final ServicioTrazabilidad trazabilidad = new ServicioTrazabilidad(ventasRepo, inventario);
    final Producto carne;
    final Producto unidades;

    Escenario() {
        abastecedores.registrar("9001", "Proveedor Central", TipoAbastecedor.PROVEEDOR);
        abastecedores.registrar("9002", "Matadero Central", TipoAbastecedor.MATADERO);
        clientes.registrar("1001", "Ana María");
        carne = productos.registrar("P-001", "Carne molida", "Bovino", "Molida",
                FormaVenta.POR_PESO, decimal("24000"));
        unidades = productos.registrar("P-002", "Hamburguesa", "Bovino", "Preparado",
                FormaVenta.POR_UNIDAD, decimal("6500"));
    }

    DatosLoteRecibido lote(String codigo, String producto, String cantidad, int diasVencimiento) {
        return new DatosLoteRecibido(codigo, producto, decimal(cantidad), null,
                HOY.minusDays(1), HOY.plusDays(diasVencimiento));
    }

    Compra comprar(String numero, DatosLoteRecibido... lotes) {
        return compras.registrarCompra(new DatosCompra(numero, "9001", List.of(lotes)));
    }

    Venta vender(String numero, ItemVenta... items) {
        return ventas.registrarVenta(new DatosVenta(numero, "1001", List.of(items)));
    }

    ItemVenta item(String producto, String cantidad) {
        return new ItemVenta(producto, decimal(cantidad));
    }

    ServicioVentas servicioVentas(Clock reloj) {
        return new ServicioVentas(ventasRepo, clientes, productos, inventario, reloj);
    }

    ServicioInventario servicioInventario(Clock reloj) {
        return new ServicioInventario(inventario, productos, reloj);
    }

    static Clock reloj(LocalDate fecha) {
        return Clock.fixed(fecha.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
    }

    static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }

    static BigDecimal saldo(List<Lote> lotes) {
        return lotes.stream().map(Lote::getCantidadDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static void cantidad(String esperada, BigDecimal actual) {
        assertEquals(0, decimal(esperada).compareTo(actual),
                () -> "Se esperaba " + esperada + " y se obtuvo " + actual);
    }
}
