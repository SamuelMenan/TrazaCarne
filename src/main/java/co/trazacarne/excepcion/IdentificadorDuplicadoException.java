package co.trazacarne.excepcion;

/** Un identificador obligatorio ya existe en su ámbito (RN-01). */
public class IdentificadorDuplicadoException extends ReglaNegocioException {

    public IdentificadorDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
