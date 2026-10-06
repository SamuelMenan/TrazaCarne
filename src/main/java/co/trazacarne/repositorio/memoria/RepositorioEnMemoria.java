package co.trazacarne.repositorio.memoria;

import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Almacena registros durante la sesión, conservando el orden de registro.
 *
 * <p><b>Genéricos:</b> {@code T} es el tipo de registro (Cliente, Producto, Venta...) e {@code ID}
 * el tipo de su identificador (String). Gracias a esto, una sola clase sirve para todos los repositorios
 * en lugar de escribir RepositorioClientes, RepositorioProductos, etc.
 *
 * <p>Uso en Main:
 * <pre>
 *   new RepositorioEnMemoria&lt;&gt;(Cliente::getDocumento)   // el id del cliente es su documento
 *   new RepositorioEnMemoria&lt;&gt;(Producto::getCodigo)     // el id del producto es su código
 * </pre>
 * El {@code Cliente::getDocumento} es una referencia a método: le dice al repositorio
 * "para saber el id de un registro, llama a este getter".
 */
public class RepositorioEnMemoria<T, ID> implements Repositorio<T, ID> {

    // LinkedHashMap: busca rápido por identificador y mantiene el orden de registro al listar.
    private final Map<ID, T> datos = new LinkedHashMap<>();
    // Función que dice cómo sacar el id de cada registro (ej. Cliente::getDocumento).
    private final Function<T, ID> obtenerIdentificador;

    public RepositorioEnMemoria(Function<T, ID> obtenerIdentificador) {
        this.obtenerIdentificador = Objects.requireNonNull(obtenerIdentificador,
                "Se requiere una función para obtener el identificador.");
    }

    @Override
    public void guardar(T registro) {
        ID identificador = identificadorDe(registro);
        if (datos.containsKey(identificador)) {
            throw new IdentificadorDuplicadoException(
                    "Ya existe un registro con el identificador " + identificador + ".");
        }
        datos.put(identificador, registro);
    }

    @Override
    public void actualizar(T registro) {
        ID identificador = identificadorDe(registro);
        if (!datos.containsKey(identificador)) {
            throw new RegistroNoEncontradoException(
                    "No existe un registro con el identificador " + identificador + ".");
        }
        datos.put(identificador, registro);
    }

    @Override
    public Optional<T> buscarPorId(ID identificador) {
        validarIdentificador(identificador);
        return Optional.ofNullable(datos.get(identificador));
    }

    // Devuelve una copia para que no se pueda modificar el almacenamiento desde afuera.
    @Override
    public List<T> listar() {
        return List.copyOf(datos.values());
    }

    @Override
    public boolean existe(ID identificador) {
        validarIdentificador(identificador);
        return datos.containsKey(identificador);
    }

    // --- Validaciones internas ---

    private ID identificadorDe(T registro) {
        if (registro == null) {
            throw new ReglaNegocioException("El registro es obligatorio.");
        }
        ID identificador = obtenerIdentificador.apply(registro);
        validarIdentificador(identificador);
        return identificador;
    }

    /**
     * Rechaza identificadores nulos o, si son texto, vacíos.
     *
     * <p>{@code identificador instanceof String texto && texto.isBlank()} es <i>pattern matching</i>:
     * comprueba si el id es un String y, si lo es, lo guarda en la variable {@code texto} para
     * poder usarlo en la misma línea. Como ID es genérico, no se sabe de antemano si es texto.
     */
    private void validarIdentificador(ID identificador) {
        if (identificador == null || identificador instanceof String texto && texto.isBlank()) {
            throw new ReglaNegocioException("El identificador es obligatorio.");
        }
    }
}
