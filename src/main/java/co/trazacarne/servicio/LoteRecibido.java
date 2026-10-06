package co.trazacarne.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un lote tal como los entrega quien registra la compra (los "lotesRecibidos" del diagrama). */
public record LoteRecibido(String codigoLote, String codigoProducto, BigDecimal cantidad,
                           LocalDate fechaSacrificio, LocalDate fechaProcesamiento, LocalDate fechaVencimiento) {
}
