package es.uvigo.dagss.pedidos;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import es.uvigo.dagss.pedidos.entidades.Almacen;
import es.uvigo.dagss.pedidos.entidades.Articulo;
import es.uvigo.dagss.pedidos.entidades.ArticuloAlmacen;
import es.uvigo.dagss.pedidos.entidades.Direccion;
import es.uvigo.dagss.pedidos.entidades.Familia;

public class Main {

    private static EntityManagerFactory emf;

    public static final void main(String[] args) {
        emf = Persistence.createEntityManagerFactory("pedidos_PU");

        pruebaCrearEntidadesConEM();
        pruebaConsultaArticulosConEM();

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
}
