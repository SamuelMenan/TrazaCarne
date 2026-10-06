package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;

/** Validación compartida de textos obligatorios del negocio. */
public final class ValidacionDatos {

    private ValidacionDatos() {
    }

    public static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return valor.strip();
    }
}
