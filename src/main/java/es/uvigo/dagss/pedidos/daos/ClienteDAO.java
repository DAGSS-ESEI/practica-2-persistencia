package es.uvigo.dagss.pedidos.daos;

import java.util.List;

import es.uvigo.dagss.pedidos.entidades.Cliente;

public interface ClienteDAO extends GenericoDAO<Cliente, String>{
    public List<Cliente> buscarPorLocalidad(String localidad) throws PedidosException;
    public List<Cliente> buscarPorNombre(String patron)  throws PedidosException;
}
