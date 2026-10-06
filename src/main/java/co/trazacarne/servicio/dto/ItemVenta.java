package co.trazacarne.servicio.dto;

import java.math.BigDecimal;

/** Un producto pedido en la venta y su cantidad (kg o unidades). */
public record ItemVenta(String codigoProducto, BigDecimal cantidad) { }
