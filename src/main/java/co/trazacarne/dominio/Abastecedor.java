package co.trazacarne.dominio;

/** Matadero o proveedor que suministra los lotes (HU-01). */
public class Abastecedor {

    private final String nit;
    private String nombre;
    private final TipoAbastecedor tipo;
    private boolean activo;

    public Abastecedor(String nit, String nombre, TipoAbastecedor tipo) {
        this.nit = Validar.texto(nit, "El NIT");
        this.nombre = Validar.texto(nombre, "El nombre del abastecedor");
        this.tipo = Validar.obligatorio(tipo, "El tipo de abastecedor");
        this.activo = true;
    }

    public boolean esMatadero() {
        return tipo == TipoAbastecedor.MATADERO;
    }

    public void actualizar(String nombre) {
        this.nombre = Validar.texto(nombre, "El nombre del abastecedor");
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean estaActivo() {
        return activo;
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
}
