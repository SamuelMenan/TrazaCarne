package co.trazacarne.servicio;

import co.trazacarne.dominio.Producto;
import co.trazacarne.dominio.ProductoPorPeso;
import co.trazacarne.dominio.ProductoPorUnidad;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.Repositorio;

import java.math.BigDecimal;

/** Casos de uso de HU-03. */
public class ServicioProductos {

    public static final String FORMA_PESO = "PESO";
    public static final String FORMA_UNIDAD = "UNIDAD";

    private final Repositorio<Producto, String> productos;

    public ServicioProductos(Repositorio<Producto, String> productos) {
        this.productos = productos;
    }

    /** forma: "PESO" o "UNIDAD" (en el modelo de datos, formaVenta es el discriminador de las subclases). */
    public Producto registrar(String codigo, String nombre, String especie, String tipoCorte,
                              String forma, BigDecimal precio) {
        Producto producto = crear(codigo, nombre, especie, tipoCorte, forma, precio);
        if (productos.existe(producto.getCodigo())) {
            throw new IdentificadorDuplicadoException("Ya existe un producto con código " + producto.getCodigo() + ".");
        }
        productos.guardar(producto);
        return producto;
    }

    public Producto consultar(String codigo) {
        return productos.buscarPorId(codigo)
                .orElseThrow(() -> new ReglaNegocioException("No existe el producto con código " + codigo + "."));
    }

    public void actualizarPrecio(String codigo, BigDecimal precio) {
        Producto producto = consultar(codigo);
        producto.actualizarPrecio(precio);
        productos.guardar(producto);
    }

    /** RN-02: se desactiva, nunca se borra. */
    public void desactivar(String codigo) {
        Producto producto = consultar(codigo);
        producto.desactivar();
        productos.guardar(producto);
    }

    private static Producto crear(String codigo, String nombre, String especie, String tipoCorte,
                                  String forma, BigDecimal precio) {
        if (FORMA_PESO.equalsIgnoreCase(forma)) {
            return new ProductoPorPeso(codigo, nombre, especie, tipoCorte, precio);
        }
        if (FORMA_UNIDAD.equalsIgnoreCase(forma)) {
            return new ProductoPorUnidad(codigo, nombre, especie, tipoCorte, precio);
        }
        throw new ReglaNegocioException("La forma de venta debe ser PESO o UNIDAD.");
    }
}
