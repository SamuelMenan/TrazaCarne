# TrazaCarne

Proyecto académico en Java para gestionar inventario por lotes y conservar la trazabilidad de una carnicería, aplicando POO y SOLID.

## Estado actual

El primer bloque implementa productos vendidos por peso o por unidad, validación de cantidades, actualización de precio, desactivación y cálculo de subtotales. `Main` ejecuta una demostración de esas clases.

Clientes, abastecedores, compras, lotes, inventario, ventas y el menú de consola se construirán en las siguientes etapas. Los objetos creados durante esta demostración solo existen mientras el programa se ejecuta.

## Requisitos

- JDK 27, que es la versión actualmente configurada y comprobada en el proyecto.
- IntelliJ con soporte de Maven, o Maven instalado para trabajar desde una terminal.
- Acceso a Maven Central para descargar dependencias la primera vez.

La versión del JDK puede ajustarse si el curso pide otra, alineando el SDK de IntelliJ y `maven.compiler.release` en `pom.xml`.

## Ejecutar en IntelliJ

1. Abrir la carpeta que contiene `pom.xml` como proyecto Maven.
2. Si IntelliJ muestra cambios de Maven pendientes, utilizar **Load Maven Changes** o **Reload All Maven Projects**.
3. Abrir `src/main/java/co/trazacarne/Main.java`.
4. Pulsar el botón verde junto a `main` y elegir **Run 'Main.main()'**.

La demostración muestra:

```text
TrazaCarne: demostración inicial de productos
Carne molida | cantidad: 1.250 | subtotal: 30000.00 COP
Hamburguesa | cantidad: 3 | subtotal: 19500.00 COP
```

## Ejecutar pruebas

En IntelliJ se puede ejecutar `ProductoTest` desde el botón verde o abrir el panel Maven y utilizar **Lifecycle > test**.

Si Maven está disponible en la terminal, ejecutar desde la carpeta de `pom.xml`:

```powershell
mvn test
```

Las pruebas comprueban cantidades por peso y unidad, datos obligatorios, precios inválidos, conservación del precio anterior, redondeo y validación polimórfica.

## Crear y ejecutar el archivo JAR

En IntelliJ utilizar **Maven > Lifecycle > package**, o en una terminal con Maven:

```powershell
mvn package
java -jar target/trazacarne-1.0-SNAPSHOT.jar
```

El JAR utiliza `co.trazacarne.Main` como punto de entrada. JUnit es una dependencia de pruebas y no es necesario para ejecutar la demostración.

## Organización

| Ubicación | Contenido |
|---|---|
| `src/main/java/co/trazacarne/dominio` | Productos y sus reglas de negocio. |
| `src/main/java/co/trazacarne/excepcion` | Motivos de rechazo de operaciones. |
| `src/test/java/co/trazacarne/dominio` | Pruebas de comportamiento del dominio. |
| `docs` | Plan y explicación de la implementación. |
| `target` | Clases compiladas, resultados de pruebas y JAR. |

`target`, `out` y la caché local de verificación `.maven-cache` están excluidos de Git. La caché `.maven-cache` se utilizó para comprobar este proyecto; el uso normal de Maven desde IntelliJ puede utilizar su repositorio predeterminado.

## Documentación para continuar

- [Plan de implementación](docs/PLAN_IMPLEMENTACION.md).
- [Primer bloque: productos y POO](docs/PRIMER_BLOQUE_PRODUCTOS.md).

Referencias técnicas utilizadas para configurar las herramientas:

- [JUnit 5.13.4: guía oficial](https://docs.junit.org/5.13.4/user-guide/index.html).
- [Maven Compiler: configuración de release](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html).
- [Maven Surefire: ejecución con JUnit Platform](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html).
