package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;

/** Origen de las compras, identificado por NIT y con un tipo fijo. */
public class Abastecedor {

    private final String nit;
    private String nombre;
    private final TipoAbastecedor tipo;
    private boolean activo;

    public Abastecedor(String nit, String nombre, TipoAbastecedor tipo) {
        this.nit = ValidacionDatos.textoObligatorio(nit, "El NIT del abastecedor");
        this.nombre = ValidacionDatos.textoObligatorio(nombre, "El nombre del abastecedor");
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de abastecedor es obligatorio.");
        }
        this.tipo = tipo;
        this.activo = true;
    }

    public void actualizar(String nombre) {
        this.nombre = ValidacionDatos.textoObligatorio(nombre, "El nombre del abastecedor");
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean esMatadero() {
        return tipo == TipoAbastecedor.MATADERO;
    }

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
