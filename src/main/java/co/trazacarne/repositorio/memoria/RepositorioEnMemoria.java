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

/** Almacena registros durante la sesión, conservando el orden de registro. */
public class    RepositorioEnMemoria<T, ID> implements Repositorio<T, ID> {

    private final Map<ID, T> datos = new LinkedHashMap<>();
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

    @Override
    public List<T> listar() {
        return List.copyOf(datos.values());
    }

    @Override
    public boolean existe(ID identificador) {
        validarIdentificador(identificador);
        return datos.containsKey(identificador);
    }

    private ID identificadorDe(T registro) {
        if (registro == null) {
            throw new ReglaNegocioException("El registro es obligatorio.");
        }
        ID identificador = obtenerIdentificador.apply(registro);
        validarIdentificador(identificador);
        return identificador;
    }

    private void validarIdentificador(ID identificador) {
        if (identificador == null || identificador instanceof String texto && texto.isBlank()) {
            throw new ReglaNegocioException("El identificador es obligatorio.");
        }
    }
}
