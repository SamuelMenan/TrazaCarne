package co.trazacarne.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos solicitados; las reglas del lote se validan en Lote. */
public record DatosLoteRecibido(String codigoLote, String codigoProducto, BigDecimal cantidad,
                               LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                               LocalDate fechaVencimiento) { }
