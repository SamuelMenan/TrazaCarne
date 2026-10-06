package co.trazacarne.excepcion;

public class RegistroNoEncontradoException extends ReglaNegocioException {

    public RegistroNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
