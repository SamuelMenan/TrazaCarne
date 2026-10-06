package co.trazacarne.dominio;

import co.trazacarne.excepcion.ReglaNegocioException;

/** Comprobaciones compartidas por las clases del dominio. Sin public: solo se usa dentro de este paquete. */
final class Validar {

    private Validar() {
    }

    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return valor.strip();
    }

    static <T> T obligatorio(T valor, String campo) {
        if (valor == null) {
            throw new ReglaNegocioException(campo + " es obligatorio.");
        }
        return valor;
    }
}
