package co.trazacarne.excepcion;

/** Las fechas de un lote son incoherentes o faltan (RN-03, RN-04). */
public class FechasInvalidasException extends ReglaNegocioException {

    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
