package co.trazacarne.excepcion;

/** Comunica el motivo por el que una regla del negocio impide una operación. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
