package co.trazacarne.servicio.dto;

import co.trazacarne.excepcion.ReglaNegocioException;
import java.util.List;

/** Datos que llegan desde la consola para registrar una compra (record = clase de solo datos). */
public record DatosCompra(String numero, String nitAbastecedor, List<DatosLoteRecibido> lotes) {
    // Rechaza lotes nulos y guarda una copia que no se puede modificar.
    public DatosCompra {
        if (lotes != null) {
            for (DatosLoteRecibido lote : lotes) {
                if (lote == null) { throw new ReglaNegocioException("Los lotes recibidos no pueden ser nulos."); }
            }
            lotes = List.copyOf(lotes);
        }
    }
}
