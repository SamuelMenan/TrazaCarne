package co.trazacarne.servicio;

import co.trazacarne.dominio.Cliente;
import co.trazacarne.excepcion.IdentificadorDuplicadoException;
import co.trazacarne.excepcion.ReglaNegocioException;
import co.trazacarne.repositorio.Repositorio;

/** Casos de uso de HU-02. */
public class ServicioClientes {

    private final Repositorio<Cliente, String> clientes;

    public ServicioClientes(Repositorio<Cliente, String> clientes) {
        this.clientes = clientes;
    }

    public Cliente registrar(String documento, String nombre) {
        Cliente cliente = new Cliente(documento, nombre);
        if (clientes.existe(cliente.getDocumento())) {
            throw new IdentificadorDuplicadoException("Ya existe un cliente con documento " + cliente.getDocumento() + ".");
        }
        clientes.guardar(cliente);
        return cliente;
    }

    public Cliente consultar(String documento) {
        return clientes.buscarPorId(documento)
                .orElseThrow(() -> new ReglaNegocioException("No existe el cliente con documento " + documento + "."));
    }

    public void actualizar(String documento, String nombre) {
        Cliente cliente = consultar(documento);
        cliente.actualizar(nombre);
        clientes.guardar(cliente);
    }

    /** RN-02: se desactiva, nunca se borra. */
    public void desactivar(String documento) {
        Cliente cliente = consultar(documento);
        cliente.desactivar();
        clientes.guardar(cliente);
    }
}
