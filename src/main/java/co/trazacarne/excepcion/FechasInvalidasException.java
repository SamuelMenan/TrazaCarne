package co.trazacarne.excepcion;

/** Fechas faltantes o en un orden imposible (por ejemplo, un lote ya vencido). */
public class FechasInvalidasException extends ReglaNegocioException {
    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
