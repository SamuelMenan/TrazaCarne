package co.trazacarne.excepcion;

/** Se intentó operar con un cliente, abastecedor o producto desactivado. */
public class RegistroInactivoException extends ReglaNegocioException {

    public RegistroInactivoException(String mensaje) {
        super(mensaje);
    }
}
