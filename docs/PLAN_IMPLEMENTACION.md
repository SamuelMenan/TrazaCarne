# TrazaCarne: plan de implementación en Java

Este documento organiza la construcción de TrazaCarne a partir de sus requisitos, reglas de negocio, historias de usuario y diagramas. Explica qué implementar primero, por qué hacerlo en ese orden, de dónde sale cada parte del código y cómo comprobar que funciona.

Está pensado como una guía de trabajo y aprendizaje de Programación Orientada a Objetos (POO) y SOLID. Las etapas se pueden completar en varias sesiones. Una etapa se considera terminada por su comportamiento verificable, no por la cantidad de archivos creados.

**Fecha de elaboración:** 5 de octubre de 2026.

## 1. Objetivo y alcance

La primera versión permitirá trabajar con una sola carnicería y realizar este flujo:

```text
Registrar abastecedor, cliente y producto
                |
                v
Registrar compra y sus lotes
                |
                v
Consultar existencias y registrar una venta
                |
                v
Consultar origen y destino del producto
                |
                v
Anular la venta conservando el historial
```

También se implementarán consultas de inventario, próximos vencimientos, movimientos y registro de pérdidas, según la prioridad acordada.

La aplicación se desarrollará en Java, con interfaz de consola y almacenamiento inicial en memoria. Maven organizará la compilación, las dependencias y las pruebas. IntelliJ será el editor de trabajo.

Spring Boot, una interfaz web, una base de datos, varias carnicerías, pedidos previos y reportes avanzados quedan fuera de este plan inicial. La separación de responsabilidades facilitará incorporar otras formas de interacción o almacenamiento en el futuro.

### Fuentes del diseño

El plan se basa en los documentos revisados durante la conversación:

- `TrazaCarne_documento.pdf`: problema, alcance, RF-01 a RF-07, RNF-01 a RNF-05, RN-01 a RN-11, HU-01 a HU-08 y criterios de aceptación.
- `TrazaCarne_Diagramas.docx`: casos de uso, modelo de dominio, modelos de datos, clases UML y secuencias de compra, venta, anulación y trazabilidad.

Las decisiones propuestas en este plan se distinguen de las reglas ya definidas. Cuando una decisión cambie el comportamiento o una firma del diseño, se actualizará el diagrama o se dejará registrada la explicación correspondiente.

## 2. Punto de partida del proyecto

Al preparar inicialmente este documento se verificó lo siguiente:

- El proyecto Maven está dentro de la carpeta `TrazaCarne/TrazaCarne` del espacio de trabajo.
- El paquete raíz utilizado es `co.trazacarne`.
- Los paquetes de dominio, estrategia, servicios, repositorios, memoria, consola y excepciones ya están creados.
- Existen paquetes iniciales de pruebas para dominio y servicios.
- El único archivo Java es `Main.java`, con el ejemplo generado por IntelliJ.
- `pom.xml` tiene `org.example` como `groupId` y configura la compilación para Java 27.
- Las clases del negocio y sus pruebas todavía están pendientes.

### Avance de la primera sesión

Se prepararon Maven y JUnit, se comprobó el JDK 27 configurado y se implementaron `Producto`, `ProductoPorPeso`, `ProductoPorUnidad`, `ReglaNegocioException` y `CantidadInvalidaException`. `Main` ejecuta una demostración de subtotales. La compilación, las 46 comprobaciones de dominio y la generación del JAR finalizaron correctamente.

Las decisiones adoptadas sobre cantidades, precios y redondeo, junto con su explicación, están en [Primer bloque: productos y POO](PRIMER_BLOQUE_PRODUCTOS.md). Las instrucciones de ejecución están en [README](../README.md). La demostración del JAR se ejecutó y produjo los subtotales esperados: `30000.00 COP` para 1.250 kg y `19500.00 COP` para tres unidades.

### Avance de la segunda sesión

La etapa 3 quedó implementada: `Cliente`, `Abastecedor`, `TipoAbastecedor`, `FormaVenta`, repositorio genérico en memoria y servicios de clientes, abastecedores y productos. Se añadieron errores específicos de identificador duplicado, registro inexistente y registro inactivo.

Los servicios normalizan los identificadores, rechazan duplicados activos e inactivos, consultan, actualizan y desactivan conservando los registros. `Main` entrega los repositorios mediante interfaces y demuestra una desactivación. Las 112 comprobaciones, la compilación y la ejecución del JAR finalizaron correctamente.

El contrato distingue `guardar` un registro nuevo de `actualizar` uno existente. Las consultas de registros inactivos siguen disponibles; `consultarActivo` permite exigir su disponibilidad para nuevas operaciones. La explicación está en [Segundo bloque: gestión y repositorios](SEGUNDO_BLOQUE_GESTION.md).

Los tipos relacionados con ventas y movimientos se crearán cuando se implemente la primera clase que los necesite. Las decisiones de fechas y trazabilidad permanecen pendientes para sus etapas.

### Entrega de los bloques restantes: 6 de octubre de 2026

La primera versión de consola quedó implementada con compras, lotes, inventario, FEFO, ventas, anulación, trazabilidad, pérdidas y movimientos. Se completaron los tipos y errores pendientes y se incorporaron DTO de entrada y salida. El arranque normal abre un menú real; la opción `--demo` ejecuta el flujo reproducible de 5 + 3 kg.

Cada entidad conserva sus propias validaciones, según la decisión acordada. La fecha de operación procede del reloj entregado a los servicios; en uso normal se emplea `America/Bogota` y las pruebas utilizan relojes fijos. La recepción rechaza lotes vencidos, exige procesamiento no posterior a la recepción y conserva sacrificio cuando corresponde. El historial exige un orden temporal no retroactivo. Las pérdidas se registran explícitamente y las anuladas pueden consultarse con su estado.

Las 158 comprobaciones integradas finalizaron sin fallos ni errores. Las nuevas guías detalladas por archivo están en el [índice de bloques](INDICE_GUIAS.md). Allí también se describen los cambios que deberán reflejarse en los diagramas originales: DTO, estado de borrador y asignación FEFO sin descuento inmediato.

Crear paquetes prepara la organización del código. Las reglas de POO y SOLID se demostrarán al implementar las clases y sus relaciones.

## 3. De dónde sale el código

La documentación se transforma en código mediante decisiones sobre responsabilidades.

| Fuente | Qué ayuda a descubrir | Ejemplo en TrazaCarne |
|---|---|---|
| Problema y alcance | Qué debe resolver la primera versión | Conservar el origen y destino de la carne. |
| Modelo de dominio | Conceptos y relaciones del negocio | Una compra contiene lotes; una venta tiene detalles. |
| Información requerida | Atributos que deben conservarse | Código de lote, cantidad, procesamiento y vencimiento. |
| Reglas de negocio | Validaciones y comportamiento | Evitar cantidades negativas y ventas de lotes vencidos. |
| Historias de usuario | Operaciones que necesita el vendedor | Registrar compra, registrar venta, anular y consultar. |
| Diagrama de clases | Responsabilidades, contratos y tipos iniciales | `Producto`, `Inventario`, `Repositorio` y servicios. |
| Diagramas de secuencia | Orden de colaboración entre objetos | Buscar cliente, preparar detalles, asignar lotes y registrar. |
| Criterios de aceptación | Escenarios verificables | La segunda anulación debe rechazarse. |

### Ejemplo: de una regla a un método y una prueba

RN-07 establece que el inventario nunca debe quedar negativo.

1. **Responsabilidad:** proteger la cantidad disponible de un lote.
2. **Clase:** `Lote`, porque conoce su propia cantidad.
3. **Atributo:** `cantidadDisponible`, privado.
4. **Operación:** `descontar(cantidad)`.
5. **Validaciones:** cantidad positiva, forma de venta válida y disponibilidad suficiente.
6. **Resultado válido:** se reduce la cantidad disponible.
7. **Resultado inválido:** se comunica el motivo y se conserva la cantidad anterior.
8. **Prueba:** un lote con 5 kg rechaza un descuento de 6 kg y sigue teniendo 5 kg.

El código no surge solo de copiar nombres de un diagrama. Cada método debe expresar una responsabilidad y proteger las condiciones que le corresponden.

### Preguntas antes de crear una clase

- ¿Qué representa y qué responsabilidad tiene?
- ¿Qué información necesita conservar?
- ¿Qué reglas puede comprobar con esa información?
- ¿Con qué otros objetos colabora?
- ¿En qué requisito o necesidad técnica se justifica?
- ¿Cómo se comprobará su comportamiento?

Un sustantivo del documento puede sugerir una clase, pero no obliga a crearla. Por ejemplo, el vendedor es el actor que utiliza la consola; el alcance actual no exige desarrollar un módulo de usuarios y autenticación.

## 4. Organización de carpetas y paquetes

La siguiente es la estructura prevista al completar la implementación. Las clases se crearán conforme se necesiten en las etapas, no todas como archivos vacíos al comienzo.

```text
TrazaCarne/
├── docs/
│   └── PLAN_IMPLEMENTACION.md
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── co/trazacarne/
│   │   │       ├── Main.java
│   │   │       ├── dominio/
│   │   │       │   ├── Producto.java
│   │   │       │   ├── ProductoPorPeso.java
│   │   │       │   ├── ProductoPorUnidad.java
│   │   │       │   ├── Cliente.java
│   │   │       │   ├── Abastecedor.java
│   │   │       │   ├── Compra.java
│   │   │       │   ├── Lote.java
│   │   │       │   ├── Venta.java
│   │   │       │   ├── DetalleVenta.java
│   │   │       │   ├── AsignacionLote.java
│   │   │       │   ├── Inventario.java
│   │   │       │   ├── MovimientoInventario.java
│   │   │       │   ├── TipoAbastecedor.java
│   │   │       │   ├── TipoMovimiento.java
│   │   │       │   ├── EstadoVenta.java
│   │   │       │   └── estrategia/
│   │   │       │       ├── EstrategiaAsignacion.java
│   │   │       │       └── EstrategiaFEFO.java
│   │   │       ├── servicio/
│   │   │       │   ├── ServicioProductos.java
│   │   │       │   ├── ServicioClientes.java
│   │   │       │   ├── ServicioAbastecedores.java
│   │   │       │   ├── ServicioCompras.java
│   │   │       │   ├── ServicioVentas.java
│   │   │       │   ├── ServicioInventario.java
│   │   │       │   └── ServicioTrazabilidad.java
│   │   │       ├── repositorio/
│   │   │       │   ├── Repositorio.java
│   │   │       │   └── memoria/
│   │   │       │       └── RepositorioEnMemoria.java
│   │   │       ├── consola/
│   │   │       │   └── MenuConsola.java
│   │   │       └── excepcion/
│   │   │           ├── ReglaNegocioException.java
│   │   │           ├── CantidadInvalidaException.java
│   │   │           ├── FechasInvalidasException.java
│   │   │           ├── IdentificadorDuplicadoException.java
│   │   │           ├── RegistroInactivoException.java
│   │   │           ├── StockInsuficienteException.java
│   │   │           └── VentaYaAnuladaException.java
│   │   └── resources/
│   └── test/
│       └── java/
│           └── co/trazacarne/
│               ├── dominio/
│               ├── servicio/
│               └── repositorio/
└── README.md
```

Los paquetes de pruebas adicionales se crearán al necesitar casos que correspondan a ellos. `README.md` explicará cómo ejecutar el proyecto y sus pruebas; este plan explicará cómo construirlo.

### Responsabilidad de cada paquete

| Paquete | Responsabilidad | Ejemplo de cambio que le corresponde |
|---|---|---|
| `dominio` | Representar el negocio y proteger sus reglas | Cambiar la interpretación de vencimiento. |
| `dominio.estrategia` | Definir y aplicar la política de asignación de lotes | Incorporar otra prioridad de salida. |
| `servicio` | Coordinar casos de uso y acceso a registros | Registrar una venta de varios productos. |
| `repositorio` | Definir contratos para guardar y consultar | Establecer la búsqueda por identificador. |
| `repositorio.memoria` | Implementar almacenamiento en memoria | Usar un mapa para conservar registros. |
| `consola` | Leer datos y presentar resultados | Mostrar un mensaje o añadir una opción. |
| `excepcion` | Expresar fallos de reglas de negocio | Informar que la venta ya fue anulada. |

## 5. Cómo colaborarán las partes

```text
Vendedor
   |
   v
MenuConsola
   |
   v
Servicios ------> Contratos de repositorio
   |                         ^
   v                         |
Dominio              Implementación en memoria
```

`Main` creará y conectará esos objetos al arrancar el programa.

- La consola llamará a los servicios.
- Los servicios coordinarán objetos del dominio y utilizarán interfaces de repositorio.
- El dominio protegerá sus reglas sin depender de la consola.
- La implementación en memoria cumplirá los contratos de almacenamiento.
- Una regla como la cantidad válida podrá comprobarse sin ejecutar un menú.

Los servicios tienen responsabilidades que requieren ver varios registros: por ejemplo, verificar que un NIT no esté repetido. Los objetos del dominio tienen responsabilidades sobre su propio estado: por ejemplo, que el NIT no esté vacío.

## 6. Decisiones pendientes antes de las operaciones críticas

Estas propuestas requieren quedar documentadas al implementarlas. No se presentan como reglas que ya estaban aprobadas en los documentos originales.

| Tema | Propuesta inicial | Por qué importa |
|---|---|---|
| Unidad de peso | Kilogramos en toda la primera versión | Evita mezclar gramos y kilogramos. |
| Precisión del peso | Hasta tres decimales | Permite representar milésimas de kilogramo. |
| Cantidades y dinero | `BigDecimal` | Permite trabajar con cantidades decimales exactas. |
| Precio | Definir moneda y aceptar hasta dos decimales | Evita interpretaciones distintas de un importe. |
| Redondeo | Documentar el modo y el momento del redondeo | Asegura totales reproducibles. |
| Precio válido | Concretar si se exige mayor que cero | El documento pide precio, pero no precisa su límite. |
| Vencimiento | Propuesta: vencido desde su fecha de vencimiento | Determina si se puede vender ese mismo día. |
| Fecha de operación | Recibir una fecha consistente en las operaciones | Facilita comprobar casos con fechas fijas. |
| Desempate FEFO | Ordenar por vencimiento y luego por código | Hace predecible el reparto entre lotes empatados. |
| Producto repetido | Consolidar en un solo detalle por producto | Coincide con el modelo de datos y evita validar líneas aisladamente. |
| Anulación con lote vencido | Reponer al lote original y excluirlo de lo vendible | Respeta devolución y vencimiento. |
| Ventas anuladas en consultas | Mostrar estado en el historial; filtrar para consultas vigentes | Conserva trazabilidad sin confundir ventas actuales. |
| Datos inexistentes | Definir respuestas y mensajes claros | Evita errores técnicos visibles para el vendedor. |

También debe concretarse qué sucede al recibir mercancía ya vencida y si se permiten fechas futuras de procesamiento o sacrificio. La coherencia entre las fechas del lote ya está definida; la relación con la fecha de recepción requiere una decisión adicional.

Para usar `BigDecimal`, se evitará convertir importes desde `double`. La entrada textual puede convertirse directamente, por ejemplo con `new BigDecimal("1.250")`. Las comparaciones numéricas deben tener en cuenta que valores como `1.0` y `1.00` representan la misma cantidad aunque tengan distinta escala.

## 7. Etapas de implementación

### Etapa 0. Preparar y verificar el proyecto

**Objetivo:** disponer de una base que compile, ejecute y permita comprobar el negocio.

**Trabajo previsto:**

- Confirmar el JDK requerido por el curso y alinear IntelliJ y Maven.
- Revisar el `groupId` de `pom.xml` y ajustar la identificación del proyecto.
- Configurar una biblioteca de pruebas, como JUnit, y su ejecución con Maven.
- Sustituir el ejemplo de IntelliJ por un arranque sencillo en `Main`.
- Preparar las instrucciones básicas de ejecución.

**Criterio de finalización:** el proyecto compila, `Main` ejecuta y el sistema de pruebas puede correr un caso sencillo.

La preparación de la herramienta de pruebas será breve. Las pruebas importantes vendrán de las reglas del negocio.

### Etapa 1. Tipos básicos y errores de negocio

**Objetivo:** representar categorías y fallos de forma consistente.

**Clases iniciales:** `TipoAbastecedor`, `EstadoVenta`, `TipoMovimiento` y `ReglaNegocioException`.

Las excepciones específicas se añadirán cuando la primera regla que las necesita se implemente. Esto incluye cantidades inválidas, fechas inválidas, duplicados, registros inactivos, stock insuficiente y ventas ya anuladas.

**Origen:** categorías presentes en los diagramas y RNF-05, que exige mensajes comprensibles cuando una operación no puede realizarse.

**Criterio de finalización:** los estados y categorías se representan sin textos ambiguos, y una regla incumplida puede comunicarse con un motivo claro.

### Etapa 2. Productos y validación de cantidades

**Objetivo:** implementar el primer bloque de POO con comportamiento verificable.

**Clases:** `Producto`, `ProductoPorPeso` y `ProductoPorUnidad`.

`Producto` será abstracto y conservará los datos comunes: código, nombre, especie, corte, precio y estado activo. Los identificadores se protegerán frente a cambios que rompan su relación con otros registros.

Las variantes implementarán `validarCantidad()` de acuerdo con su forma de venta. La validación formará parte de un contrato explícito: la cantidad debe ser positiva y válida para el producto concreto.

Los métodos para actualizar precio o desactivar expresarán operaciones permitidas. No se generarán setters para todos los atributos de forma automática.

**Origen:** RF-03, HU-03, RN-02 y RN-06.

**Comprobaciones:**

- Un producto por peso acepta `1.250`, según la precisión acordada.
- Un producto por unidad acepta `3` y rechaza `3.5`.
- `3.0` también representa tres unidades: la validación comprobará el valor entero, no solo la cantidad de decimales escritos.
- Ambos rechazan cero y valores negativos.
- Un cambio de precio respeta la regla acordada para importes.
- La validación se invoca mediante una referencia `Producto`, sin preguntar su variante.

**Criterio de finalización:** las cantidades se validan polimórficamente y el objeto conserva un estado válido.

### Etapa 3. Clientes, abastecedores y repositorios en memoria

**Objetivo:** disponer de los participantes y productos necesarios para compras y ventas.

**Clases:** `Cliente`, `Abastecedor`, `Repositorio`, `RepositorioEnMemoria`, `ServicioClientes`, `ServicioAbastecedores` y `ServicioProductos`.

Los objetos comprobarán sus datos obligatorios. Los servicios verificarán duplicados y coordinarán registro, consulta, actualización y desactivación.

Antes de implementar `Repositorio`, se definirá su contrato: qué devuelve una búsqueda sin resultado, cómo identifica cada entidad, cómo registra un elemento nuevo y cómo actualiza uno existente. Un registro nuevo no debe reemplazar silenciosamente otro con el mismo identificador.

También se definirá qué sucede al intentar actualizar un identificador desconocido. La propuesta es comunicar que el registro no existe y conservar los datos anteriores, en lugar de insertar uno de forma implícita.

Las instancias de repositorio para clientes, productos y abastecedores tendrán sus tipos y datos separados, aunque compartan la implementación genérica del diseño.

Las desactivaciones conservarán los registros. Una identidad desactivada seguirá contando al comprobar duplicados y mantendrá sus relaciones históricas.

**Origen:** RF-01 a RF-03, HU-01 a HU-03, RN-01, RN-02 y RNF-04.

**Comprobaciones:** registro válido, identificador vacío, duplicado, búsqueda existente e inexistente, actualización y desactivación sin pérdida del historial.

**Criterio de finalización:** podemos crear y consultar abastecedores, clientes y productos mediante servicios que reciben interfaces de repositorio por constructor.

### Etapa 4. Lotes, compras y entradas de inventario

**Objetivo:** incorporar mercancía con origen conocido y cantidades controladas.

**Clases:** `Lote`, `Compra`, `MovimientoInventario`, `ServicioCompras` y el comportamiento inicial de `Inventario`.

`Lote` conservará producto, compra de origen, cantidades y fechas. Mediante la compra podrá conocerse el abastecedor. Las fechas y la relación de origen se protegerán frente a modificaciones posteriores.

La construcción comprobará fechas coherentes y cantidad válida. La fecha de sacrificio será obligatoria cuando el abastecedor sea un matadero. La cantidad recibida y la cantidad disponible tendrán significados distintos: la primera registra la recepción; la segunda cambia con los movimientos.

`ServicioCompras` comprobará número, participantes, contenido y códigos de lote. Se comprobarán duplicados tanto entre los lotes recibidos como frente a todos los lotes registrados anteriormente.

Toda la compra se validará antes de incorporar lotes y movimientos. También se protegerá frente a un segundo registro que duplicaría existencias.

`MovimientoInventario` conservará los hechos del movimiento: identidad, lote, tipo, cantidad, fecha y motivo cuando corresponda. Un movimiento registrado será inmutable y tendrá una referencia identificable a la operación que lo originó cuando sea necesaria para auditarla.

**Origen:** RF-04, HU-04, RN-01, RN-03 a RN-06 y RN-10.

**Comprobaciones:** compra válida, compra vacía, fecha inválida, sacrificio ausente para matadero, cantidad inválida y código repetido. Un rechazo debe dejar compras, lotes y movimientos sin cambios.

**Criterio de finalización:** una compra válida incorpora sus lotes una sola vez y deja los movimientos de entrada correspondientes.

### Etapa 5. Existencias vendibles y asignación FEFO

**Objetivo:** saber cuánto puede venderse y de qué lotes debe salir.

**Clases:** ampliar `Inventario` y crear `EstrategiaAsignacion` y `EstrategiaFEFO`.

La consulta de existencias distinguirá la cantidad registrada en un lote de su disponibilidad para venta. Un lote vencido puede conservar cantidad registrada y movimientos históricos, aunque no aporte existencias vendibles.

FEFO filtrará los lotes elegibles del producto, descartará los que no pueden utilizarse, aplicará el orden acordado y calculará cuánto tomar de cada uno.

**Ajuste propuesto al diseño:** la estrategia devolverá un plan de asignaciones antes de descontar. La firma inicial del diagrama, que realiza asignación y descuento directamente, deberá reflejar este ajuste. Separar preparación y aplicación facilita garantizar que una venta se acepte completa o se rechace sin cambios.

**Origen:** RN-07, RN-08 y RN-09.

**Comprobaciones:**

- Una solicitud de 8 kg utiliza 5 kg del lote que vence primero y 3 kg del siguiente.
- Los lotes vencidos no participan.
- El desempate entre vencimientos iguales es reproducible.
- Stock insuficiente produce un rechazo.
- Preparar asignaciones no altera cantidades ni movimientos.

**Criterio de finalización:** se obtiene un reparto completo y válido, o se informa que la solicitud no puede satisfacerse.

### Etapa 6. Registro completo de ventas

**Objetivo:** vender varios productos conservando precio y origen de cada cantidad.

**Clases:** `Venta`, `DetalleVenta`, `AsignacionLote` y `ServicioVentas`.

`DetalleVenta` conservará producto, cantidad y precio utilizado. Ese precio será una copia del valor aplicado en la operación, para que una actualización posterior del producto no altere ventas anteriores.

`AsignacionLote` conservará lote y cantidad tomada. Las asignaciones deben pertenecer al producto del detalle y sumar exactamente su cantidad. Los detalles y asignaciones de una venta registrada estarán protegidos frente a cambios posteriores.

El registro seguirá este orden:

1. Comprobar identidad de la venta y existencia del cliente.
2. Obtener productos y comprobar que se permiten nuevas operaciones con ellos.
3. Consolidar las líneas repetidas de cada producto.
4. Validar cantidades y preparar los detalles con sus precios.
5. Calcular todas las asignaciones necesarias.
6. Comprobar que toda la venta puede completarse.
7. Aplicar descuentos y registrar movimientos de salida.
8. Conservar la venta completa con su estado e historial.

`REGISTRADA` significará que ya se aplicaron sus descuentos y movimientos. Los datos preparados antes de ese momento todavía no representan una venta registrada; cambiar el estado por sí solo no completa la operación.

En la primera versión de consola no se requiere desarrollar concurrencia. Sí se requiere que un fallo esperado de validación no deje una operación a medias. La aplicación de cambios utilizará información ya validada y deberá evitar efectos parciales.

**Origen:** RF-05, HU-05 y RN-05 a RN-10.

**Comprobaciones:** venta válida, venta vacía, participante inactivo, cantidad inválida, reparto entre lotes, stock insuficiente y número repetido.

Se añadirán dos casos importantes: dos líneas de 6 kg con solo 10 kg disponibles deben rechazarse; una venta de varios productos con stock insuficiente en el último debe conservar intactos todos los lotes y movimientos.

**Criterio de finalización:** una venta se registra completa y una sola vez, o se rechaza sin cambios. El precio histórico permanece fijo.

### Etapa 7. Anulación de ventas

**Objetivo:** revertir el efecto de una venta sin borrar los hechos registrados.

**Clases:** ampliar `Venta`, `Inventario` y `ServicioVentas`.

La anulación verificará que la venta está registrada y no ha sido anulada. Utilizará las asignaciones originales, repondrá cada cantidad al mismo lote y registrará movimientos de devolución. Después conservará el estado `ANULADA`.

`ANULADA` significará que se completó esa única devolución, no que simplemente se cambió un valor del estado.

Una desactivación posterior del cliente o producto no debe impedir consultar o anular una venta histórica. Una devolución a un lote vencido no lo convertirá en vendible.

**Origen:** HU-06, RN-10 y RN-11.

**Comprobaciones:** primera anulación, segunda anulación, devolución a varios lotes, conservación de salida y devolución, y devolución a un lote vencido.

**Criterio de finalización:** la primera anulación devuelve exactamente las cantidades originales; una segunda no cambia cantidades ni genera movimientos adicionales.

### Etapa 8. Consultas de trazabilidad

**Objetivo:** reconstruir origen y destino utilizando las relaciones guardadas.

**Clase:** `ServicioTrazabilidad`.

El recorrido desde una venta será:

```text
Venta -> DetalleVenta -> AsignacionLote -> Lote -> Compra -> Abastecedor
```

Desde un lote se localizarán sus asignaciones, ventas y clientes. La consulta podrá mostrar cantidades y estado de las ventas para que el resultado sea comprensible y no se limite a una lista de nombres.

La trazabilidad debe conservarse desde las etapas de compra y venta. Esta etapa implementa su consulta, no reconstruye información que se haya omitido anteriormente.

Se concretará la distinción entre consulta histórica y consulta de ventas vigentes. Si se conserva historial de anuladas, la figura de secuencia que las excluye debe ajustarse para representar el comportamiento elegido.

**Origen:** RF-07, HU-08, RN-03, RN-09 y RN-10.

**Comprobaciones:** origen de una venta con varios lotes, ventas y clientes de un lote, participantes desactivados, identificadores inexistentes y ventas anuladas con estado visible según la consulta.

**Criterio de finalización:** el origen y destino pueden identificarse con registros conservados, sin deducirlos a partir del inventario actual.

### Etapa 9. Consultas de inventario y pérdidas

**Objetivo:** completar la información operativa sobre existencias y movimientos.

**Clases:** ampliar `Inventario` y crear `ServicioInventario`.

Se ofrecerán existencias por producto y lote, próximos vencimientos y movimientos. Las consultas indicarán si muestran cantidades totales registradas o cantidades vendibles.

El registro de pérdidas validará cantidad y lote, reducirá sus existencias y dejará un movimiento `PERDIDA` con su motivo. Se concretará si un lote vencido genera una pérdida automática o si el vendedor debe registrarla; el alcance inicial favorece el registro explícito para evitar cambios silenciosos.

**Origen:** RF-06, HU-07, RN-07 y RN-10.

**Prioridad:** HU-07 es `Should have`; el control básico de existencias que necesita la venta ya se habrá implementado en etapas anteriores.

**Comprobaciones:** consulta por lote y producto, ventana de próximos vencimientos, pérdida válida, pérdida superior a lo disponible e historial de movimientos.

**Criterio de finalización:** el sistema explica cuánto queda y qué entradas, salidas, devoluciones y pérdidas produjeron ese resultado.

### Etapa 10. Consola y conexión en Main

**Objetivo:** permitir que el vendedor utilice los casos de uso implementados.

**Clases:** `MenuConsola` y `Main`.

`MenuConsola` leerá opciones y datos, los convertirá a sus tipos y llamará a los servicios. Mostrará resultados y mensajes de negocio. Una entrada no numérica o una fecha con formato incorrecto deberá permitir corregir la entrada o volver al menú.

La conversión de texto pertenece a la consola. La decisión de si una cantidad numérica puede venderse pertenece al dominio.

`Main` creará repositorios, inventario, estrategia y servicios; entregará las dependencias por constructor y arrancará el menú. Las mismas instancias compartidas se utilizarán durante la sesión para conservar una visión consistente de los lotes y cantidades.

Los datos en memoria se perderán al cerrar el programa; ese comportamiento forma parte del alcance inicial y debe explicarse en las instrucciones de uso.

**Origen:** RNF-02, RNF-03 y RNF-05.

**Comprobaciones:** recorrido manual del flujo completo, entradas inválidas, errores de negocio comprensibles, regreso al menú y cierre normal.

**Criterio de finalización:** el vendedor completa las operaciones desde consola y las reglas siguen comprobándose sin depender de ella.

### Etapa 11. Verificación de entrega y documentación

**Objetivo:** comprobar el cumplimiento del alcance y dejar el proyecto utilizable.

Se ejecutarán las pruebas relevantes, se realizará el escenario completo y se relacionarán historias de usuario con comportamientos implementados. Se revisará la consistencia de los diagramas con las decisiones adoptadas y se completará `README.md` con requisitos de ejecución, instrucciones y límites de la versión.

**Criterio de finalización:** los requisitos implementados tienen evidencia de funcionamiento, los cambios de diseño están explicados y otra persona puede ejecutar el proyecto siguiendo las instrucciones.

## 8. Primer escenario completo

Este escenario será nuestro primer objetivo de integración, con fechas fijas para que el resultado sea reproducible.

| Dato | Valor de ejemplo |
|---|---|
| Fecha de operación | 5 de octubre de 2026 |
| Producto | Carne molida vendida por peso |
| Lote 001 | 5 kg, vence el 6 de octubre de 2026 |
| Lote 002 | 20 kg, vence el 15 de octubre de 2026 |
| Cantidad solicitada | 8 kg |

1. Registrar un abastecedor, un cliente y el producto.
2. Registrar la compra que incorpora los dos lotes con fechas y origen válidos.
3. Comprobar 25 kg vendibles en la fecha de operación.
4. Registrar la venta de 8 kg.
5. Comprobar asignaciones de 5 kg al lote 001 y 3 kg al lote 002.
6. Comprobar cantidades restantes de 0 kg y 17 kg.
7. Consultar origen, abastecedor, lotes y cliente.
8. Anular la venta en la misma fecha del escenario.
9. Comprobar cantidades de 5 kg y 20 kg, junto con sus movimientos de devolución.
10. Intentar una segunda anulación y comprobar que el estado no cambia.

Se realizará un escenario adicional con anulación posterior al vencimiento para comprobar que las cantidades devueltas al lote vencido no se venden.

## 9. POO aplicada al código

| Pilar | Aplicación concreta | Qué debemos comprobar |
|---|---|---|
| Encapsulación | Cantidades y fechas protegidas; operaciones como `descontar()` | El estado inválido no puede introducirse mediante setters. |
| Abstracción | `Producto` y contratos como `Repositorio` | El consumidor trabaja con el comportamiento que necesita. |
| Herencia | Variantes por peso y por unidad | Comparten una identidad y comportamiento común justificable. |
| Polimorfismo | Cada variante implementa validación de cantidad | El detalle llama a `validarCantidad()` sin preguntar la variante. |

Además utilizaremos composición: una venta contiene detalles y estos contienen asignaciones. Una venta no hereda de un detalle. La herencia expresa una relación «es un»; la composición expresa que un objeto contiene o utiliza otros.

## 10. SOLID aplicado al código

| Principio | Decisión visible en TrazaCarne |
|---|---|
| S: responsabilidad única | El menú interactúa; los servicios coordinan; los objetos protegen sus reglas. |
| O: abierto a extensión, cerrado a modificación | La asignación depende de `EstrategiaAsignacion`, que permite incorporar otra política. |
| L: sustitución de Liskov | Las variantes de `Producto` respetan el contrato de cantidad válida para su forma de venta. |
| I: segregación de interfaces | Los contratos contienen las operaciones que sus consumidores necesitan. |
| D: inversión de dependencias | Los servicios reciben interfaces de repositorio, y `Main` proporciona la implementación. |

Para Liskov, el contrato de `Producto` no prometerá que todo decimal positivo sea válido en cualquier producto. Prometerá validar de acuerdo con la forma de venta. Así la variante por unidad puede rechazar fracciones sin contradecir el contrato común.

No se creará una interfaz para cada clase por obligación. Tampoco se añadirán patrones solo para aumentar la cantidad de conceptos utilizados. Cada abstracción debe resolver una necesidad del diseño y poder explicarse con un ejemplo.

## 11. Reglas que deben permanecer protegidas

Estas condiciones guiarán las revisiones del código:

- Los identificadores obligatorios no están vacíos y no se repiten en su ámbito.
- La cantidad disponible de un lote nunca es negativa.
- El producto determina qué cantidades son válidas.
- Un lote conserva su origen y sus fechas.
- Una operación válida contiene al menos un elemento con cantidad positiva.
- Los lotes vencidos no aportan existencias vendibles.
- Las asignaciones de un detalle suman su cantidad y usan lotes del mismo producto.
- El precio histórico de una venta no depende de cambios posteriores del producto.
- Una venta registrada no permite modificar silenciosamente sus detalles y asignaciones.
- Una venta se anula como máximo una vez.
- Entradas, salidas, devoluciones y pérdidas dejan movimientos.
- Desactivar participantes conserva relaciones históricas.
- Rechazar una compra o venta no produce cambios parciales.

El inventario tendrá una fuente coherente de cantidades. Si las cantidades disponibles se guardan en `Lote` y existe un historial de movimientos, ambos deberán mantenerse consistentes. No se mantendrán contadores independientes en varios servicios que puedan divergir.

## 12. Estrategia de pruebas

Las pruebas acompañarán cada etapa. Se escribirán para comportamientos relevantes y condiciones que pueden romper inventario o trazabilidad, evitando pruebas que solo repitan getters o la estructura interna del código.

### Pruebas del dominio

Comprobarán cantidades, fechas, descuentos, reposiciones, precios históricos y transición de anulación. Se utilizarán fechas y cantidades conocidas para que el resultado sea reproducible.

### Pruebas de servicios

Comprobarán coordinación con repositorios en memoria: duplicados, compras completas, ventas de varios productos, ausencia de cambios parciales, anulación y consultas de trazabilidad.

### Pruebas de repositorios

Comprobarán las partes importantes de su contrato: búsqueda, ausencia de resultados, registro sin sobrescritura accidental y conservación de los registros necesarios.

### Prueba de integración del flujo

El escenario de 8 kg recorrerá compra, inventario, FEFO, venta, trazabilidad y anulación. Permitirá detectar fallos que no aparecen al comprobar una clase aislada.

### Comprobación de consola

Se recorrerán manualmente las opciones principales y las entradas inválidas. Las reglas del negocio seguirán cubiertas por pruebas que no requieren escribir en el menú.

| Escenario esencial | Resultado esperado |
|---|---|
| Unidad con fracción | Rechazo sin registrar la operación. |
| Peso válido | Aceptación conforme a la precisión acordada. |
| Fechas incoherentes | Lote rechazado. |
| Identificador duplicado | Registro rechazado sin reemplazar el original. |
| Lote repetido en una compra | Compra rechazada sin entradas parciales. |
| Reparto FEFO | Primero el lote más próximo a vencer. |
| Producto repetido en la venta | Validación de la cantidad total consolidada. |
| Falta stock en un producto de la venta | Todos los lotes y movimientos permanecen intactos. |
| Cambio posterior de precio | La venta anterior conserva su precio. |
| Primera anulación | Devolución exacta a los lotes originales. |
| Segunda anulación | Rechazo sin cantidades ni movimientos adicionales. |
| Devolución a lote vencido | Cantidad registrada, excluida de lo vendible. |
| Participante desactivado | Historial y trazabilidad conservados. |

## 13. Cómo trabajar en cada sesión

1. Elegir una regla o un comportamiento pequeño de la etapa actual.
2. Consultar su requisito, historia y diagrama relacionado.
3. Identificar qué objeto tiene la responsabilidad y la información necesaria.
4. Definir sus entradas, resultado y condiciones de rechazo.
5. Implementar el comportamiento y su comprobación relevante.
6. Compilar y ejecutar las pruebas relacionadas.
7. Revisar encapsulación, contratos y dependencias.
8. Registrar decisiones de diseño que cambien lo acordado.
9. Guardar un avance coherente en Git cuando corresponda.
10. Continuar con el siguiente comportamiento.

Las pruebas de una etapa previa se repetirán cuando un cambio pueda afectarlas. Una vez que la verificación apropiada pase, se continuará con la implementación siguiente.

## 14. Errores que conviene evitar

| Error | Problema que produce | Decisión de este plan |
|---|---|---|
| Concentrar el negocio en `Main` | Dificulta comprender responsabilidades y comprobarlas | Usar `Main` para crear y conectar objetos. |
| Validar únicamente desde el menú | Otra forma de invocar el programa puede omitir las reglas | Proteger reglas en dominio y coordinación en servicios. |
| Generar todos los setters | Permite romper cantidades, origen o historial | Ofrecer operaciones con significado. |
| Descontar al recorrer cada línea | Puede dejar ventas incompletas | Preparar toda la operación antes de aplicar cambios. |
| Verificar líneas repetidas por separado | Puede vender más de lo disponible | Consolidar por producto. |
| Usar el precio actual para ventas antiguas | Altera el historial | Guardar el precio aplicado en cada detalle. |
| Borrar registros utilizados | Rompe relaciones históricas | Desactivar conservando los registros. |
| Reponer sin usar las asignaciones | Devuelve a lotes equivocados | Utilizar el origen registrado al vender. |
| Copiar cantidades en varios lugares | Genera estados contradictorios | Mantener una fuente coherente y sus movimientos. |
| Crear todas las clases vacías primero | Da apariencia de avance sin comportamiento | Implementar por etapas comprobables. |

## 15. Seguimiento del avance

Esta lista refleja el avance comprobado del código y las guías. La actualización gráfica de los diagramas originales queda distinguida de esta entrega de código y Markdown.

- [x] Etapa 0: proyecto, JDK, Maven y pruebas preparados.
- [x] Etapa 1: tipos básicos y errores de negocio, incluidos estados de venta y tipos de movimiento.
- [x] Etapa 2: productos y validación polimórfica.
- [x] Etapa 3: clientes, abastecedores y repositorios.
- [x] Etapa 4: compras, lotes y entradas.
- [x] Etapa 5: existencias vendibles y FEFO.
- [x] Etapa 6: ventas completas y precio histórico.
- [x] Etapa 7: anulación con devolución al origen.
- [x] Etapa 8: trazabilidad en ambos sentidos.
- [x] Etapa 9: consultas de inventario y pérdidas.
- [x] Etapa 10: consola y conexión en `Main`.
- [x] Etapa 11: verificación de entrega de código y guías.
- [x] Decisiones de cantidades, importes y fechas documentadas.
- [x] Escenario completo de 8 kg comprobado.
- [x] Rechazos importantes conservan el estado anterior.
- [x] Cambios de contratos respecto al UML explicados en el índice y las guías.
- [ ] Actualizar la representación gráfica de los diagramas originales; fuera de esta entrega de código y Markdown.
- [x] Instrucciones de ejecución y límites de la versión disponibles.

## 16. Primera sesión de implementación

La primera sesión tendrá un alcance concreto: preparar el proyecto y comenzar `Producto` y sus variantes.

1. Confirmar el JDK y comprobar el arranque.
2. Preparar la ejecución de pruebas.
3. Concretar las decisiones mínimas sobre cantidad y precio.
4. Crear la excepción necesaria para cantidades inválidas.
5. Implementar `Producto` y su contrato de validación.
6. Implementar `ProductoPorPeso` y `ProductoPorUnidad`.
7. Comprobar cantidades válidas e inválidas.
8. Explicar la relación entre requisitos, responsabilidades, código y pruebas.

Al terminar podremos demostrar los cuatro pilares de POO con un comportamiento del negocio y tendremos una base para continuar con clientes, abastecedores y compras.
