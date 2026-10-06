package co.trazacarne.excepcion;

/** Un registro desactivado no admite nuevas operaciones (RN-02). */
public class RegistroInactivoException extends ReglaNegocioException {

    public RegistroInactivoException(String mensaje) {
        super(mensaje);
    }
}
