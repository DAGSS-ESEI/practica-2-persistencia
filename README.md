# DAO genérico con JPA
> DAGSS 2026-2027, semana 2

Práctica guiada: se parte de un proyecto con el modelo de dominio **ya mapeado** y se construye una **capa de acceso a datos (patrón DAO)** sobre JPA propia.

* Proyecto con JPA (dependencias, `persistence.xml`, entidades ya anotadas).
* Construcción de un **DAO genérico** y de DAOs específicos.
* Uso de `EntityManager`, transacciones, JPQL y Criteria API.

## 1. Qué contiene el proyecto

* `src/main/java/es/uvigo/dagss/pedidos/entidades/`: modelo de dominio ya mapeado con JPA (`Cliente`, `Pedido`, `LineaPedido`, `Articulo`, `Familia`, `Almacen`, `ArticuloAlmacen`, `Direccion`, ...).
* `src/main/java/es/uvigo/dagss/pedidos/Main.java`: dos pruebas "en bruto" con `EntityManager`:
  * `pruebaCrearEntidadesConEM()`: crea familias, artículos, un almacén y stock.
  * `pruebaConsultaArticulosConEM()`: lista los artículos.
* `src/main/resources/META-INF/persistence.xml`: unidad de persistencia `pedidos_PU` (MySQL, `drop-and-create`).
* **No hay capa DAO**: la vas a construir tú en los ejercicios.

El modelo E-R está en [`doc/modeloER_pedidos.png`](doc/modeloER_pedidos.png).

## 2. Requisitos y puesta en marcha

### Requisitos previos

* Servidor de BD MySQL 8.0.46 (o compatible)
* Java 25 (OpenJDK 25 u Oracle JDK 25)
* Maven 3.9.8
* (opcional) Git
* (opcional) IDE Java (Eclipse, Netbeans, IntelliJ, VS Code)

**Nota:** en los equipos de laboratorio puede ser conveniente establecer la variable de entorno `JAVA_HOME`, para que `mvn` compile y ejecute siempre con el mismo JDK:

```sh
export JAVA_HOME=/usr/lib/jvm/jdk-25

export PATH=$JAVA_HOME/bin:$PATH
```

*Ajustad la ruta a la instalación de Java 25 disponible en vuestro equipo.*

### Crear la BD

#### Con servidor de BD en local

* Crear la BD `pruebas_dagss` en MySQL y un usuario `dagss/dagss`:

```sql
mysql -u root -p    -- pedirá la contraseña de MySQL

mysql> create database pruebas_dagss;
mysql> create user dagss@localhost identified by "dagss";
mysql> grant all privileges on pruebas_dagss.* to dagss@localhost;
```

Adicionalmente, puede ser necesario establecer una zona horaria compatible:

```sql
mysql> set @@global.time_zone = '+00:00';
mysql> set @@session.time_zone = '+00:00';
```

**Nota:** no hace falta crear tablas ni cargar datos a mano: el proyecto usa `drop-and-create`, de modo que JPA crea el esquema en cada ejecución y `pruebaCrearEntidadesConEM()` inserta los datos de ejemplo.

#### Con servidor de BD en contenedor Docker

Para no instalar MySQL localmente puede arrancarse un contenedor Docker (los datos se guardarán en un volumen gestionado por Docker llamado `dagss_dbdata`):

```bash
docker run -d --rm --name dagss-mysql \
  -e MYSQL_DATABASE=pruebas_dagss \
  -e MYSQL_USER=dagss \
  -e MYSQL_PASSWORD=dagss \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3307:3306 \
  -v dagss_dbdata:/var/lib/mysql \
  mysql:8.0.46
```

Y probar:

```bash
docker exec -it dagss-mysql mysql -udagss -pdagss --default-character-set=utf8mb4 pruebas_dagss -e "SHOW TABLES;"
```

El proyecto viene configurado para la BD de Docker (**3307**). Si usas un MySQL local en el puerto **3306**, cambia la URL en `persistence.xml` (la línea de local está comentada justo al lado):

```
jdbc:mysql://localhost:3306/pruebas_dagss?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
```

### Ejecutar el proyecto base

```sh
mvn package
mvn exec:java -Dexec.mainClass="es.uvigo.dagss.pedidos.Main"
```

Deberías ver el log SQL de Hibernate y la listaxe de artículos creados. A partir de ahí, empieza el trabajo.

## 3. Revisar las anotaciones JPA existentes

Revisar las anotaciones JPA proporcionadas, teniendo en cuenta lo siguiente en cuanto a las relaciones:

1. Salvo en el caso de la entidad `Cliente`, que usa como clave primaria su DNI, las demás entidades usan claves autogeneradas de tipo `GenerationType.IDENTITY` (necesario para mapear un atributo autoincremental de MySQL).
2. Salvo para la relación entre `Pedido` y su entidad débil `LineaPedido`, nos hemos limitado a relaciones unidireccionales.
    * Para asegurar que ambos extremos de esa relación bidireccional se mantienen consistentes, se incluyen los métodos `anadirLineaPedido()` y `anadirLineaPedidoInterno()` en `Pedido`, que se coordinan con el método `setPedido()` de `LineaPedido` para que en todo momento los dos lados de la relación sean correctos.
    * Más detalles en [JPA implementation patterns: Bidirectional assocations](https://xebia.com/blog/jpa-implementation-patterns-bidirectional-assocations/) y [Object corruption, one side of the relationship is not updated after updating the other side](https://en.wikibooks.org/wiki/Java_Persistence/Relationships#Object_corruption,_one_side_of_the_relationship_is_not_updated_after_updating_the_other_side)
  
3. La relación N:M entre `Articulo` y `Almacen` tiene un atributo propio, `stock`, por lo que se ha modelado esa relación como una entidad adicional `ArticuloAlmacen`, anotada con `@Entity`, empleando dos relaciones unidireccionales `@ManyToOne` hacia `Articulo` y `Almacen`.
    * Para gestionar el campo clave multiatributo se usa la clase auxiliar `ArticuloAlmacenId`, vinculada a la entidad que soporta la relación con atributos mediante la anotación `@IdClass` (un método alternativo sería utilizar `@EmbeddedId`)
    * Más detalles en [Mapping_a_Join_Table_with_Additional_Columns](https://en.wikibooks.org/wiki/Java_Persistence/ManyToMany#Mapping_a_Join_Table_with_Additional_Columns)

## 4. Ejercicios: construir la capa DAO

El patrón **DAO** (_Data Access Object_) encapsula el acceso a datos detrás de una interfaz: el resto de la aplicación no necesita conocer `EntityManager`, JPQL ni los detalles de JPA.

### Ejercicio 1: Interface `GenericoDao<T,K>`

Crea el paquete `es.uvigo.dagss.pedidos.daos` y la interface `GenericoDao<T,K>` con estos métodos:

```java
T crear(T entidad) throws PedidosException;
T actualizar(T entidad) throws PedidosException;
void eliminar(T entidad) throws PedidosException;
T buscarPorClave(K clave) throws PedidosException;
List<T> buscarTodos() throws PedidosException;
Long contarTodos() throws PedidosException;
```

Pistas:
* `T` es el tipo de la entidad y `K` el tipo de su clave.
* Crea también la excepción `PedidosException` (checked) para envolver los errores de persistencia.

### Ejercicio 2: `GenericoDaoJpa<T,K>`: `crear` y `buscarTodos`

Crea la clase `GenericoDaoJpa<T,K>` (que implementa `GenericoDao`) con:
* Constructor `(EntityManager em, Class<T> tipoEntidad)`.
* `crear(T entidad)`: abre transacción, `em.persist(...)`, `commit`; si falla, `rollback` y lanza `PedidosException`.
* `buscarTodos()`: consulta todas las entidades de `tipoEntidad` (con Criteria o con JPQL).

Pistas:
* Guarda `em` y `tipoEntidad` como atributos `protected` para que los hereden los DAOs específicos.
* Sigue siempre el mismo patrón de transacción (`begin` / `commit` / `rollback`).

No implementes todavía el resto de métodos.

### Ejercicio 3: `ClienteDao` y `ClienteDaoJpa`

* `ClienteDao extends GenericoDao<Cliente, String>` (la clave es el DNI).
* `ClienteDaoJpa extends GenericoDaoJpa<Cliente, String> implements ClienteDao`, con un constructor `(EntityManager em)` que llame a `super(em, Cliente.class)`.
* Añade a `Main` una prueba `pruebaClientesConDAO()` que:
  * cree dos `Cliente` (necesitan una `Direccion`),
  * use `crear`, `buscarPorClave`, `buscarTodos` y `contarTodos`.

Comprueba la salida y el SQL que genera Hibernate.

### Ejercicio 4: Resto de métodos y entidades

Completa la capa DAO:

* En `GenericoDaoJpa`: `actualizar` (`merge`), `eliminar` (`remove` + `merge`), `buscarPorClave` (`find`) y `contarTodos` (Criteria `count`).
* `PedidoDao` / `PedidoDaoJpa`:
  * `buscarPorCliente(String dni)` con JPQL.
  * `buscarPorClienteConLineasPedido(String dni)` con `JOIN FETCH` para evitar el problema N+1.
  * `buscarPorClaveConLineasPedido(Long id)` forzando la carga de las líneas dentro de la transacción.
  * Recuerda: la relación `Pedido` → `LineaPedido` tiene `cascade = ALL`; no hace falta un DAO para `LineaPedido`.
* DAOs para `Familia`, `Articulo`, `Almacen` y `ArticuloAlmacen`, con consultas propias (por nombre, por familia, stock, etc.).

### Ejercicio 5: Patrones de diseño y SOLID

Reflexiona sobre qué principios SOLID se están aplicando en tu solución y qué patrones GoF detectas. Como orientación, puedes pensar en la responsabilidad de cada clase, en qué habría que tocar para añadir una entidad nueva y qué dependencias existen.

## 5. Cómo comprobar

Cada vez que hagas un cambio que quieras probar:

```sh
mvn package
mvn exec:java -Dexec.mainClass="es.uvigo.dagss.pedidos.Main"
```

Y:
* Revisa el SQL que imprime Hibernate (`hibernate.show_sql` y `hibernate.format_sql` están activos en `persistence.xml`).
* Consulta la BD:

```sql
mysql -u dagss -p   -- contraseña: dagss

mysql> use pruebas_dagss;
mysql> show tables;
mysql> select * from Cliente;
mysql> select * from Pedido;
```

**Atención:** `persistence.xml` usa `drop-and-create`: el *drop* ocurre **al arrancar** (al crear el `EntityManagerFactory`), no al terminar. Por eso, **después de ejecutar el `Main` los datos están en la BD y puedes consultarlos** sin cambiar nada; se perderán **en la siguiente ejecución**, que volverá a borrar y recrear el esquema.
  Si quieres conservar datos entre ejecuciones (o añadir los tuyos), cambia en `persistence.xml` a:

```xml
<property name="jakarta.persistence.schema-generation.database.action" value="update"/>
```

(o `none`) y comenta `pruebaCrearEntidadesConEM()` para no duplicar los datos de ejemplo.

## 6. Referencias

* [Patrón DAO](https://en.wikipedia.org/wiki/Data_access_object)
* [Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/)
* [Criteria API (Jakarta EE Tutorial)](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-criteria/persistence-criteria.html)
