package co.trazacarne.excepcion;

public class RegistroInactivoException extends ReglaNegocioException {

    public RegistroInactivoException(String mensaje) {
        super(mensaje);
    }
}
