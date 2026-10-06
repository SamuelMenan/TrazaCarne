package co.trazacarne.excepcion;

/** Comunica el motivo por el que una regla del negocio impide una operación. */
// Clase base: todas las demás excepciones heredan de esta, así la consola las atrapa con un solo catch.
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
