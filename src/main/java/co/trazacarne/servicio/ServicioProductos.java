package co.trazacarne.servicio;

import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.ProductoPorUnidad;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Casos de uso de productos: registrar, consultar, listar, cambiar precio y desactivar. */
public class ServicioProductos {

    // Depende de la interfaz, no de la implementación en memoria.
    private final Repositorio<Producto, String> repositorio;

    public ServicioProductos(Repositorio<Producto, String> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio de productos es obligatorio.");
    }

    public Producto registrar(String codigo, String nombre, String especie, String tipoCorte,
                              FormaVenta forma, BigDecimal precio) {
        if (forma == null) {
            throw new ReglaNegocioException("La forma de venta es obligatoria.");
        }
        // Según la forma de venta se crea la subclase correcta (herencia + polimorfismo).
        Producto producto = switch (forma) {
            case POR_PESO -> new ProductoPorPeso(codigo, nombre, especie, tipoCorte, precio);
            case POR_UNIDAD -> new ProductoPorUnidad(codigo, nombre, especie, tipoCorte, precio);
        };
        if (repositorio.existe(producto.getCodigo())) {
            throw new IdentificadorDuplicadoException(
                    "Ya existe un producto con el código " + producto.getCodigo() + ".");
        }
        repositorio.guardar(producto);
        return producto;
    }

    public Producto consultar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("El código del producto es obligatorio.");
        }
        String identificador = codigo.strip();
        return repositorio.buscarPorId(identificador).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe un producto con el código " + identificador + "."));
    }

    // Lo usan compras y ventas: además de existir, el producto debe estar activo.
    public Producto consultarActivo(String codigo) {
        Producto producto = consultar(codigo);
        if (!producto.estaActivo()) {
            throw new RegistroInactivoException("El producto " + producto.getCodigo() + " está inactivo.");
        }
        return producto;
    }

    public List<Producto> listar() {
        return repositorio.listar();
    }

    public void actualizarPrecio(String codigo, BigDecimal precio) {
        Producto producto = consultar(codigo);
        producto.actualizarPrecio(precio);
        repositorio.actualizar(producto);
    }

    public void desactivar(String codigo) {
        Producto producto = consultar(codigo);
        producto.desactivar();
        repositorio.actualizar(producto);
    }
}
