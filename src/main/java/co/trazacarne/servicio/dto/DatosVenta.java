package co.trazacarne.servicio.dto;

import co.trazacarne.excepcion.ReglaNegocioException;
import java.util.List;

/** Datos que llegan desde la consola para registrar una venta (record = clase de solo datos). */
public record DatosVenta(String numero, String documentoCliente, List<ItemVenta> items) {
    // Rechaza productos nulos y guarda una copia que no se puede modificar.
    public DatosVenta {
        if (items != null) {
            for (ItemVenta item : items) {
                if (item == null) { throw new ReglaNegocioException("Los productos solicitados no pueden ser nulos."); }
            }
            items = List.copyOf(items);
        }
    }
}
