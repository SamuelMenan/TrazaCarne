package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;

/** Origen de las compras, identificado por NIT y con un tipo fijo. */
public class Abastecedor {

    // NIT y tipo son fijos; el nombre y el estado sí pueden cambiar.
    private final String nit;
    private String nombre;
    private final TipoAbastecedor tipo;
    private boolean activo;

    // Todo abastecedor nuevo empieza activo.
    public Abastecedor(String nit, String nombre, TipoAbastecedor tipo) {
        this.nit = validarTexto(nit, "El NIT del abastecedor");
        this.nombre = validarTexto(nombre, "El nombre del abastecedor");
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de abastecedor es obligatorio.");
        }
        this.tipo = tipo;
        this.activo = true;
    }

    public void actualizar(String nombre) {
        this.nombre = validarTexto(nombre, "El nombre del abastecedor");
    }

    // No se borra para conservar el origen de los lotes; ya no se le pueden registrar compras.
    public void desactivar() {
        this.activo = false;
    }

    // Los lotes de un matadero exigen fecha de sacrificio (se valida en Lote).
    public boolean esMatadero() {
        return tipo == TipoAbastecedor.MATADERO;
    }

    // El texto no puede venir vacío; se le quitan los espacios sobrantes.
    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return valor.strip();
    }

    // --- Getters ---

    public String getNit() {
        return nit;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoAbastecedor getTipo() {
        return tipo;
    }

    public boolean estaActivo() {
        return activo;
    }
}
