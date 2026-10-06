package co.trazacarne.excepcion;

/** No hay existencias suficientes para vender o descontar la cantidad pedida. */
public class StockInsuficienteException extends ReglaNegocioException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
