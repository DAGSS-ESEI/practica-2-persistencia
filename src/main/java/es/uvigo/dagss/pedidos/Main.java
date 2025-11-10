package es.uvigo.dagss.pedidos;

import java.util.Calendar;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import es.uvigo.dagss.pedidos.daos.ClienteDAO;
import es.uvigo.dagss.pedidos.daos.ClienteDAOJPA;
import es.uvigo.dagss.pedidos.daos.PedidoDAO;
import es.uvigo.dagss.pedidos.daos.PedidoDAOJPA;
import es.uvigo.dagss.pedidos.daos.PedidosException;
import es.uvigo.dagss.pedidos.entidades.Almacen;
import es.uvigo.dagss.pedidos.entidades.Articulo;
import es.uvigo.dagss.pedidos.entidades.ArticuloAlmacen;
import es.uvigo.dagss.pedidos.entidades.Cliente;
import es.uvigo.dagss.pedidos.entidades.Direccion;
import es.uvigo.dagss.pedidos.entidades.Familia;
import es.uvigo.dagss.pedidos.entidades.LineaPedido;
import es.uvigo.dagss.pedidos.entidades.Pedido;

public class Main {

    private static EntityManagerFactory emf;

    public static final void main(String[] args) {
        emf = Persistence.createEntityManagerFactory("pedidos_PU");

        try {
            pruebaCrearEntidadesConEM();
            pruebaCrearEntidadesConDAOs();
            pruebaConsultaArticulosConEM();
            pruebaConsultaPedidosConDAO();
            pruebaConsultaCriteriaQuery();
        } catch (PedidosException e) {
            System.err.println("Excepción en main()");
            e.printStackTrace();
        }

        emf.close();

        System.exit(0);
    }

    private static final void pruebaCrearEntidadesConEM() {
        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Familia f1 = new Familia("tubos", "tubos de todas clases");
            Familia f2 = new Familia("tuercas", "tuercas de todas clases");
            em.persist(f1);
            em.persist(f2);

            Articulo a1 = new Articulo("tubo acero", "tubo de acero", f1, 10.0);
            Articulo a2 = new Articulo("tubo plastico", "tubo de plastico", f1, 5.0);
            Articulo a3 = new Articulo("tuerca acero", "tuerca de acero 10/18", f2, 10.0);
            Articulo a4 = new Articulo("tuerca plástico", "tuerca de plástico", f2, 5.0);
            em.persist(a1);
            em.persist(a2);
            em.persist(a3);
            em.persist(a4);

            Direccion d = new Direccion("calle 3", "Santiago", "33333", "A Coruña", "981333333");
            Almacen a = new Almacen("principal", "almacen principal", d);
            em.persist(a);

            ArticuloAlmacen aa1 = new ArticuloAlmacen(a1, a, 10);
            ArticuloAlmacen aa2 = new ArticuloAlmacen(a2, a, 15);
            ArticuloAlmacen aa3 = new ArticuloAlmacen(a3, a, 20);
            ArticuloAlmacen aa4 = new ArticuloAlmacen(a4, a, 25);
            em.persist(aa1);
            em.persist(aa2);
            em.persist(aa3);
            em.persist(aa4);

            tx.commit();

        } catch (Exception e) {
            System.err.println("Error en pruebaCrearEntidades");
            e.printStackTrace(System.err);

            if ((tx != null) && tx.isActive()) {
                tx.rollback();
            }
        }

        em.close();
    }

    private static final void pruebaCrearEntidadesConDAOs() throws PedidosException {
        EntityManager em = emf.createEntityManager();

        ClienteDAO clienteDAO = new ClienteDAOJPA(em);
        PedidoDAO pedidoDAO = new PedidoDAOJPA(em);

        Direccion d1 = new Direccion("calle 1", "Ourense", "11111", "Ourense", "988111111");
        Direccion d2 = new Direccion("calle 2", "Santiago", "22222", "A Coruña", "981222222");

        Cliente c1 = new Cliente("11111111A", "Pepe Cliente1 Cliente1", d1);
        Cliente c2 = new Cliente("22222222A", "Ana Cliente2 Cliente2", d2);
        c1 = clienteDAO.crear(c1);
        c2 = clienteDAO.crear(c2);

        Cliente c = clienteDAO.buscarPorClave("11111111A");
        Articulo a1 = em.find(Articulo.class, (long) 1);
        Articulo a2 = em.find(Articulo.class, (long) 3);
        Articulo a3 = em.find(Articulo.class, (long) 4);

        Pedido p1 = new Pedido(Calendar.getInstance().getTime(), c);
        p1.anadirLineaPedido(new LineaPedido(p1, 2, a1, a1.getPrecioUnitario()));
        p1.anadirLineaPedido(new LineaPedido(p1, 2, a2, a2.getPrecioUnitario()));
        p1.anadirLineaPedido(new LineaPedido(p1, 2, a3, a3.getPrecioUnitario()));
        p1 = pedidoDAO.crear(p1);

        Pedido p2 = new Pedido(Calendar.getInstance().getTime(), c);
        p2.anadirLineaPedido(new LineaPedido(p2, 100, a1, a1.getPrecioUnitario()));
        p2.anadirLineaPedido(new LineaPedido(p2, 100, a2, a2.getPrecioUnitario()));

        p2 = pedidoDAO.crear(p2);

        em.close();
    }

    private static void pruebaConsultaArticulosConEM() {
        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            System.out.println("[PRUEBA JPA]: --------------------");
            System.out.println("[PRUEBA JPA]: Listado de artículos");
            System.out.println("[PRUEBA JPA]: --------------------");

            TypedQuery<Articulo> query = em.createQuery("SELECT a FROM Articulo AS a", Articulo.class);
            List<Articulo> articulos = query.getResultList();

            for (Articulo a : articulos) {
                System.out.println("[PRUEBA JPA]: " + a.toString());
            }
            System.out.println("[PRUEBA JPA]: ------------------\n");

            tx.commit();

        } catch (Exception e) {
            System.err.println("Error en pruebaConsultaArticulos");
            e.printStackTrace(System.err);

            if ((tx != null) && tx.isActive()) {
                tx.rollback();
            }
        }

        em.close();
    }

    private static void pruebaConsultaPedidosConDAO() {
        EntityManager em = emf.createEntityManager();

        System.out.println("[PRUEBA JPA]: ------------------");
        System.out.println("[PRUEBA JPA]: Listado de pedidos");
        System.out.println("[PRUEBA JPA]: ------------------");

        PedidoDAO dao = new PedidoDAOJPA(em);

        List<Pedido> pedidos = dao.buscarPorClienteConLineasPedido("11111111A");
        System.out.println("tamano " + pedidos.size());
        for (Pedido p : pedidos) {
            System.out.println("[PRUEBA JPA]: " + p.toString());
            for (LineaPedido lp : p.getLineas()) {
                System.out.println("[PRUEBA JPA]:   " + lp.toString() + "   [total : " + lp.getImporteTotal() + "]");
            }
            System.out.println("[PRUEBA JPA]: ------------------");
        }
        System.out.println("[PRUEBA JPA]: ------------------\n");

        em.close();
    }

    private static void pruebaConsultaCriteriaQuery() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            System.out.println("[PRUEBA JPA]: --------------------");
            System.out.println("[PRUEBA JPA]: Pruebas criteria API");
            System.out.println("[PRUEBA JPA]: --------------------");
            
            CriteriaBuilder cb = em.getCriteriaBuilder();

            // Pedidos con id=2
            // SQL: "SELECT * FROM Pedido p WHERE p.id = 2"
            CriteriaQuery<Pedido> c = cb.createQuery(Pedido.class);
            Root<Pedido> pedido = c.from(Pedido.class);
            c.select(pedido).where(cb.equal(pedido.get("id"), 2));

            Pedido resultado = em.createQuery(c).getSingleResult();
            System.out.println("Resultado criteria API 1: " + resultado);


            // Pedidos de los Clientes de nombre "Pepe"
            // SQL: "SELECT * FROM Pedido p JOIN Cliente c ON c.DNI=p.cliente_DNI WHERE c.nombre LIKE '%Pepe%' " 
            Join<Pedido, Cliente> joinCliente = pedido.join("cliente");
            c.select(pedido).where(cb.like(joinCliente.get("nombre"), "%Pepe%"));
            List<Pedido> resultados = em.createQuery(c).getResultList();
            if (!resultados.isEmpty()) {
                Pedido resultado2 = resultados.get(0);
                System.out.println("Resultado criteria API 2: " + resultado2);
            } else {
                System.out.println("Pedido no encontrado.");
            }

            System.out.println("[PRUEBA JPA]: ------------------\n");
            tx.commit();
        } catch (Exception e) {
            System.err.println("Error en pruebaConsultaArticulos");
            e.printStackTrace(System.err);
            if ((tx != null) && tx.isActive()) {
                tx.rollback();
            }
        }
        em.close();
    }
}
