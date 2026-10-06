package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.Repositorio;

/** Casos de uso de HU-01. */
public class ServicioAbastecedores {

    private final Repositorio<Abastecedor, String> abastecedores;

    public ServicioAbastecedores(Repositorio<Abastecedor, String> abastecedores) {
        this.abastecedores = abastecedores;
    }

    public Abastecedor registrar(String nit, String nombre, TipoAbastecedor tipo) {
        Abastecedor abastecedor = new Abastecedor(nit, nombre, tipo);
        if (abastecedores.existe(abastecedor.getNit())) {
            throw new IdentificadorDuplicadoException("Ya existe un abastecedor con NIT " + abastecedor.getNit() + ".");
        }
        abastecedores.guardar(abastecedor);
        return abastecedor;
    }

    public Abastecedor consultar(String nit) {
        return abastecedores.buscarPorId(nit)
                .orElseThrow(() -> new ReglaNegocioException("No existe el abastecedor con NIT " + nit + "."));
    }

    public void actualizar(String nit, String nombre) {
        Abastecedor abastecedor = consultar(nit);
        abastecedor.actualizar(nombre);
        abastecedores.guardar(abastecedor);
    }

    /** RN-02: se desactiva, nunca se borra. */
    public void desactivar(String nit) {
        Abastecedor abastecedor = consultar(nit);
        abastecedor.desactivar();
        abastecedores.guardar(abastecedor);
    }
}
