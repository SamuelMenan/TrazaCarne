package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.Compra;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Producto;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.servicio.dto.DatosCompra;
import co.trazacarne.servicio.dto.DatosLoteRecibido;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Registra compras: crea los lotes recibidos y los pasa al inventario. */
public class ServicioCompras {
    private final Repositorio<Compra, String> repositorio;
    private final ServicioAbastecedores abastecedores;
    private final ServicioProductos productos;
    private final Inventario inventario;
    // El reloj se recibe desde afuera para poder fijar la fecha en pruebas y en la demo.
    private final Clock reloj;

    public ServicioCompras(Repositorio<Compra, String> repositorio, ServicioAbastecedores abastecedores,
                           ServicioProductos productos, Inventario inventario, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.abastecedores = Objects.requireNonNull(abastecedores);
        this.productos = Objects.requireNonNull(productos);
        this.inventario = Objects.requireNonNull(inventario);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Compra registrarCompra(DatosCompra datos) {
        if (datos == null || datos.lotes() == null || datos.lotes().isEmpty()) {
            throw new ReglaNegocioException("La compra debe contener al menos un lote.");
        }
        String numero = validarNumero(datos.numero());
        if (repositorio.existe(numero)) {
            throw new IdentificadorDuplicadoException("Ya existe la compra " + numero + ".");
        }
        Abastecedor origen = abastecedores.consultarActivo(datos.nitAbastecedor());
        // La compra queda con la fecha de hoy; luego se agrega cada lote recibido.
        LocalDateTime instante = LocalDateTime.now(reloj);
        Compra compra = new Compra(numero, instante.toLocalDate(), origen);
        for (DatosLoteRecibido recibido : datos.lotes()) {
            Producto producto = productos.consultarActivo(recibido.codigoProducto());
            compra.agregarLote(recibido.codigoLote(), producto, recibido.cantidad(), recibido.fechaSacrificio(),
                    recibido.fechaProcesamiento(), recibido.fechaVencimiento());
        }
        // La compra completa se valida en el inventario antes de registrar entradas.
        compra.registrar(inventario, instante);
        repositorio.guardar(compra);
        return compra;
    }

    public Compra consultar(String numero) {
        String id = validarNumero(numero);
        return repositorio.buscarPorId(id).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe la compra " + id + "."));
    }

    public List<Compra> listar() { return repositorio.listar(); }

    // El número no puede venir vacío; se le quitan los espacios sobrantes.
    private String validarNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new ReglaNegocioException("El número de compra es obligatorio.");
        }
        return numero.strip();
    }
}
