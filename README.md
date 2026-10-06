# TrazaCarne

Proyecto académico en Java para gestionar inventario por lotes y conservar la trazabilidad de una carnicería, aplicando POO y SOLID.

## Estado actual

La primera versión de consola está implementada: gestión de productos, clientes y abastecedores; compras con lotes y fechas; inventario; ventas con asignación FEFO; anulaciones; pérdidas; movimientos y trazabilidad en ambos sentidos.

`Main` crea y conecta los repositorios y servicios y abre el menú. El inicio normal no carga datos de ejemplo. Los datos solo existen mientras el programa se ejecuta. La opción `--demo` ejecuta un escenario completo con fecha fija sin pedir entradas.

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

Al iniciar aparecen estas opciones:

```text
TrazaCarne | Inventario y trazabilidad
Los datos se conservan durante esta sesión en memoria.

1. Clientes | 2. Abastecedores | 3. Productos
4. Compras | 5. Ventas | 6. Inventario | 7. Trazabilidad | 0. Salir
```

## Ejecutar pruebas

En IntelliJ se puede ejecutar una clase de pruebas desde el botón verde o abrir el panel Maven y utilizar **Lifecycle > test** para ejecutar todas.

Si Maven está disponible en la terminal, ejecutar desde la carpeta de `pom.xml`:

```powershell
mvn test
```

Las 158 comprobaciones actuales cubren el dominio, repositorios, servicios, flujo completo y consola. Incluyen compras sin entradas parciales, FEFO, productos repetidos, precio histórico, devoluciones a lotes vencidos y cierre al agotarse la entrada. Los casos anteriores siguen pasando.

## Crear y ejecutar el archivo JAR

En IntelliJ utilizar **Maven > Lifecycle > package**, o en una terminal con Maven:

```powershell
mvn package
java -jar target/trazacarne-1.0-SNAPSHOT.jar
```

El JAR utiliza `co.trazacarne.Main` como punto de entrada. JUnit es una dependencia de pruebas y no es necesario para ejecutar el programa.

Para una demostración sin introducir datos:

```powershell
java -jar target/trazacarne-1.0-SNAPSHOT.jar --demo
```

La demostración recibe dos lotes de 5 y 20 kg, vende 8 kg repartidos en 5 y 3, muestra el origen y anula la venta restaurando 5 y 20 kg. Utiliza el 5 de octubre de 2026 como fecha fija para funcionar sin depender del día de ejecución.

## Uso básico

1. Registrar un cliente, un abastecedor y un producto.
2. Registrar una compra indicando sus lotes, cantidades y fechas.
3. Consultar inventario y registrar una venta.
4. Consultar el origen de la venta o las ventas y clientes asociados a un lote.
5. Anular la venta, si corresponde, conservando el historial.

Cada submenú tiene `0. Volver`; el menú principal tiene `0. Salir`. Las fechas se escriben como `AAAA-MM-DD`. Las cantidades admiten punto o coma decimal, sin separadores de miles.

## Decisiones de la primera versión

- Cantidades por peso en kilogramos con hasta tres decimales; por unidad, valores enteros positivos.
- Precios positivos con hasta dos decimales; subtotales redondeados con `HALF_UP`.
- Fechas de operación obtenidas del reloj del sistema en `America/Bogota`.
- Un lote se considera vencido desde su fecha de vencimiento; no se recibe mercancía ya vencida.
- FEFO desempata por código y prepara todas las asignaciones antes de descontar.
- Las líneas repetidas de una venta se validan individualmente y luego se agrupan por producto.
- Una venta anulada devuelve al mismo lote, aunque esté vencido o el producto esté inactivo.
- Las pérdidas se registran explícitamente; el vencimiento no borra existencias ni genera pérdidas automáticas.
- El historial puede incluir anuladas; una consulta de ventas vigentes las excluye.
- El historial de movimientos no admite operaciones anteriores a su último instante registrado.

El alcance es una aplicación de consola con datos en memoria y ejecución secuencial. Cambiar el almacenamiento por una base de datos requerirá incorporar transacciones para aplicar coordinadamente los cambios de registros y existencias.

## Organización

| Ubicación | Contenido |
|---|---|
| `src/main/java/co/trazacarne/dominio` | Entidades, cantidades, historial y estados. |
| `src/main/java/co/trazacarne/dominio/estrategia` | Contrato de asignación y estrategia FEFO. |
| `src/main/java/co/trazacarne/servicio` | Coordinación de todos los casos de uso. |
| `src/main/java/co/trazacarne/servicio/dto` | Datos de entrada y resultados de trazabilidad. |
| `src/main/java/co/trazacarne/consola` | Menús, lectura y presentación. |
| `src/main/java/co/trazacarne/repositorio` | Contrato de almacenamiento. |
| `src/main/java/co/trazacarne/repositorio/memoria` | Implementación en memoria. |
| `src/main/java/co/trazacarne/excepcion` | Motivos de rechazo de operaciones. |
| `src/test/java/co/trazacarne` | Pruebas de dominio, repositorios y servicios. |
| `docs` | Plan y explicación de la implementación. |
| `target` | Clases compiladas, resultados de pruebas y JAR. |

`target`, `out` y la caché local de verificación `.maven-cache` están excluidos de Git. La caché `.maven-cache` se utilizó para comprobar este proyecto; el uso normal de Maven desde IntelliJ puede utilizar su repositorio predeterminado.

## Documentación para continuar

- [Índice y orden de lectura por bloques](docs/INDICE_GUIAS.md).
- [Plan de implementación](docs/PLAN_IMPLEMENTACION.md).
- [Primer bloque: productos y POO](docs/PRIMER_BLOQUE_PRODUCTOS.md).
- [Segundo bloque: gestión y repositorios](docs/SEGUNDO_BLOQUE_GESTION.md).
- [Bloque 3: compras y lotes](docs/BLOQUE_03_COMPRAS_Y_LOTES.md).
- [Bloque 4: inventario y FEFO](docs/BLOQUE_04_INVENTARIO_Y_FEFO.md).
- [Bloque 5: ventas y anulación](docs/BLOQUE_05_VENTAS_Y_ANULACION.md).
- [Bloque 6: trazabilidad y consultas](docs/BLOQUE_06_TRAZABILIDAD_Y_CONSULTAS.md).
- [Bloque 7: consola y Main](docs/BLOQUE_07_CONSOLA_Y_MAIN.md).
- [Bloque 8: pruebas y verificación](docs/BLOQUE_08_PRUEBAS_Y_VERIFICACION.md).

Referencias técnicas utilizadas para configurar las herramientas:

- [JUnit 5.13.4: guía oficial](https://docs.junit.org/5.13.4/user-guide/index.html).
- [Maven Compiler: configuración de release](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html).
- [Maven Surefire: ejecución con JUnit Platform](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html).
