package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Producto;
import co.trazacarne.excepcion.CantidadInvalidaException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.Repositorio;

import java.time.LocalDate;
import java.util.List;

/** Caso de uso de HU-04: una compra se registra completa o no se registra. */
public class ServicioCompras {

    private final Repositorio<Compra, String> compras;
    private final Repositorio<Abastecedor, String> abastecedores;
    private final Repositorio<Producto, String> productos;
    private final Inventario inventario;

    public ServicioCompras(Repositorio<Compra, String> compras, Repositorio<Abastecedor, String> abastecedores,
                           Repositorio<Producto, String> productos, Inventario inventario) {
        this.compras = compras;
        this.abastecedores = abastecedores;
        this.productos = productos;
        this.inventario = inventario;
    }

    public Compra registrarCompra(String numero, String nit, LocalDate fecha, List<LoteRecibido> lotesRecibidos) {
        if (numero != null && compras.existe(numero.strip())) {
            throw new IdentificadorDuplicadoException("La compra " + numero.strip() + " ya fue registrada.");
        }
        if (lotesRecibidos == null || lotesRecibidos.isEmpty()) {
            throw new CantidadInvalidaException("La compra debe tener al menos un lote.");   // RN-05
        }
        Abastecedor abastecedor = abastecedores.buscarPorId(nit)
                .orElseThrow(() -> new ReglaNegocioException("No existe el abastecedor con NIT " + nit + "."));

        // 1. Armar la compra completa en memoria: si un lote falla, nada llegó al inventario.
        Compra compra = new Compra(numero, fecha, abastecedor);
        for (LoteRecibido recibido : lotesRecibidos) {
            Producto producto = productos.buscarPorId(recibido.codigoProducto())
                    .orElseThrow(() -> new ReglaNegocioException(
                            "No existe el producto con código " + recibido.codigoProducto() + "."));
            compra.agregarLote(recibido.codigoLote(), producto, recibido.cantidad(),
                    recibido.fechaSacrificio(), recibido.fechaProcesamiento(), recibido.fechaVencimiento());
        }

        // 2. Aplicar: el inventario revisa los códigos contra lotes anteriores antes de agregar.
        inventario.registrarEntrada(compra);
        compras.guardar(compra);
        return compra;
    }
}
