package co.trazacarne.excepcion;

/** Una cantidad debe ser positiva y respetar la forma de venta del producto. */
public class CantidadInvalidaException extends ReglaNegocioException {

    public CantidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}
