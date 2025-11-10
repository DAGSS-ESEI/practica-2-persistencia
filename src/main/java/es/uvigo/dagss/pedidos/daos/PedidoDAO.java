package es.uvigo.dagss.pedidos.daos;

import java.util.List;

import es.uvigo.dagss.pedidos.entidades.Pedido;

public interface PedidoDAO extends GenericoDAO<Pedido, Long> {
    public Pedido buscarPorClaveConLineasPedido(Long clave);
    public List<Pedido> buscarPorCliente(String DNI);
    public List<Pedido> buscarPorClienteConLineasPedido(String DNI);
}
