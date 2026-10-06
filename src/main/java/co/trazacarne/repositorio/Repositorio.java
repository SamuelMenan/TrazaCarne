package co.trazacarne.repositorio;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de almacenamiento (RNF-04). Los servicios dependen de esta interfaz, no de su implementación.
 * T = tipo de entidad; ID = tipo de su identificador.
 */
public interface Repositorio<T, ID> {

    /** Guarda la entidad; si ya existe una con el mismo id, la reemplaza. Los servicios usan existe() antes de registrar. */
    void guardar(T entidad);

    /** Optional.empty() si no existe; nunca devuelve null. */
    Optional<T> buscarPorId(ID id);

    List<T> listar();

    boolean existe(ID id);
}
