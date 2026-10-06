package co.trazacarne.servicio.dto;

import co.trazacarne.dominio.EstadoVenta;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila por venta que utilizó el lote, incluyendo cliente, cantidad y estado. */
public record DestinoLote(String numeroVenta, LocalDate fecha, EstadoVenta estado,
                          String documentoCliente, String nombreCliente, BigDecimal cantidad) { }
