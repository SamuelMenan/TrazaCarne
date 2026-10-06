package co.trazacarne.excepcion;

/** No hay existencias vendibles suficientes (RN-07). */
public class StockInsuficienteException extends ReglaNegocioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
