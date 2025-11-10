package es.uvigo.dagss.pedidos.daos;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import es.uvigo.dagss.pedidos.entidades.Cliente;

public class ClienteDAOJPA extends GenericoDAOJPA<Cliente, String> implements ClienteDAO {

    public ClienteDAOJPA(EntityManager em) {
        super(em, Cliente.class);
    }

    @Override
    public List<Cliente> buscarPorLocalidad(String localidad) throws PedidosException {
        List<Cliente> resultado = Collections.emptyList();
        EntityTransaction tx = this.em.getTransaction();
        try {
            tx.begin();
            TypedQuery<Cliente> query = em.createQuery("SELECT c FROM Cliente AS c WHERE c.direccion.localidad = :localidad", Cliente.class);
            query.setParameter("localidad", localidad);
            resultado = query.getResultList();
            tx.commit();
        } catch (Exception e) {
            if ((tx != null) && (tx.isActive())) {
                tx.rollback();
            }
            throw new PedidosException("Excepción buscando por localidad", e);
        }
        return resultado;
    }

    @Override
    public List<Cliente> buscarPorNombre(String patron) throws PedidosException {
        List<Cliente> resultado = Collections.emptyList();
        EntityTransaction tx = this.em.getTransaction();
        try {
            tx.begin();
            TypedQuery<Cliente> query = em.createQuery("SELECT c FROM Cliente AS c WHERE c.nombre LIKE :patron", Cliente.class);
            query.setParameter("patron", "%" + patron + "%");
            resultado = query.getResultList();
            tx.commit();
        } catch (Exception e) {
            if ((tx != null) && (tx.isActive())) {
                tx.rollback();
            }
            throw new PedidosException("Excepción buscando por nombre", e);
        }
        return resultado;
    }

}
