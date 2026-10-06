package co.trazacarne.excepcion;

/** El identificador buscado no existe. */
public class RegistroNoEncontradoException extends ReglaNegocioException {

    public RegistroNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
