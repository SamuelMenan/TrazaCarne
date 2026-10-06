# Bloque 08: pruebas de compras, inventario, ventas y trazabilidad

Este bloque explica las pruebas nuevas archivo por archivo. No sustituyen las pruebas anteriores de productos, participantes, servicios y repositorios: se añaden para comprobar las operaciones que ya involucran a varios objetos.

Una prueba de este bloque no pregunta únicamente si existe un método. Prepara datos, ejecuta una operación y comprueba un resultado del negocio. Cuando esperamos un rechazo, también comprobamos que no haya quedado una compra, un descuento o un movimiento parcial.

## 1. De dónde salen estas pruebas

Las reglas y criterios de aceptación se convierten en escenarios concretos:

| Regla del negocio | Escenario que escribimos |
|---|---|
| No vender más de lo disponible | Intentar vender 6 kg cuando hay 5 kg. |
| FEFO: primero lo que vence antes | Recibir lotes en otro orden y solicitar una cantidad repartida entre ellos. |
| Una venta debe registrarse completa | Pedir dos productos: el primero tiene stock y el segundo no. |
| El origen permanece asociado a la venta | Consultar dos asignaciones después de descontar 5 + 3 kg. |
| Una venta solo puede anularse una vez | Intentar una segunda anulación y comprobar que no reaparezcan cantidades adicionales. |
| El precio histórico debe conservarse | Cambiar el precio actual después de registrar una venta. |
| No duplicar lotes | Repetir un código dentro de una compra y en otra compra posterior. |
| Las fechas deben ser coherentes | Recibir un lote que vence hoy o fue procesado después de la recepción. |

También comprobamos decisiones del diseño que sostienen esas reglas: planificación sin descuentos, listas protegidas, líneas repetidas y rechazo de movimientos anteriores al historial del inventario.

## 2. Archivos creados y orden de lectura

Todos los archivos están en `src/test/java/co/trazacarne/flujo`:

```text
flujo/
├── Escenario.java
├── ComprasTest.java
├── InventarioFefoTest.java
├── VentasTest.java
└── TrazabilidadTest.java
```

Lee primero `Escenario.java`, porque prepara los objetos compartidos por las pruebas. Después sigue compra → inventario → venta → trazabilidad. Cada clase de pruebas crea un escenario nuevo: los registros de una prueba no quedan disponibles para la siguiente.

La dependencia `junit-jupiter` del `pom.xml` permite utilizar JUnit 5. Sus anotaciones indican qué métodos son pruebas y sus comprobaciones, llamadas aserciones, verifican el resultado.

## 3. Cómo leer una prueba

Este ejemplo real está en `VentasTest.java`:

```java
@Test
void dosLineasDeSeisKilosSeCompruebanComoDoceContraDiezDisponibles() {
    e.comprar("C-001", e.lote("L-001", "P-001", "10", 2));

    assertThrows(ReglaNegocioException.class,
            () -> e.vender("V-001", e.item("P-001", "6"),
                    e.item(" P-001 ", "6")));

    assertRechazoConSaldo("10", 1);
}
```

La primera línea del cuerpo **prepara** un lote con 10 kg. La segunda parte **ejecuta** la solicitud de 6 + 6 kg y exige que se lance una excepción. La última línea **comprueba las consecuencias**: siguen quedando 10 kg, solo existe el movimiento de entrada y no se guardó ninguna venta.

`@Test` hace que JUnit ejecute el método. El nombre del método describe la regla. Una función anónima como `() -> e.vender(...)` permite entregar a JUnit la operación que debe ejecutar para comprobar la excepción.

`assertThrows` no significa que la prueba haya fallado: en este caso, recibir la excepción es justamente el resultado esperado.

### Las comprobaciones utilizadas

| Expresión | Qué comprueba |
|---|---|
| `assertEquals(esperado, actual)` | Que ambos valores sean iguales. |
| `assertSame(esperado, actual)` | Que sea exactamente el mismo objeto. |
| `assertTrue(condicion)` | Que la condición sea verdadera. |
| `assertFalse(condicion)` | Que la condición sea falsa. |
| `assertThrows(Tipo.class, operacion)` | Que la operación lance ese tipo de excepción o una subclase. |
| `assertAll(...)` | Que se revisen todas las comprobaciones agrupadas y se informen sus fallos. |

Comprobar solamente la excepción no basta en compras y ventas. También verificamos el estado final para detectar efectos parciales.

## 4. `Escenario.java`: preparar un sistema pequeño

`Escenario` no es una entidad de producción ni un servicio nuevo. Es una ayuda usada exclusivamente por las pruebas para no escribir repetidamente toda la conexión de repositorios y servicios.

### Fecha y reloj

```java
static final LocalDate HOY = LocalDate.of(2026, 10, 5);
static final ZoneId ZONA = ZoneId.of("America/Bogota");
static final Clock RELOJ = reloj(HOY);
```

Las pruebas se ejecutan como si fueran las 10:00 de esa fecha en Bogotá. La fecha fija es deliberada: la misma prueba debe dar el mismo resultado mañana o dentro de un año. No utilizamos la fecha real del computador para decidir si estos lotes están vencidos.

```java
static Clock reloj(LocalDate fecha) {
    return Clock.fixed(fecha.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
}
```

`Clock.fixed` crea un reloj que siempre devuelve el mismo instante. Para probar una anulación diez días después, entregamos otro reloj a un servicio que utiliza los mismos registros e inventario. Cambia el momento de operación, no los datos históricos.

### Repositorios e inyección por constructor

```java
final Repositorio<Compra, String> comprasRepo =
        new RepositorioEnMemoria<>(Compra::getNumero);
```

Este repositorio guarda compras y obtiene sus identificadores llamando a `getNumero()`. `Compra::getNumero` es una referencia a un método: indica qué función usar para obtener la clave.

De la misma manera se crean los repositorios de abastecedores, clientes, productos y ventas. Todos son nuevos para cada escenario. Los servicios reciben esos repositorios y un único `Inventario`, de modo que las compras y las ventas trabajan sobre las mismas existencias.

```java
final Inventario inventario = new Inventario(new EstrategiaFEFO());
```

El inventario recibe la estrategia concreta que utilizaremos en estos escenarios. La estrategia de producción conserva su interfaz; la prueba ensambla el sistema como lo hace `Main`.

### Constructor del escenario

El constructor registra dos abastecedores (`9001` proveedor y `9002` matadero), un cliente (`1001`) y dos productos:

- `P-001`: carne por peso, a 24 000 COP por kg.
- `P-002`: hamburguesa por unidad, a 6 500 COP por unidad.

Con esa diferencia comprobamos que una compra o venta respete la forma de venta del producto, incluso cuando llega a él por medio de un servicio.

### Métodos auxiliares

| Método | Qué prepara o comprueba |
|---|---|
| `lote(codigo, producto, cantidad, diasVencimiento)` | Crea el DTO de un lote procesado ayer y con vencimiento relativo a `HOY`. |
| `comprar(numero, lotes...)` | Registra una compra del proveedor `9001`. |
| `item(producto, cantidad)` | Crea un ítem de solicitud de venta. |
| `vender(numero, items...)` | Registra una venta del cliente `1001`. |
| `servicioVentas(reloj)` | Crea otro servicio de ventas con el mismo inventario y repositorios. |
| `servicioInventario(reloj)` | Consulta y opera sobre el mismo inventario con otra fecha. |
| `decimal(texto)` | Construye un `BigDecimal` desde texto. |
| `saldo(lotes)` | Suma las cantidades disponibles de una lista de lotes, incluidos los vencidos. |
| `cantidad(esperada, actual)` | Compara cantidades por su valor numérico. |

Los puntos suspensivos, por ejemplo `DatosLoteRecibido... lotes`, significan que el método acepta varios argumentos de ese tipo. `List.of(lotes)` los transforma en una lista.

La consulta `existencias()` devuelve los lotes, porque la consola necesita mostrar el saldo de cada uno. Cuando la prueba necesita el total físico, utiliza esta ayuda:

```java
static BigDecimal saldo(List<Lote> lotes) {
    return lotes.stream().map(Lote::getCantidadDisponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

`stream()` recorre la lista, `map` obtiene la cantidad de cada lote y `reduce` suma todas las cantidades empezando en cero. Esta suma no filtra vencimientos: sirve para distinguir cantidad física de cantidad vendible.

```java
static void cantidad(String esperada, BigDecimal actual) {
    assertEquals(0, decimal(esperada).compareTo(actual),
            () -> "Se esperaba " + esperada + " y se obtuvo " + actual);
}
```

Usamos `compareTo` porque `BigDecimal.equals` también tiene en cuenta la escala: `5` y `5.000` pueden ser distintos para `equals`, aunque representen la misma cantidad. Un resultado `0` en `compareTo` significa que tienen el mismo valor numérico.

## 5. `ComprasTest.java`: recepción completa o rechazo sin cambios

Este archivo comprueba que los datos recibidos se convierten en compras, lotes y movimientos de entrada solo cuando toda la compra es válida.

### Compra válida y origen

`recibirCompraCreaLotesConOrigenYMovimientosDeEntrada()` recibe 5 kg y 12 unidades. Comprueba que la compra se puede consultar, que el primer lote apunta a esa misma compra, que ambas cantidades están disponibles y que existen dos movimientos de tipo `ENTRADA`.

```java
assertSame(compra, e.inventario.consultarLote("L-001").getCompra());
```

Aquí verificamos la relación necesaria para reconstruir el origen; no copiamos el nombre de un proveedor en el lote y perdemos el vínculo con la compra.

### Matadero y fechas

`mataderoExigeSacrificioYConservaEsaFechaComoOrigen()` primero intenta recibir un lote de matadero sin sacrificio. Comprueba el rechazo y ausencia de efectos. Después repite la compra con una fecha válida y comprueba que el lote la conserva.

`fechaIncoherenteRechazaTodaLaCompra()` es una prueba parametrizada:

```java
@ParameterizedTest
@ValueSource(strings = {
        "sacrificioPosterior", "procesamientoFuturo", "venceHoy", "vencido"
})
```

JUnit ejecuta el mismo método una vez por cada texto. El `switch` del test prepara la fecha incorrecta correspondiente. La compra siempre contiene primero un lote válido y después el inválido: así también verificamos que el primero no entre por accidente.

### Los demás métodos de este archivo

| Método | Escenario y resultado que comprueba |
|---|---|
| `loteRepetidoDentroDeCompraNoDejaEntradaParcial()` | Dos lotes con el mismo código, uno con espacios externos. Se rechaza toda la compra. |
| `loteRegistradoEnOtraCompraNoModificaElOriginalNiInsertaElNuevo()` | Un código existente aparece en una compra nueva. Se conserva únicamente la compra y el saldo originales. |
| `abastecedorInactivoNoPuedeOriginarUnaCompraNueva()` | Desactivar el proveedor impide registrar nuevas entradas. |
| `productoInactivoEnSegundoLoteRechazaTambienElPrimero()` | Un segundo producto inactivo invalida la compra completa. |
| `recepcionRespetaLaCantidadEnteraDelProductoPorUnidad()` | No pueden recibirse `2.5` unidades de hamburguesa. |
| `numeroDeCompraDuplicadoNoInsertaOtrosLotes()` | Repetir el número de compra no incorpora los lotes de la nueva solicitud. |
| `registrarLaMismaCompraDosVecesNoDuplicaLaRecepcion()` | Llamar otra vez a `Compra.registrar()` se rechaza y conserva un solo movimiento de entrada. |
| `datosCompraCopianLaListaRecibida()` | Vaciar la lista original después de crear el DTO no modifica lo solicitado. Su lista expuesta tampoco permite cambios. |

El método privado `assertSinCompraNiInventario()` comprueba simultáneamente tres listas vacías: compras, lotes y movimientos. Se reutiliza dentro del test para que ninguna comprobación del rechazo se olvide.

## 6. `InventarioFefoTest.java`: elegir lotes y conservar el historial

### Orden FEFO y desempate

`fefoOrdenaPorVencimientoYCodigoSinDescontarTodavia()` recibe primero un lote tardío y después los próximos a vencer. Los dos próximos vencen el mismo día, pero se registran en orden B → A.

Solicitar 10 kg debe producir:

```text
L-A:  3 kg
L-B:  5 kg
L-Z:  2 kg
```

La prueba verifica el orden y las cantidades de las asignaciones, pero también exige que sigan existiendo los 28 kg iniciales. **Planificar el reparto no equivale a vender.**

### Estrategia pura

`estrategiaEsPuraNoReordenaLaListaNiModificaElInventario()` llama directamente a `EstrategiaFEFO.asignar()`. Comprueba que empieza por el lote próximo, conserva el orden de la lista recibida y deja intactos saldos y movimientos.

No basta con que la estrategia elija bien: necesitamos poder utilizar su resultado para validar una venta completa antes de aplicar cambios.

### Existencias frente a disponibilidad

`alLlegarElVencimientoConservaExistenciasPeroExcluyeCantidadVendible()` avanza el reloj un día. El lote que vence ese día deja de ser vendible, aunque su cantidad siga físicamente registrada:

```java
cantidad("25", saldo(manana.existencias("P-001")));
cantidad("20", manana.disponible("P-001"));
```

La pérdida se registra mediante una operación explícita; la consulta de vencimiento no borra cantidades por sí sola.

### Los demás métodos de este archivo

| Método | Qué comprobamos |
|---|---|
| `stockInsuficienteNoProduceAsignacionParcialNiMovimientos()` | Planificar 6 kg contra 5 kg se rechaza sin descuentos. |
| `proximosAVencerExcluyeVencidosYAgotadosYRespetaElLimiteDeDias()` | La consulta omite lotes ya vencidos, sin cantidad y fuera del plazo solicitado. |
| `perdidaReduceElLoteYDejaUnMovimientoConTipoPerdida()` | Perder 1.250 kg deja 3.750 kg y un movimiento `PERDIDA`. |
| `perdidaMayorAlDisponibleOMotivoVacioNoCambiaSaldoNiHistorial()` | Una pérdida inválida conserva la cantidad y el historial. |
| `movimientoAnteriorALaRecepcionSeRechazaSinCambios()` | Un reloj anterior a la recepción no puede introducir una pérdida retroactiva. |
| `listasPublicasNoPermitenBorrarLotesNiMovimientosDelInventario()` | El código consumidor no puede vaciar las listas internas del inventario. |

## 7. `VentasTest.java`: registro, precio histórico y anulación

### El escenario principal de 5 + 3 kg

`ventaDeOchoKilosDescuentaCincoYTresDeSusLotesOriginales()` recibe un lote de 5 kg próximo a vencer y otro de 20 kg posterior. Registra una venta de 8 kg y comprueba:

- Estado `REGISTRADA`.
- Saldos finales de 0 y 17 kg.
- Asignaciones históricas de 5 y 3 kg.
- Total de 192 000 COP.
- Dos movimientos de salida.

El método privado `comprarDosLotes()` prepara ese mismo escenario para las pruebas que lo necesitan.

### Rechazo de una venta completa

`faltaDeStockEnSegundoProductoNoDescuentaElPrimero()` es una prueba de coordinación. El primer producto tiene suficiente stock y el segundo no. Después del rechazo, ambos saldos siguen iguales, solo quedan los movimientos de entrada y el repositorio de ventas está vacío.

```java
assertAll(
        () -> cantidad("5", e.consultas.disponible("P-001")),
        () -> cantidad("2", e.consultas.disponible("P-002")),
        () -> assertEquals(2, e.inventario.getMovimientos().size()),
        () -> assertTrue(e.ventas.listar().isEmpty())
);
```

Esto detectaría una implementación que descuenta el primer producto y solo después descubre el fallo del segundo.

### Productos repetidos

Tres métodos se complementan:

| Método | Motivo de la prueba |
|---|---|
| `dosLineasDeSeisKilosSeCompruebanComoDoceContraDiezDisponibles()` | Validar cada línea por separado sería insuficiente: juntas exceden el stock. |
| `lineasRepetidasValidasSeUnificanSinDuplicarLaAsignacion()` | Una solicitud de 1.250 + 2.250 kg debe quedar en un detalle de 3.500 kg. |
| `cantidadNegativaNoPuedeCompensarseConOtraLineaPositiva()` | Hay que validar cada línea antes de sumar. `-2 + 5` no debe convertirse en una solicitud válida de 3 kg. |

### Precio histórico

`ventaConservaElPrecioHistoricoAunqueCambieElProducto()` registra a 24 000 COP por kg y luego cambia el precio actual a 30 000. La venta conserva su precio unitario y total originales. Incluso después de anularla, se conserva el importe histórico.

```java
cantidad("24000", venta.getDetalles().getFirst().getPrecioUnitario());
cantidad("192000", venta.calcularTotal());
cantidad("240000", e.carne.calcularSubtotal(decimal("8")));
```

La venta histórica y el cálculo actual tienen resultados distintos porque corresponden a precios de momentos diferentes.

### Anulación

`anulacionReponeCadaLoteYDejaMovimientosDeDevolucion()` comprueba los saldos originales de 5 y 20 kg, el estado `ANULADA`, dos devoluciones y la conservación de la venta en su repositorio.

`segundaAnulacionNoDuplicaCantidadesNiMovimientos()` intenta anular de nuevo. El saldo permanece en 25 kg y el historial no crece.

`anulacionTardiaDevuelveLotesVencidosAunqueClienteYProductoEstenInactivos()` avanza diez días y desactiva al cliente y al producto. La anulación sigue permitida, porque corrige una venta histórica. Devuelve los 25 kg a sus lotes, pero la disponibilidad vendible es 0 por el vencimiento.

### Los demás métodos de este archivo

| Método | Qué evita |
|---|---|
| `clienteOProductoInactivoImpideUnaVentaNueva()` | Registrar operaciones nuevas con participantes inactivos. Se ejecuta dos veces, una por cada condición. |
| `numeroDeVentaDuplicadoNoDescuentaStockUnaSegundaVez()` | Que repetir un número de venta vuelva a descontar existencias. |
| `planAnteriorSeRevalidaSiOtraVentaYaConsumioLaCantidad()` | Aplicar un plan preparado cuando su saldo ya cambió. Se conserva la venta pendiente como `BORRADOR`. |
| `datosVentaYDetalleProtegenSusListasContraCambiosExternos()` | Que modificar una lista externa o expuesta borre solicitudes, detalles o asignaciones. |

`assertRechazoConSaldo()` comprueba saldo, cantidad de movimientos y ausencia de ventas. Es una ayuda local de esta clase de pruebas; no agrega comportamiento al negocio.

## 8. `TrazabilidadTest.java`: recorrer origen y destino

Este archivo verifica resultados de consulta después de operaciones reales. No crea manualmente una lista de origen o destino para comprobarla contra sí misma.

| Método | Preparación y comprobación |
|---|---|
| `origenReconstruyeProveedorProductoLotesYCantidadesDeUnaVenta()` | Compra dos lotes, vende 8 kg y obtiene el origen de los 5 + 3 kg: proveedor, producto, vencimiento y estado. |
| `destinoIncluyeAnuladasCuandoSePideHistorialYLasExcluyeDelFiltroVigente()` | Registra dos ventas del mismo lote y anula una. El historial conserva ambas; la consulta vigente devuelve una sola. |
| `clientesDelLoteConservaClientesHistoricosSinDuplicarlosPorVenta()` | El mismo cliente aparece en varias ventas, luego todas se anulan y se desactiva el cliente. La consulta histórica lo conserva una vez, junto con otro cliente. |
| `consultarOrigenODestinoInexistenteNoInventaResultados()` | Consultar una venta o lote desconocido lanza `RegistroNoEncontradoException` y no crea registros. |

Por ejemplo:

```java
var historial = e.trazabilidad.ventasDelLote("L-001", true);
var vigentes = e.trazabilidad.ventasDelLote("L-001", false);
```

`true` pide incluir las ventas anuladas. `false` pide excluirlas. `var` deja que Java infiera el tipo de la variable; no permite cambiarlo a otro tipo después.

Las respuestas de trazabilidad son DTO de salida. Los registros de Java se consultan con métodos como `cantidad()` y `codigoLote()`, mientras las entidades usan métodos como `getCantidadDisponible()`.

## 9. Ejecutar las pruebas

Abre una terminal en la carpeta que contiene `pom.xml`:

```powershell
mvn test
```

Para ejecutar solo los archivos nuevos:

```powershell
mvn "-Dtest=ComprasTest,InventarioFefoTest,VentasTest,TrazabilidadTest" test
```

Para seguir una sola regla mientras estudias el código:

```powershell
mvn "-Dtest=VentasTest#dosLineasDeSeisKilosSeCompruebanComoDoceContraDiezDisponibles" test
```

En IntelliJ también puedes ejecutar el botón verde al lado de una clase o de un método de prueba. Si Maven no está disponible en la terminal, usa el panel **Maven → Lifecycle → test**.

Una ejecución correcta muestra `Failures: 0`, `Errors: 0` y `BUILD SUCCESS`. Los nuevos archivos contienen 41 escenarios contando las ejecuciones de pruebas parametrizadas; se ejecutan junto con los escenarios anteriores cuando utilizas `mvn test`.

## 10. Cómo investigar un fallo

1. Lee el nombre del método: describe el comportamiento esperado.
2. Revisa los datos que prepara y qué fecha usa el reloj.
3. Identifica la aserción que falló y compara valor esperado con valor obtenido.
4. Sigue la llamada hacia el servicio, la entidad y el inventario.
5. Corrige la responsabilidad que incumple la regla y vuelve a ejecutar el escenario afectado.
6. Ejecuta la suite completa después de integrar el cambio.

Si falla la distribución FEFO, empieza por la estrategia. Si la cantidad es correcta pero faltan movimientos, revisa la operación de inventario. Si una venta cambia de total después de actualizar un producto, revisa dónde se conserva el precio. Si una consulta excluye un cliente histórico, revisa su filtro de trazabilidad.

Las pruebas no demuestran todos los comportamientos posibles ni garantizan automáticamente un buen diseño. Ofrecen evidencia sobre estos escenarios concretos y ayudan a conservar sus reglas mientras seguimos modificando el programa.

## 11. Ejercicio de lectura

Comienza por `ventaDeOchoKilosDescuentaCincoYTresDeSusLotesOriginales()` y responde:

1. ¿Qué dos lotes recibe `comprarDosLotes()`?
2. ¿Por qué se agota el primer lote antes de utilizar el segundo?
3. ¿Qué línea comprueba el total de la venta?
4. ¿Qué objetos conservan la relación con los lotes originales?
5. ¿Qué saldos deben quedar después de anular esa misma venta?

Después ejecuta `anulacionReponeCadaLoteYDejaMovimientosDeDevolucion()` y sigue en producción las llamadas `ServicioVentas.anularVenta()` → `Venta.anular()` → operación de devolución del inventario. Esa lectura muestra cómo una regla documentada llega al servicio, al dominio y finalmente a una comprobación verificable.
