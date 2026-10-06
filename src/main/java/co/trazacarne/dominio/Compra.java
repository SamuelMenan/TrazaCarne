package co.trazacarne.dominio;

import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.RegistroInactivoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mercancía recibida de un abastecedor; cada producto entra como lote (HU-04). */
public class Compra {

    private final String numero;
    private final LocalDate fecha;
    private final Abastecedor abastecedor;
    private final List<Lote> lotes;

    public Compra(String numero, LocalDate fecha, Abastecedor abastecedor) {
        this.numero = Validar.texto(numero, "El número de compra");
        this.fecha = Validar.obligatorio(fecha, "La fecha de compra");
        this.abastecedor = Validar.obligatorio(abastecedor, "El abastecedor");
        if (!abastecedor.estaActivo()) {
            throw new RegistroInactivoException("El abastecedor " + abastecedor.getNit() + " está inactivo.");
        }
        this.lotes = new ArrayList<>();
    }

    public Lote agregarLote(String codigo, Producto producto, BigDecimal cantidad,
                            LocalDate fechaSacrificio, LocalDate fechaProcesamiento, LocalDate fechaVencimiento) {
        String codigoLimpio = Validar.texto(codigo, "El código del lote");
        boolean repetido = lotes.stream().anyMatch(l -> l.getCodigo().equals(codigoLimpio));
        if (repetido) {
            throw new IdentificadorDuplicadoException("El lote " + codigoLimpio + " está repetido en la compra.");
        }
        Validar.obligatorio(producto, "El producto del lote");
        if (!producto.estaActivo()) {
            throw new RegistroInactivoException("El producto " + producto.getCodigo() + " está inactivo.");
        }
        Lote lote = new Lote(codigoLimpio, this, producto, cantidad,
                fechaSacrificio, fechaProcesamiento, fechaVencimiento);
        lotes.add(lote);
        return lote;
    }

    /** Lista de solo lectura: los lotes se agregan únicamente con agregarLote. */
    public List<Lote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Abastecedor getAbastecedor() {
        return abastecedor;
    }
}
