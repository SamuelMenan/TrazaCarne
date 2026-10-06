package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.util.List;
import java.util.Objects;

/** Casos de uso de abastecedores: registrar, consultar, listar, actualizar y desactivar. */
public class ServicioAbastecedores {

    // Depende de la interfaz, no de la implementación en memoria.
    private final Repositorio<Abastecedor, String> repositorio;

    public ServicioAbastecedores(Repositorio<Abastecedor, String> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio de abastecedores es obligatorio.");
    }

    public Abastecedor registrar(String nit, String nombre, TipoAbastecedor tipo) {
        // Primero se crea (valida los datos) y luego se revisa que el NIT no exista.
        Abastecedor abastecedor = new Abastecedor(nit, nombre, tipo);
        if (repositorio.existe(abastecedor.getNit())) {
            throw new IdentificadorDuplicadoException(
                    "Ya existe un abastecedor con el NIT " + abastecedor.getNit() + ".");
        }
        repositorio.guardar(abastecedor);
        return abastecedor;
    }

    public Abastecedor consultar(String nit) {
        if (nit == null || nit.isBlank()) {
            throw new ReglaNegocioException("El NIT del abastecedor es obligatorio.");
        }
        String identificador = nit.strip();
        return repositorio.buscarPorId(identificador).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe un abastecedor con el NIT " + identificador + "."));
    }

    // Lo usan las compras: además de existir, el abastecedor debe estar activo.
    public Abastecedor consultarActivo(String nit) {
        Abastecedor abastecedor = consultar(nit);
        if (!abastecedor.estaActivo()) {
            throw new RegistroInactivoException("El abastecedor " + abastecedor.getNit() + " está inactivo.");
        }
        return abastecedor;
    }

    public List<Abastecedor> listar() {
        return repositorio.listar();
    }

    public void actualizar(String nit, String nombre) {
        Abastecedor abastecedor = consultar(nit);
        abastecedor.actualizar(nombre);
        repositorio.actualizar(abastecedor);
    }

    public void desactivar(String nit) {
        Abastecedor abastecedor = consultar(nit);
        abastecedor.desactivar();
        repositorio.actualizar(abastecedor);
    }
}
