package co.trazacarne.excepcion;

/** Una venta solo se puede anular una vez. */
public class VentaYaAnuladaException extends ReglaNegocioException {
    public VentaYaAnuladaException(String mensaje) {
        super(mensaje);
    }
}
