package co.trazacarne.dominio;

/** Cliente identificado por documento; se conserva aunque sea desactivado (RN-02). */
public class Cliente {

    private final String documento;
    private String nombre;
    private boolean activo;

    public Cliente(String documento, String nombre) {
        this.documento = ValidacionDatos.textoObligatorio(documento, "El documento del cliente");
        this.nombre = ValidacionDatos.textoObligatorio(nombre, "El nombre del cliente");
        this.activo = true;
    }

    public void actualizar(String nombre) {
        this.nombre = ValidacionDatos.textoObligatorio(nombre, "El nombre del cliente");
    }

    public void desactivar() {
        this.activo = false;
    }

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
