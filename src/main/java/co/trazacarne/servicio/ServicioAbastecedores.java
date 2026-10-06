package co.trazacarne.servicio;

import co.trazacarne.dominio.Abastecedor;
import co.trazacarne.dominio.TipoAbastecedor;
import co.trazacarne.dominio.ValidacionDatos;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.util.List;
import java.util.Objects;

public class ServicioAbastecedores {

    private final Repositorio<Abastecedor, String> repositorio;

    public ServicioAbastecedores(Repositorio<Abastecedor, String> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio de abastecedores es obligatorio.");
    }

    public Abastecedor registrar(String nit, String nombre, TipoAbastecedor tipo) {
        Abastecedor abastecedor = new Abastecedor(nit, nombre, tipo);
        if (repositorio.existe(abastecedor.getNit())) {
            throw new IdentificadorDuplicadoException(
                    "Ya existe un abastecedor con el NIT " + abastecedor.getNit() + ".");
        }
        repositorio.guardar(abastecedor);
        return abastecedor;
    }

    public Abastecedor consultar(String nit) {
        String identificador = ValidacionDatos.textoObligatorio(nit, "El NIT del abastecedor");
        return repositorio.buscarPorId(identificador).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe un abastecedor con el NIT " + identificador + "."));
    }

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
