package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;

/** Cliente identificado por documento; se conserva aunque sea desactivado (RN-02). */
public class Cliente {

    // El documento identifica al cliente y no cambia; el nombre sí se puede actualizar.
    private final String documento;
    private String nombre;
    private boolean activo;

    // Todo cliente nuevo empieza activo.
    public Cliente(String documento, String nombre) {
        this.documento = validarTexto(documento, "El documento del cliente");
        this.nombre = validarTexto(nombre, "El nombre del cliente");
        this.activo = true;
    }

    public void actualizar(String nombre) {
        this.nombre = validarTexto(nombre, "El nombre del cliente");
    }

    // No se borra para no perder sus ventas; solo deja de poder comprar.
    public void desactivar() {
        this.activo = false;
    }

    // El texto no puede venir vacío; se le quitan los espacios sobrantes.
    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return valor.strip();
    }

    // --- Getters ---

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean estaActivo() {
        return activo;
    }
}
