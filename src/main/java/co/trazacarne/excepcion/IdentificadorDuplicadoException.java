package co.trazacarne.excepcion;

/** Ya existe un registro con ese documento, NIT, código o número. */
public class IdentificadorDuplicadoException extends ReglaNegocioException {

    public IdentificadorDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
