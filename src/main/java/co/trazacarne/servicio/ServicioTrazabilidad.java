package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.AsignacionLote;
import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.DetalleVenta;
import co.trazacarne.dominio.EstadoVenta;
import co.trazacarne.dominio.Inventario;
import co.trazacarne.dominio.Lote;
import co.trazacarne.dominio.Venta;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;
import co.trazacarne.servicio.dto.DestinoLote;
import co.trazacarne.servicio.dto.OrigenVenta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Trazabilidad en dos sentidos: de una venta hacia su origen y de un lote hacia sus clientes. */
public class ServicioTrazabilidad {
    private final Repositorio<Venta, String> ventas;
    private final Inventario inventario;

    public ServicioTrazabilidad(Repositorio<Venta, String> ventas, Inventario inventario) {
        this.ventas = Objects.requireNonNull(ventas);
        this.inventario = Objects.requireNonNull(inventario);
    }

    // Hacia atrás: ¿de qué lotes y de qué abastecedor salió lo que se vendió?
    public List<OrigenVenta> origenDeVenta(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new ReglaNegocioException("El número de venta es obligatorio.");
        }
        Venta venta = ventas.buscarPorId(numero.strip()).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe la venta " + numero.strip() + "."));
        List<OrigenVenta> resultado = new ArrayList<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            for (AsignacionLote asignacion : detalle.getAsignaciones()) {
                Lote lote = asignacion.getLote();
                Abastecedor origen = lote.getAbastecedor();
                resultado.add(new OrigenVenta(venta.getNumero(), venta.getEstado(),
                        detalle.getProducto().getCodigo(), detalle.getProducto().getNombre(),
                        lote.getCodigo(), asignacion.getCantidad(), origen.getNit(), origen.getNombre(),
                        lote.getFechaSacrificio(), lote.getFechaProcesamiento(), lote.getFechaVencimiento()));
            }
        }
        return List.copyOf(resultado);
    }

    /**
     * Hacia adelante: ¿en qué ventas terminó este lote? Sirve, por ejemplo, para avisar a los clientes
     * si hay que retirar un lote contaminado.
     *
     * <p>Recorre todas las ventas y, para cada una, calcula cuánto salió de este lote. La condición del
     * {@code if} salta (con {@code continue}) las ventas que no interesan:
     * <ul>
     *   <li>Siempre se saltan las que están en BORRADOR (nunca descontaron inventario).</li>
     *   <li>Las ANULADAS se saltan solo si {@code incluirAnuladas} es false.</li>
     * </ul>
     * Ojo con la precedencia: {@code &&} se evalúa antes que {@code ||}, así que se lee como
     * {@code BORRADOR || (!incluirAnuladas && ANULADA)}.
     */
    public List<DestinoLote> ventasDelLote(String codigo, boolean incluirAnuladas) {
        Lote lote = inventario.consultarLote(codigo);
        List<DestinoLote> resultado = new ArrayList<>();
        for (Venta venta : ventas.listar()) {
            if (venta.getEstado() == EstadoVenta.BORRADOR
                    || !incluirAnuladas && venta.getEstado() == EstadoVenta.ANULADA) { continue; }
            BigDecimal cantidad = cantidadDelLote(venta, lote);
            if (cantidad.signum() > 0) {
                resultado.add(new DestinoLote(venta.getNumero(), venta.getFecha(), venta.getEstado(),
                        venta.getCliente().getDocumento(), venta.getCliente().getNombre(), cantidad));
            }
        }
        return List.copyOf(resultado);
    }

    public List<DestinoLote> ventasDelLote(String codigo) {
        return ventasDelLote(codigo, true);
    }

    // Clientes que recibieron producto del lote, sin repetir.
    public List<Cliente> clientesDelLote(String codigo) {
        Lote lote = inventario.consultarLote(codigo);
        Map<String, Cliente> resultado = new LinkedHashMap<>();
        for (Venta venta : ventas.listar()) {
            if (venta.getEstado() != EstadoVenta.BORRADOR && cantidadDelLote(venta, lote).signum() > 0) {
                resultado.put(venta.getCliente().getDocumento(), venta.getCliente());
            }
        }
        return List.copyOf(resultado.values());
    }

    /**
     * Cuánto de una venta salió de un lote concreto (0 si no lo usó).
     *
     * <p>Recorre venta → detalles → asignaciones. Se compara con {@code ==} (mismo objeto) y no por código,
     * porque el inventario trabaja con las mismas instancias de Lote que se guardaron al comprar.
     */
    private BigDecimal cantidadDelLote(Venta venta, Lote lote) {
        BigDecimal cantidad = BigDecimal.ZERO;
        for (DetalleVenta detalle : venta.getDetalles()) {
            for (AsignacionLote asignacion : detalle.getAsignaciones()) {
                if (asignacion.getLote() == lote) { cantidad = cantidad.add(asignacion.getCantidad()); }
            }
        }
        return cantidad;
    }
}
