# Bloque 4: inventario, movimientos y FEFO, explicado archivo por archivo

Este bloque responde dos preguntas diferentes: **¿cuánto queda?** y **¿de cuáles lotes debe salir una venta?** El inventario conserva los lotes y su historial; la estrategia FEFO prepara un reparto siguiendo la fecha de vencimiento.

Para comprenderlo necesitas el [Bloque 3: compras y lotes](BLOQUE_03_COMPRAS_Y_LOTES.md). Allí se explica que un lote conserva cantidad recibida y disponible, y que su cantidad solo se modifica mediante operaciones del paquete de dominio.

## 1. Reglas que se convierten en código

Según el plan, RN-07, RN-08 y RN-09 sustentan el control de cantidades, la prioridad de salida y el origen de cada cantidad vendida. RN-10 exige conservar movimientos. HU-07 añade las consultas y pérdidas.

Aplicamos estas decisiones:

- Una cantidad registrada físicamente puede estar vencida y no ser vendible.
- FEFO excluye lotes vencidos, sin cantidad o con producto inactivo.
- Si dos lotes vencen el mismo día, se ordenan por código para que el resultado sea reproducible.
- Preparar asignaciones no descuenta existencias.
- Se valida una operación completa antes de aplicar sus cambios.
- Una pérdida se registra explícitamente con un motivo. El paso del tiempo no genera pérdidas automáticamente.
- Los movimientos se registran en orden temporal. Se permite el mismo instante, pero se rechaza uno anterior al último movimiento.

## 2. Archivo `TipoMovimiento.java`

[Abrir TipoMovimiento.java](../src/main/java/co/trazacarne/dominio/TipoMovimiento.java).

```java
public enum TipoMovimiento {
    ENTRADA, SALIDA, DEVOLUCION, PERDIDA
}
```

Es un `enum`, un conjunto cerrado de categorías:

| Valor | Hecho que representa | Efecto sobre el disponible |
|---|---|---|
| `ENTRADA` | Mercancía recibida en una compra | Establece la cantidad inicial del lote. |
| `SALIDA` | Mercancía entregada por una venta | Resta cantidad. |
| `DEVOLUCION` | Mercancía repuesta al anular una venta | Suma lo vendido al lote original. |
| `PERDIDA` | Mercancía retirada por daño, vencimiento u otro motivo | Resta cantidad. |

Una devolución añade un nuevo hecho al historial: no elimina la salida original.

## 3. Archivo `MovimientoInventario.java`

[Abrir MovimientoInventario.java](../src/main/java/co/trazacarne/dominio/MovimientoInventario.java).

### 3.1. Qué conserva

```java
public final class MovimientoInventario {
    private final long id;
    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final Lote lote;
    private final BigDecimal cantidad;
    private final String referencia;
    private final String motivo;
```

Está en el paquete de dominio. Importa `BigDecimal` para cantidades, `LocalDateTime` para fecha con hora y la excepción de negocio.

La clase es `final`: no se puede heredar de ella. Todos los atributos son privados y finales. El movimiento conserva sus hechos originales: cuándo ocurrió, cuánto movió y por qué. El lote referenciado puede cambiar de cantidad después, pero la cantidad del movimiento permanece fija.

`long` es un entero amplio para numerar movimientos. `referencia` contiene el número de compra, venta o el código de lote en una pérdida. `motivo` describe la causa.

### 3.2. Constructor

```java
MovimientoInventario(long id, LocalDateTime fecha, TipoMovimiento tipo, Lote lote,
                     BigDecimal cantidad, String referencia, String motivo) {
    if (id <= 0 || fecha == null || tipo == null || lote == null) {
        throw new ReglaNegocioException("El movimiento requiere identidad, fecha, tipo y lote.");
    }
    lote.getProducto().validarCantidad(cantidad);
    if (referencia == null || referencia.isBlank() || motivo == null || motivo.isBlank()) {
        throw new ReglaNegocioException("El movimiento requiere referencia y motivo.");
    }
```

El constructor tiene acceso de paquete. El inventario lo utiliza al registrar una operación; no se ofrecen movimientos independientes desde la consola.

Exige identidad positiva y referencias completas. La cantidad se valida mediante el producto del lote. Después asigna los campos y usa `strip()` para quitar espacios externos de referencia y motivo.

### 3.3. Getters

`getId`, `getFecha`, `getTipo`, `getLote`, `getCantidad`, `getReferencia` y `getMotivo` permiten leer esos datos. No existen setters: el movimiento representa un hecho ocurrido, no un formulario para editar posteriormente.

## 4. Archivo `AsignacionLote.java`

[Abrir AsignacionLote.java](../src/main/java/co/trazacarne/dominio/AsignacionLote.java).

```java
public final class AsignacionLote {
    private final Lote lote;
    private final BigDecimal cantidad;

    public AsignacionLote(Lote lote, BigDecimal cantidad) {
        if (lote == null) {
            throw new ReglaNegocioException("La asignación requiere un lote.");
        }
        lote.getProducto().validarCantidad(cantidad);
        this.lote = lote;
        this.cantidad = cantidad;
    }
```

Representa «esta cantidad sale de este lote». Por ejemplo, para vender 8 kg puede haber una asignación de 5 kg al lote `L-001` y otra de 3 kg al `L-002`.

El constructor exige lote y cantidad válida. Los getters `getLote()` y `getCantidad()` devuelven esos datos.

Una asignación por sí sola no descuenta mercancía ni demuestra que haya stock suficiente. Se puede construir para preparar una venta; FEFO y el inventario comprueban disponibilidad en los pasos correspondientes.

La asignación y el movimiento tienen propósitos distintos: la primera conserva el origen de un detalle vendido; el segundo registra un cambio en las existencias.

## 5. Archivo `EstrategiaAsignacion.java`

[Abrir EstrategiaAsignacion.java](../src/main/java/co/trazacarne/dominio/estrategia/EstrategiaAsignacion.java).

```java
public interface EstrategiaAsignacion {
    List<AsignacionLote> asignar(Producto producto, BigDecimal cantidad,
                               List<Lote> candidatos, LocalDate hoy);
}
```

Es una interfaz del paquete `dominio.estrategia`. Declara el contrato, sin escribir el algoritmo:

- Recibir el producto, la cantidad solicitada, los posibles lotes y la fecha de operación.
- Devolver una lista que cubra la cantidad completa.
- Prepararla sin modificar existencias.
- Comunicar el rechazo cuando no se pueda satisfacer la solicitud.

Los imports permiten referenciar los tipos del dominio y de Java. `List<AsignacionLote>` expresa el tipo de resultado.

El inventario depende de este contrato. `Main` le proporciona `EstrategiaFEFO`. Esta relación permite incorporar otra política sin reescribir las operaciones de inventario.

## 6. Archivo `EstrategiaFEFO.java`

[Abrir EstrategiaFEFO.java](../src/main/java/co/trazacarne/dominio/estrategia/EstrategiaFEFO.java).

### 6.1. Declaración y comprobaciones iniciales

```java
public class EstrategiaFEFO implements EstrategiaAsignacion {
    @Override
    public List<AsignacionLote> asignar(Producto producto, BigDecimal cantidad,
                                      List<Lote> candidatos, LocalDate hoy) {
```

`implements` indica que cumple la interfaz. `@Override` permite que Java compruebe que este método implementa el contrato declarado.

Antes de ordenar, exige producto, candidatos y fecha. Después llama `producto.validarCantidad(cantidad)`, por lo que mantiene la misma regla de peso o unidades usada en compras.

### 6.2. Filtrar candidatos y detectar repeticiones

```java
List<Lote> elegibles = new ArrayList<>();
Set<String> codigos = new HashSet<>();
for (Lote lote : candidatos) {
    if (lote == null || !codigos.add(lote.getCodigo())) {
        throw new ReglaNegocioException("Los candidatos no pueden ser nulos ni repetir lotes.");
    }
    if (lote.getProducto().getCodigo().equals(producto.getCodigo()) && lote.puedeVenderse(hoy)) {
        elegibles.add(lote);
    }
}
```

`elegibles` empieza vacía y reúne solo los lotes que se pueden utilizar. `Set<String>` conserva códigos sin repetirlos; `HashSet` es su implementación.

`codigos.add(...)` devuelve `true` si el código era nuevo y `false` si ya estaba. `!` invierte ese resultado: una repetición produce la excepción. Así no sumamos dos veces la misma mercancía.

El segundo `if` exige que el lote pertenezca al producto solicitado y cumpla `puedeVenderse(hoy)`.

### 6.3. Orden FEFO

```java
elegibles.sort(Comparator.comparing(Lote::getFechaVencimiento)
        .thenComparing(Lote::getCodigo));
```

`sort()` ordena la lista. `Comparator.comparing(...)` indica qué dato comparar. `Lote::getFechaVencimiento` es una referencia a método: equivale a pedir la fecha a cada lote. `thenComparing(...)` establece el desempate por código.

No se ordena por fecha de compra ni por fecha de procesamiento. El criterio principal es el vencimiento.

### 6.4. Repartir la cantidad

```java
List<AsignacionLote> resultado = new ArrayList<>();
BigDecimal pendiente = cantidad;
for (Lote lote : elegibles) {
    if (pendiente.signum() == 0) { break; }
    BigDecimal tomada = pendiente.min(lote.getCantidadDisponible());
    resultado.add(new AsignacionLote(lote, tomada));
    pendiente = pendiente.subtract(tomada);
}
```

`pendiente` comienza con toda la solicitud. `min()` toma el menor valor entre lo pendiente y lo disponible. `break` sale del bucle cuando ya se completó la cantidad.

Ejemplo de 8 kg:

| Vuelta | Disponible en el lote | Pendiente antes | Tomada | Pendiente después |
|---|---:|---:|---:|---:|
| `L-001` | 5 | 8 | 5 | 3 |
| `L-002` | 20 | 3 | 3 | 0 |

El método construye asignaciones, pero en este fragmento no llama `descontar()`.

```java
if (pendiente.signum() > 0) {
    throw new StockInsuficienteException(
            "No hay existencias vendibles suficientes de " + producto.getNombre() + ".");
}
return List.copyOf(resultado);
```

Si todavía falta cantidad, rechaza el plan completo. No devuelve una asignación parcial. Si está completo, devuelve una copia no modificable.

## 7. Archivo `StockInsuficienteException.java`

[Abrir StockInsuficienteException.java](../src/main/java/co/trazacarne/excepcion/StockInsuficienteException.java).

Hereda de `ReglaNegocioException` y su constructor llama `super(mensaje)`. Identifica un fallo de disponibilidad y transporta su explicación. FEFO, `Lote` o `Inventario` detectan el problema; la excepción no calcula existencias.

## 8. Archivo `Inventario.java`

[Abrir Inventario.java](../src/main/java/co/trazacarne/dominio/Inventario.java).

Es el punto que coordina modificaciones de cantidades y sus movimientos. Mantiene una sola instancia compartida durante la sesión; los servicios no crean un inventario distinto por operación.

### 8.1. Imports y atributos

Importa la estrategia, excepciones, cantidades, fechas y colecciones. `ChronoUnit` calcula días entre fechas; `Comparator` ordena consultas; `Objects` comprueba dependencias.

```java
private final Map<String, Lote> lotes = new LinkedHashMap<>();
private final Map<String, Compra> compras = new LinkedHashMap<>();
private final Map<String, Venta> ventas = new LinkedHashMap<>();
private final List<MovimientoInventario> movimientos = new ArrayList<>();
private final EstrategiaAsignacion estrategia;
```

Un `Map<K,V>` relaciona una clave con un valor. Aquí las claves son códigos o números de texto, y los valores son los objetos registrados. `LinkedHashMap` conserva el orden de incorporación.

Las colecciones `compras` y `ventas` permiten proteger las operaciones de inventario contra un segundo registro. No representan cantidades adicionales: conservan referencias a las operaciones ya aplicadas. Los repositorios de servicios conservan esas mismas compras y ventas para consultarlas.

El constructor recibe `EstrategiaAsignacion` y rechaza `null` mediante `Objects.requireNonNull`. Puede recibir `new EstrategiaFEFO()` sin depender del nombre concreto de esa clase.

### 8.2. `registrarEntrada(compra, instante)`

Tiene acceso de paquete. `Compra.registrar()` lo llama; consola y servicios utilizan esa operación de la compra.

Primero valida el instante, exige que el día coincida con el de la compra, rechaza una compra previamente registrada y comprueba que el abastecedor continúe activo.

```java
List<MovimientoInventario> entradas = new ArrayList<>();
for (Lote lote : compra.getLotes()) {
    if (lotes.containsKey(lote.getCodigo())) {
        throw new IdentificadorDuplicadoException("Ya existe el lote " + lote.getCodigo() + ".");
    }
    if (!lote.getProducto().estaActivo()) {
        throw new RegistroInactivoException("El producto está inactivo.");
    }
    entradas.add(movimiento(entradas.size(), instante, TipoMovimiento.ENTRADA, lote,
            lote.getCantidadRecibida(), compra.getNumero(), "Recepción de compra"));
}
```

`containsKey()` pregunta si el código ya está en el mapa. `entradas` es una lista temporal: construir movimientos en ella todavía no modifica el historial oficial.

Solo después de completar las comprobaciones se aplican los cambios:

```java
for (Lote lote : compra.getLotes()) { lotes.put(lote.getCodigo(), lote); }
movimientos.addAll(entradas);
compras.put(compra.getNumero(), compra);
```

`put()` asocia código y lote. No se vuelve a sumar la cantidad al lote: ya se estableció en su constructor. `addAll()` incorpora todos los movimientos temporales al historial.

### 8.3. `planificar(producto, cantidad, hoy)`

```java
return estrategia.asignar(producto, cantidad, getLotes(), hoy);
```

Entrega los lotes registrados a la estrategia y devuelve su plan. No modifica inventario. Es el método que utilizará el servicio para preparar todos los detalles de una venta antes de registrarla.

### 8.4. `registrarSalida(venta, instante)`

Lo llama `Venta.registrar()`. Primero valida instante y fecha de la venta, y rechaza una salida ya registrada con ese número.

Obtiene un mapa de cantidades totales por lote mediante `cantidadesDe(venta)`. Después revisa cada lote:

```java
for (Map.Entry<Lote, BigDecimal> entrada : cantidades.entrySet()) {
    Lote lote = entrada.getKey();
    exigirLoteRegistrado(lote);
    if (!lote.puedeVenderse(instante.toLocalDate())) {
        throw new ReglaNegocioException("El lote " + lote.getCodigo() + " no está disponible para venta.");
    }
    if (entrada.getValue().compareTo(lote.getCantidadDisponible()) > 0) {
        throw new StockInsuficienteException("Stock insuficiente en el lote " + lote.getCodigo() + ".");
    }
```

`entrySet()` permite recorrer las parejas de un mapa. `getKey()` obtiene el lote y `getValue()` la cantidad acumulada.

Las salidas se preparan en una lista temporal. Una vez comprobadas todas, otro bucle llama `descontar()` a cada lote, se añaden los movimientos y se conserva la referencia de venta aplicada. Una falta de stock encontrada durante la validación no descuenta los lotes revisados anteriormente.

### 8.5. `devolver(venta, instante)`

Lo llama `Venta.anular()`. Exige una fecha que no preceda a la venta y que la venta sea la misma instancia registrada aquí:

```java
if (instante.toLocalDate().isBefore(venta.getFecha()) || ventas.get(venta.getNumero()) != venta) {
    throw new ReglaNegocioException("La devolución debe corresponder a una venta de este inventario.");
}
```

`!=` compara referencias: otra venta inventada con el mismo número no sirve para reponer mercancía.

Reúne las asignaciones originales, comprueba los lotes y valida que ninguna reposición supere lo recibido. Prepara movimientos `DEVOLUCION` y después aplica `reponer()` a cada lote.

Aquí no se exige que el lote continúe vigente ni que el producto esté activo: anular debe conservar el origen de la devolución. Un lote repuesto pero vencido continúa excluido de existencias vendibles.

La protección contra anular dos veces está en `Venta`, explicada en el bloque siguiente. El inventario aplica la devolución autorizada.

### 8.6. `registrarPerdida(lote, cantidad, motivo, instante)`

Comprueba instante, pertenencia del lote, motivo obligatorio, cantidad válida y stock suficiente. Después:

```java
MovimientoInventario perdida = movimiento(0, instante, TipoMovimiento.PERDIDA,
        lote, cantidad, lote.getCodigo(), motivo);
lote.descontar(cantidad);
movimientos.add(perdida);
```

Se construye el movimiento antes de descontar, para que sus validaciones puedan rechazar la operación sin alterar cantidades. Una pérdida puede retirar mercancía vencida o de un producto desactivado; no es una venta.

Ejemplo: si quedan 17 kg y se pierden 2, quedan 15 y se añade un movimiento `PERDIDA` de 2 kg. Si se solicitan 18, se rechaza y continúan los 17 kg originales.

### 8.7. `disponible(producto, hoy)`

```java
BigDecimal cantidad = BigDecimal.ZERO;
for (Lote lote : lotesDe(producto)) {
    if (lote.puedeVenderse(hoy)) {
        cantidad = cantidad.add(lote.getCantidadDisponible());
    }
}
return cantidad;
```

Empieza en cero, suma los lotes vendibles y devuelve el total. Es disponibilidad para vender, no suma física de todos los lotes. Con 5 kg vencidos y 20 vigentes devuelve 20.

### 8.8. `lotesDe(producto)`

Exige producto, recorre `lotes.values()` y selecciona los que tienen su código. `values()` obtiene los objetos sin las claves del mapa. Devuelve una copia no modificable de la lista.

La consulta incluye lotes agotados y vencidos porque resulta útil para revisar historial y existencias físicas. El filtro de venta se hace en `disponible()` o FEFO.

### 8.9. `proximosAVencer(hoy, dias)`

Rechaza fecha nula o un número de días negativo. Calcula la distancia de cada vencimiento:

```java
long distancia = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());
if (!lote.estaVencido(hoy) && distancia <= dias && lote.getCantidadDisponible().signum() > 0) {
    resultado.add(lote);
}
```

Incluye lotes todavía vigentes con cantidad, hasta el número de días indicado. Con fecha 6 de octubre y `dias = 2`, incluye vencimientos del 7 y 8. Con `dias = 0` no incluye ninguno, porque un lote que vence hoy ya está vencido según la política elegida.

Esta es una consulta operativa de mercancía restante; puede incluir productos inactivos para que también se vea su próximo vencimiento. Ordena por fecha y código, y devuelve la lista protegida.

### 8.10. `consultarLote(codigo)` y lecturas de colecciones

`consultarLote()` rechaza código vacío, elimina espacios con `strip()` y busca con `lotes.get(...)`. Si obtiene `null`, lanza `RegistroNoEncontradoException`. Si lo encuentra, devuelve el lote.

`getLotes()` y `getMovimientos()` devuelven copias no modificables de sus colecciones. Eso evita que el consumidor borre elementos del historial o añada un lote por fuera del registro de compras.

### 8.11. Método privado `cantidadesDe(venta)`

```java
Map<Lote, BigDecimal> resultado = new LinkedHashMap<>();
for (DetalleVenta detalle : venta.getDetalles()) {
    for (AsignacionLote asignacion : detalle.getAsignaciones()) {
        resultado.merge(asignacion.getLote(), asignacion.getCantidad(), BigDecimal::add);
    }
}
return resultado;
```

Tiene dos bucles porque una venta contiene detalles y cada detalle contiene asignaciones. El mapa acumula el total de cada lote.

`merge()` guarda la cantidad si el lote no estaba; si estaba, combina la cantidad anterior con la nueva. `BigDecimal::add` indica cómo combinar: sumándolas. Así se valida el total, aunque varias asignaciones se refieran al mismo lote.

### 8.12. Método privado `exigirLoteRegistrado(lote)`

```java
if (lote == null || lotes.get(lote.getCodigo()) != lote) {
    throw new RegistroNoEncontradoException("El lote no pertenece a este inventario.");
}
```

Comprueba la instancia original, no solo su código. Impide que alguien pase un lote distinto con el mismo código y modifique cantidades fuera de los objetos registrados.

### 8.13. Método privado `validarInstante(instante)`

```java
if (instante == null || !movimientos.isEmpty()
        && instante.isBefore(movimientos.get(movimientos.size() - 1).getFecha())) {
    throw new FechasInvalidasException(
            "La operación requiere fecha y no puede preceder al último movimiento.");
}
```

`size() - 1` es el índice del último movimiento, porque las listas comienzan en cero. Solo lo consulta si la lista tiene elementos.

Exige fecha y orden temporal. `&&` se evalúa antes que `||`: se rechaza si el instante es nulo, o si existe historial y el nuevo instante lo precede. Un reloj de pruebas que retrocede después de registrar movimientos provocará este rechazo.

### 8.14. Método privado `movimiento(...)`

```java
return new MovimientoInventario((long) movimientos.size() + desplazamiento + 1,
        instante, tipo, lote, cantidad, referencia, motivo);
```

Genera el identificador usando el tamaño del historial. `desplazamiento` representa los movimientos temporales que ya se prepararon en la operación actual. Si hay 3 movimientos y se preparan dos más, sus identificadores serán 4 y 5.

`(long)` convierte el tamaño al tipo del identificador. Las operaciones no borran movimientos, por lo que el conteo permanece creciente dentro de esta sesión en memoria.

## 9. Recorrido verificable

Con los dos lotes del bloque anterior, ejecutado el 6 de octubre:

1. Consultar disponible: 25 kg.
2. Planificar 8 kg: asignaciones de 5 y 3.
3. Consultar los lotes otra vez: todavía 5 y 20, porque planificar no modifica.
4. Registrar la venta con esos detalles: quedan 0 y 17.
5. Anularla: vuelven a 5 y 20, y quedan salidas y devoluciones en el historial.

La venta completa se explica en [Bloque 5: ventas y anulación](BLOQUE_05_VENTAS_Y_ANULACION.md). Las consultas mediante servicios están en [Bloque 6: trazabilidad y consultas](BLOQUE_06_TRAZABILIDAD_Y_CONSULTAS.md).

Desde la carpeta que contiene `pom.xml`, ejecuta `mvn test`. Las comprobaciones relevantes son FEFO, desempate, vencidos, stock insuficiente, ausencia de cambios al planificar, pérdidas, orden de movimientos y conservación de cantidades ante un rechazo.

## 10. Qué demuestra sobre POO y SOLID

La encapsulación está en las cantidades privadas y en los métodos de lote con acceso de paquete. La abstracción está en `EstrategiaAsignacion`. El polimorfismo aparece al llamar a la estrategia mediante su interfaz y a `producto.validarCantidad(...)` mediante `Producto`.

El inventario coordina cantidades e historial, mientras FEFO decide el orden del reparto. Esta separación permite cambiar la política de asignación manteniendo el registro de movimientos y sus validaciones.
