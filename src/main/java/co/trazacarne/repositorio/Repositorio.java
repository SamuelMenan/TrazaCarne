package co.trazacarne.repositorio;

import java.util.List;
import java.util.Optional;

/** Contrato de almacenamiento; T es el registro e ID su tipo de identificador. */
public interface Repositorio<T, ID> {

    /** Registra un elemento nuevo; rechaza un identificador ya existente. */
    void guardar(T registro);

    /** Actualiza un elemento existente; rechaza uno desconocido. */
    void actualizar(T registro);

    /** Devuelve un Optional vacío si el identificador no está registrado. */
    Optional<T> buscarPorId(ID identificador);

    /** Incluye registros activos e inactivos; la lista no permite añadir o eliminar. */
    List<T> listar();

    boolean existe(ID identificador);
}
