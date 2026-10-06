package co.trazacarne.excepcion;

public class IdentificadorDuplicadoException extends ReglaNegocioException {

    public IdentificadorDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
