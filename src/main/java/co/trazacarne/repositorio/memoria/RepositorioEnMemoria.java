package co.trazacarne.repositorio.memoria;

import co.trazacarne.repositorio.Repositorio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Implementación en memoria: los datos se pierden al cerrar el programa. */
public class RepositorioEnMemoria<T, ID> implements Repositorio<T, ID> {

    private final Map<ID, T> datos;
    private final Function<T, ID> extractorId;

    /** Ejemplo: new RepositorioEnMemoria<Cliente, String>(Cliente::getDocumento). */
    public RepositorioEnMemoria(Function<T, ID> extractorId) {
        this.datos = new LinkedHashMap<>();
        this.extractorId = extractorId;
    }

    @Override
    public void guardar(T entidad) {
        datos.put(extractorId.apply(entidad), entidad);
    }

    @Override
    public Optional<T> buscarPorId(ID id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<T> listar() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public boolean existe(ID id) {
        return datos.containsKey(id);
    }
}
