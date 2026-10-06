package co.trazacarne.servicio.dto;

import co.trazacarne.dominio.EstadoVenta;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila por asignación, con los datos necesarios para presentar su origen. */
public record OrigenVenta(String numeroVenta, EstadoVenta estado, String codigoProducto,
                          String nombreProducto, String codigoLote, BigDecimal cantidad,
                          String nitAbastecedor, String nombreAbastecedor,
                          LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                          LocalDate fechaVencimiento) { }
