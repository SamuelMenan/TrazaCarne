package co.trazacarne.dominio;

import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.RegistroInactivoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** La compra se prepara completa antes de incorporar sus lotes al inventario. */
public class Compra {
    private final String numero;
    private final LocalDate fecha;
    private final Abastecedor abastecedor;
    private final List<Lote> lotes = new ArrayList<>();
    // Evita registrar la misma compra dos veces o modificarla después.
    private boolean registrada;

    // Solo se puede comprar a un abastecedor activo.
    public Compra(String numero, LocalDate fecha, Abastecedor abastecedor) {
        if (numero == null || numero.isBlank()) {
            throw new ReglaNegocioException("El número de compra es obligatorio.");
        }
        if (fecha == null || abastecedor == null) {
            throw new ReglaNegocioException("La compra requiere fecha y abastecedor.");
        }
        if (!abastecedor.estaActivo()) {
            throw new RegistroInactivoException("El abastecedor está inactivo.");
        }
        this.numero = numero.strip();
        this.fecha = fecha;
        this.abastecedor = abastecedor;
    }

    // Crea el lote (Lote valida sus fechas y cantidad) y revisa que el código no se repita en la compra.
    public Lote agregarLote(String codigo, Producto producto, BigDecimal cantidad,
                            LocalDate sacrificio, LocalDate procesamiento, LocalDate vencimiento) {
        if (registrada) {
            throw new ReglaNegocioException("Una compra registrada no permite añadir lotes.");
        }
        Lote lote = new Lote(codigo, this, producto, cantidad, sacrificio, procesamiento, vencimiento);
        for (Lote existente : lotes) {
            if (existente.getCodigo().equals(lote.getCodigo())) {
                throw new IdentificadorDuplicadoException("El lote " + lote.getCodigo() + " se repite en la compra.");
            }
        }
        lotes.add(lote);
        return lote;
    }

    // Pasa todos los lotes al inventario de una vez; desde aquí la compra queda cerrada.
    public void registrar(Inventario inventario, LocalDateTime instante) {
        if (registrada) {
            throw new ReglaNegocioException("La compra " + numero + " ya fue registrada.");
        }
        if (lotes.isEmpty() || inventario == null) {
            throw new ReglaNegocioException("La compra requiere lotes y un inventario.");
        }
        inventario.registrarEntrada(this, instante);
        registrada = true;
    }

    // --- Getters (la lista de lotes se entrega como copia para que no se modifique desde afuera) ---

    public String getNumero() { return numero; }
    public LocalDate getFecha() { return fecha; }
    public Abastecedor getAbastecedor() { return abastecedor; }
    public List<Lote> getLotes() { return List.copyOf(lotes); }
    public boolean estaRegistrada() { return registrada; }
}
