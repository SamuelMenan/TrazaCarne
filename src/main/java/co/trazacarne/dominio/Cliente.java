package co.trazacarne.dominio;

/** Persona que recibe una venta; conserva el destino del producto (HU-02). */
public class Cliente {

    private final String documento;
    private String nombre;
    private boolean activo;

    public Cliente(String documento, String nombre) {
        this.documento = Validar.texto(documento, "El documento del cliente");
        this.nombre = Validar.texto(nombre, "El nombre del cliente");
        this.activo = true;
    }

    public void actualizar(String nombre) {
        this.nombre = Validar.texto(nombre, "El nombre del cliente");
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean estaActivo() {
        return activo;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }
}
