# Ejemplo Maven y JPA Básico
> DAGSS 2025-2026, semana 2

* Ejemplo de creación de proyectos Maven y uso desde línea de comandos
* Ejemplo de mapeo JPA e implementación de DAO genérico
* Ejemplo de uso de Criteria API

## PREVIO

**Nota:** En los equipos de laboratorio, puede ser conveniente establecer la variable de entorno `JAVA_HOME`, para que el comando `mvn` (Maven) compile y ejecute los proyectos siempre con el mismo JDK.

 ```sh
 export JAVA_HOME=/usr/lib/jvm/openjdk-20
 
 export PATH=$JAVA_HOME/bin:$PATH
 ```

### Requisitos previos

* Servidor de BD MySQL
* Maven (versión > 3.5.x)
* (opcional) GIT
* (opcional) IDE Java (Eclipse, Netbeans, IntelliJ, VS Code)

### Crear BD para los ejemplos (si no fue creada anteriormente)

* Crear BD `pruebas_dagss` en MySQL

```sql
mysql -u root -p    # pedirá la contraseña de MySQL

mysql> create database pruebas_dagss;
mysql> create user dagss@localhost identified by "dagss";
mysql> grant all privileges on pruebas_dagss.* to dagss@localhost;
```

Adicionalmente, puede ser necesario establecer un formato de fecha compatible
```sql
mysql> set @@global.time_zone = '+00:00';
mysql> set @@session.time_zone = '+00:00';
```

## CREAR Y CONFIGURAR PROYECTO MAVEN
### Crear un proyecto Maven usando el arquetipo `maven-archetype-quickstart` 
```sh
mvn archetype:generate -DgroupId=es.uvigo.dagss \
                       -DartifactId=ejemplo-persistencia \
                       -Dversion=1.0 \
                       -Dpackage=es.uvigo.dagss.pedidos \
                       -DarchetypeArtifactId=maven-archetype-quickstart \
                       -DarchetypeVersion=1.4
                         
cd ejemplo-persistencia                         
```

* Comprobar la estructura de directorios creada con `tree ejemplo-persistencia` o `ls -lR ejemplo-persistencia`
* Ajustar el archivo `pom.xml` generado (el arquetipo `maven-archetype-quickstart` establece por defecto plugins de versiones algo antiguas)
	1. Ajustar la versión de Java a utilizar, emplear al menos Java 11 [la versión de Hibernate utilizada requiere Java 11 o superior]. También puede omitirse la especificación de versiones y usar la que esté instalada por defecto.
    ```xml
      <properties>
        <maven.compiler.target>11</maven.compiler.target>
        <maven.compiler.source>11</maven.compiler.source>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      </properties>
    ```
    2. Eliminar el contenido del elemento `<dependencies>` (manteniendo las etiquetas externas que se rellenarán en el siguiente paso)
    3. Eliminar completamente la sección `<build>` (incluyendo contenido y etiquetas externas). Por defecto, se declaran versiones anticuadas de los plugins.

* Eliminar las clases de ejemplo incluidas por el arquetipo Maven
```sh
rm src/main/java/es/uvigo/dagss/pedidos/App.java

rm src/test/java/es/uvigo/dagss/pedidos/AppTest.java
```

## CONFIGURACIÓN PARA JPA
### Declarar las dependencias necesarias en `pom.xml`
1. Declarar el uso de Hibernate como _provider_ JPA (versión 6.6.2.Final) dentro de `<dependencies>...</dependencies>`

```xml
<project>
   ...
   <dependencies>
      <dependency>
         <groupId>org.hibernate.orm</groupId>
         <artifactId>hibernate-core</artifactId>
         <version>6.6.2.Final</version>
      </dependency>
      ...
   </dependencies>
</project>
```
El resto de dependencias necesarias para `hibernate-core` serán descargadas e instaladas por Maven.

2. Declarar el _connector_ JDBC para MySQL (versión 9.1.0) dentro de `<dependencies>...</dependencies>`

```xml
<project>
   ...
   <dependencies>
      ...
      <dependency>
         <groupId>com.mysql</groupId>
         <artifactId>mysql-connector-j</artifactId>
         <version>9.1.0</version>
      </dependency>
   </dependencies>
</project>
```

## MODELO E-R
![Modelo E-R](doc/modeloER_pedidos.png?raw=true "Modelo E-R del ejemplo")

## AÑADIR ENTIDADES

Crear el directorio para el paquete `entidades` y copiar los ficheros Java con la definición de las entidades (disponibles en [https://github.com/DAGSS-ESEI/practica-2-persistencia/tree/main/src/main/java/es/uvigo/dagss/pedidos/entidades](https://github.com/DAGSS-ESEI/practica-2-persistencia/tree/main/src/main/java/es/uvigo/dagss/pedidos/entidades))

```sh
mkdir -p src/main/java/es/uvigo/dagss/pedidos/entidades

cd src/main/java/es/uvigo/dagss/pedidos/entidades

wget https://raw.githubusercontent.com/DAGSS-ESEI/practica-2-persistencia/main/src/main/java/es/uvigo/dagss/pedidos/entidades/{Articulo,Almacen,ArticuloAlmacen,ArticuloAlmacenId,Familia,Direccion,Cliente,Pedido,LineaPedido,EstadoPedido}.java

popd
```

### Aspectos a revisar

Revisar las anotaciones JPA proporcionadas, teniendo en cuenta lo siguiente en cuanto a las relaciones:

1. Salvo en el caso de la entidad `Cliente`, que usa como clave primaria su DNI, las demás entidades usan claves autogeneradas de tipo `GenerationType.IDENTITY` (necesario para mapear un atributo autoincremental de MySQL).
2. Salvo para la relación entre `Pedido` y su entidad débil `LineaPedido`, nos hemos limitado a relaciones unidireccionales.
    * Para asegurar que ambos extremos de esa relación bidireccional se mantienen consistentes, se incluyen los métodos `anadirLineaPedido()` y `anadirLineaPedidoInterno()` en `Pedido`, que se coordinan con el método `setPedido()` de `LineaPedido` para que en todo momento los dos lados de la relación sean correctos.
    * Más detalles en [JPA implementation patterns: Bidirectional assocations](https://xebia.com/blog/jpa-implementation-patterns-bidirectional-assocations/) y [Object corruption, one side of the relationship is not updated after updating the other side](https://en.wikibooks.org/wiki/Java_Persistence/Relationships#Object_corruption,_one_side_of_the_relationship_is_not_updated_after_updating_the_other_side)
  
3. La relación N:M entre `Articulo` y `Almacen` tiene un atributo propio, `stock`, por lo que se ha modelado esa relación como una entidad adicional `ArticuloAlmacen`, anotada con `@Entity`, empleando dos relaciones unidireccionales `@ManyToOne` hacia `Articulo` y `Almacen`.
    * Para gestionar el campo clave multiatributo se usa la clase auxiliar `ArticuloAlmacenId`, vinculada a la entidad que soporta la relación con atributos mediante la anotación `@IdClass` (un método alternativo sería utilizar `@EmbeddedId`)
    * Más detalles en [Mapping_a_Join_Table_with_Additional_Columns](https://en.wikibooks.org/wiki/Java_Persistence/ManyToMany#Mapping_a_Join_Table_with_Additional_Columns)

### Configurar Persistence Unit de JPA

Definir fichero `persistence.xml` en `src/main/resources/META-INF`

```sh
mkdir -p src/main/resources/META-INF
nano src/main/resources/META-INF/persistence.xml
```

Contenido a incluir
```xml
<?xml version="1.0" encoding="UTF-8"?>

<persistence version="3.0" 
              xmlns="https://jakarta.ee/xml/ns/persistence"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence 
             https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd">


  <persistence-unit name="pedidos_PU" transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    
    <class>es.uvigo.dagss.pedidos.entidades.Almacen</class>
    <class>es.uvigo.dagss.pedidos.entidades.Articulo</class>
    <class>es.uvigo.dagss.pedidos.entidades.ArticuloAlmacen</class>
    <class>es.uvigo.dagss.pedidos.entidades.Cliente</class>
    <class>es.uvigo.dagss.pedidos.entidades.Familia</class>
    <class>es.uvigo.dagss.pedidos.entidades.LineaPedido</class>
    <class>es.uvigo.dagss.pedidos.entidades.Pedido</class>
    
    <properties>
      <property name="jakarta.persistence.jdbc.url" value="jdbc:mysql://localhost:3306/pruebas_dagss?serverTimezone=UTC"/>
      <property name="jakarta.persistence.jdbc.user" value="dagss"/>
      <property name="jakarta.persistence.jdbc.password" value="dagss"/>
      <property name="jakarta.persistence.jdbc.driver" value="com.mysql.cj.jdbc.Driver"/>
      <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create"/>
      
      <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL8Dialect"/>
      <property name="hibernate.cache.provider_class" value="org.hibernate.cache.NoCacheProvider"/>
      <property name="hibernate.show_sql" value="true"/>
      <property name="hibernate.format_sql" value="true"/>
    </properties>
  </persistence-unit>

</persistence>
```
**Nota:** Se usa el valor `drop-and-create` para el parámetro `jakarta.persistence.schema-generation.database.action` lo que hace que en cada ejecución se eliminen los contenidos de la BD y se cree una nueva BD vacía.
   * Esta configuración sólo tiene sentido en tiempo de desarrollo y si se desea que el motor JPA refleje los cambios realizados en el mapeo de las entidades.
   * Con una BD preexistente, o una vez definidas las entidades definitivas, se suele usar el valor `none` para no alterar la BD.

## DAO JPA Genérico

Más detalles sobre el [Patrón DAO (_Data Access Object_)](https://en.wikipedia.org/wiki/Data_access_object)

### Crear interface y clase del DAO

Crear el directorio para el paquete `daos` y copiar los ficheros Java con la definición de interfaces y clases de implementación.

```sh
mkdir -p src/main/java/es/uvigo/dagss/pedidos/daos
cd src/main/java/es/uvigo/dagss/pedidos/daos

wget https://raw.githubusercontent.com/DAGSS-ESEI/practica-2-persistencia/main/src/main/java/es/uvigo/dagss/pedidos/daos/{GenericoDAO,GenericoDAOJPA,ClienteDAO,ClienteDAOJPA,PedidoDAO,PedidoDAOJPA,PedidosException}.java

popd
```

#### Aspectos a revisar
1. Comprobar el interface _GenericoDAO_ con la definición de un DAO genérico (parametrizando la clase de la entidad y la de la clave) y su implementación _GenericoDAOJPA_ (recibe un _EntityManager_ en el constructor)
2. Comprobar los interfaces _ClienteDAO_ y _PedidoDAO_ 
	* Heredan de _GenericoDAO_ especificando los tipos correspondientes de entidad y clave
	* Añaden métodos adicionales (métodos de consulta) específicos para cada entidad
3. Revisar las implementaciones de estos interfaces:
	* _ClienteDAOJPA_: hereda de _GenericoDAOJPA_ e implementa _ClienteDAO_
	* _PedidoDAOJPA_: hereda de _GenericoDAOJPA_ e implementa _PedidoDAO_
4. Comprobar en _PedidoDAOJPA_ la gestión de la relación 1:N con _LineaPedido_:
   * La relación está mapeada usando `cascade = CascadeType.ALL` por lo que las operaciones sobre _Pedido_ se propagan a _LineaPedido_ y no es necesario emplear un DAO específico para _LineaPedido_


## PROBAR EL PROYECTO
### Añadir clase con "main()" de ejemplo
```sh
cd src/main/java/es/uvigo/dagss/pedidos/

wget https://raw.githubusercontent.com/DAGSS-ESEI/practica-2-persistencia/main/src/main/java/es/uvigo/dagss/pedidos/Main.java

popd
```

#### Aspectos a revisar
1. Se crea un _EntityManagerFactory_ al que se le pedirá la creación de los respectivos _EntityManager_ con los que se realizarán las operaciones sobre la base de datos
2. Las operaciones realizadas por el _EntityManager_ sobre la base de datos se realizan dentro de una _transacción_ proporcionada por el _EntityManager_.
3. La transacción es iniciada con `tx.begin()` y completada con éxito al hacer `tx.commit()`. En caso de que durante la ejecución de esas acciones se lance alguna excepción, se ejecuta `tx.rollback()` para omitir los cambios realizados (es decir, se descartan las entidades pendientes de sincronizar del _Persistence Context_ ligado a dicha transacción).
4. Se incluye un ejemplo de _Criteria Queries_ (en `pruebaConsultaCriteriaQuery()`) [más [detalles](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-criteria/persistence-criteria.html) en JEE Tutorial]
5. Dado que en `persistence.xml` se ha indicado la opción `drop-and-create`, en cada ejecución de estas clases de ejemplo, se eliminan los contenidos de la base de datos y se **crea** una nueva **BD vacía**.

## Ejecutar clase `Main` proporcionada
```sh
mvn package
mvn exec:java -Dexec.mainClass="es.uvigo.dagss.pedidos.Main"
```
**Nota:** En `persistence.xml` se habilita el LOG de las consultas SQL emitidas por Hibernate (`hibernate.show_sql` y `hibernate.format_sql`). 
- En la salida del ejecutable se mezclan las sentencias DDL de creación de las tablas de la BD, las sentencias SQL con las consultas concretas y la salida del propio programa al mostrar las entidades recuperadas de la BD (marcada con `[PRUEBA JPA]:`).

**Comprobar el estado de la BD:** ver las tablas creadas y el contenido de las mismas

```sql
mysql -u dagss -p   # con contraseña dagss

mysql> use pruebas_dagss;
mysql> show tables;
mysql> describe Articulo;                # los nombres de las tablas pueden ser diferentes
mysql> describe Pedido;
mysql> select * from Articulo;
mysql> select * from Pedido;
mysql> ...

```

## TAREA EXTRA
- Comprobar la importación y uso del proyecto creado desde el IDE Java empleado habitualmente.
- Implementar consultas adicionales con JPQL y Criteria API: búsqueda de pedidos dentro de un rango de fechas y cuyo importe total supere un valor mínimo.

## Proyecto final resultante

Disponible en Github: [https://github.com/DAGSS-ESEI/practica-2-persistencia](https://github.com/DAGSS-ESEI/practica-2-persistencia)

```sh
git clone https://github.com/DAGSS-ESEI/practica-2-persistencia
```
