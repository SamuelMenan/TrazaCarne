package co.trazacarne.servicio;

import co.trazacarne.dominio.FormaVenta;
import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.ProductoPorUnidad;
import co.trazacarne.dominio.ValidacionDatos;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ServicioProductos {

    private final Repositorio<Producto, String> repositorio;

    public ServicioProductos(Repositorio<Producto, String> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio de productos es obligatorio.");
    }

    public Producto registrar(String codigo, String nombre, String especie, String tipoCorte,
                              FormaVenta forma, BigDecimal precio) {
        if (forma == null) {
            throw new ReglaNegocioException("La forma de venta es obligatoria.");
        }
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
        String identificador = ValidacionDatos.textoObligatorio(codigo, "El código del producto");
        return repositorio.buscarPorId(identificador).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe un producto con el código " + identificador + "."));
    }

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
