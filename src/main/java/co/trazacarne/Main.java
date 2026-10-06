package co.trazacarne;

import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.repositorio.memoria.RepositorioEnMemoria;
import co.trazacarne.servicio.ServicioAbastecedores;
import co.trazacarne.servicio.ServicioClientes;
import co.trazacarne.servicio.ServicioProductos;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {
        System.out.println("TrazaCarne: clientes, abastecedores y productos en memoria");

        Repositorio<Cliente, String> repositorioClientes = new RepositorioEnMemoria<>(Cliente::getDocumento);
        Repositorio<Abastecedor, String> repositorioAbastecedores = new RepositorioEnMemoria<>(Abastecedor::getNit);
        Repositorio<Producto, String> repositorioProductos = new RepositorioEnMemoria<>(Producto::getCodigo);

        ServicioClientes clientes = new ServicioClientes(repositorioClientes);
        ServicioAbastecedores abastecedores = new ServicioAbastecedores(repositorioAbastecedores);
        ServicioProductos productos = new ServicioProductos(repositorioProductos);

        Cliente cliente = clientes.registrar("1001", "Cliente de demostración");
        abastecedores.registrar("900001", "Matadero de demostración", TipoAbastecedor.MATADERO);
        abastecedores.registrar("900002", "Proveedor de demostración", TipoAbastecedor.PROVEEDOR);

        Producto carneMolida = productos.registrar(
                "P-001", "Carne molida", "Bovino", "Molida", FormaVenta.POR_PESO, new BigDecimal("24000.00"));
        Producto hamburguesa = productos.registrar(
                "P-002", "Hamburguesa", "Bovino", "Preparado", FormaVenta.POR_UNIDAD, new BigDecimal("6500.00"));

        System.out.println("Registrados: " + clientes.listar().size() + " cliente, "
                + abastecedores.listar().size() + " abastecedores y " + productos.listar().size() + " productos.");

        mostrarSubtotal(carneMolida, new BigDecimal("1.250"));
        mostrarSubtotal(hamburguesa, new BigDecimal("3"));

        clientes.desactivar(cliente.getDocumento());
        System.out.println("Cliente conservado: " + clientes.consultar("1001").getNombre()
                + " | activo: " + clientes.consultar("1001").estaActivo());
        try {
            clientes.consultarActivo("1001");
        } catch (RegistroInactivoException error) {
            System.out.println("Validación de nueva operación: " + error.getMessage());
        }
    }

    private static void mostrarSubtotal(Producto producto, BigDecimal cantidad) {
        BigDecimal subtotal = producto.calcularSubtotal(cantidad);
        System.out.println(producto.getNombre() + " | cantidad: " + cantidad.toPlainString()
                + " | subtotal: " + subtotal.toPlainString() + " COP");
    }
}
