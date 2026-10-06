package co.trazacarne.servicio;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.dominio.ValidacionDatos;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.RegistroInactivoException;
import co.trazacarne.excepcion.RegistroNoEncontradoException;
import co.trazacarne.repositorio.Repositorio;

import java.util.List;
import java.util.Objects;

public class ServicioClientes {

    private final Repositorio<Cliente, String> repositorio;

    public ServicioClientes(Repositorio<Cliente, String> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio de clientes es obligatorio.");
    }

    public Cliente registrar(String documento, String nombre) {
        Cliente cliente = new Cliente(documento, nombre);
        if (repositorio.existe(cliente.getDocumento())) {
            throw new IdentificadorDuplicadoException(
                    "Ya existe un cliente con el documento " + cliente.getDocumento() + ".");
        }
        repositorio.guardar(cliente);
        return cliente;
    }

    public Cliente consultar(String documento) {
        String identificador = ValidacionDatos.textoObligatorio(documento, "El documento del cliente");
        return repositorio.buscarPorId(identificador).orElseThrow(() ->
                new RegistroNoEncontradoException("No existe un cliente con el documento " + identificador + "."));
    }

    public Cliente consultarActivo(String documento) {
        Cliente cliente = consultar(documento);
        if (!cliente.estaActivo()) {
            throw new RegistroInactivoException("El cliente " + cliente.getDocumento() + " está inactivo.");
        }
        return cliente;
    }

    public List<Cliente> listar() {
        return repositorio.listar();
    }

    public void actualizar(String documento, String nombre) {
        Cliente cliente = consultar(documento);
        cliente.actualizar(nombre);
        repositorio.actualizar(cliente);
    }

    public void desactivar(String documento) {
        Cliente cliente = consultar(documento);
        cliente.desactivar();
        repositorio.actualizar(cliente);
    }
}
