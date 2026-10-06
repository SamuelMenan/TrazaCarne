# TrazaCarne: guía de los DTO

Esta guía explica qué es un DTO, por qué el proyecto los usa y qué hace cada uno. Al final se estudia a fondo el más complejo: `OrigenVenta`.

Todos están en [`servicio/dto`](../src/main/java/co/trazacarne/servicio/dto).

## 1. ¿Qué es un DTO?

**DTO** significa *Data Transfer Object* (objeto de transferencia de datos). Es un objeto que **solo transporta datos** de una capa a otra. No tiene reglas de negocio ni comportamiento.

En TrazaCarne hay tres capas:

```
Consola (MenuConsola, Main)   →   Servicios   →   Dominio (Compra, Lote, Venta, Inventario...)
```

Los DTO viajan por la frontera entre la consola y los servicios, en los dos sentidos:

| Sentido | DTO | Para qué |
|---|---|---|
| **Entrada**: consola → servicio | `DatosCompra`, `DatosLoteRecibido`, `DatosVenta`, `ItemVenta` | Llevar lo que escribió el usuario. |
| **Salida**: servicio → consola | `OrigenVenta`, `DestinoLote` | Devolver resultados ya armados y listos para mostrar. |

### ¿Por qué no pasar directamente los objetos del dominio?

1. **De entrada todavía no existen.** Antes de registrar una compra no hay `Compra` ni `Lote`, solo textos y números que escribió el usuario. El servicio usa esos datos para crear los objetos reales y validarlos.
2. **El dominio queda protegido.** Si la consola recibiera un `Lote`, podría intentar usarlo o modificarlo. Con un DTO solo recibe una copia de los datos que necesita mostrar.
3. **Menos parámetros.** `registrarCompra(DatosCompra datos)` es más claro que un método con 9 parámetros sueltos.
4. **La consola no navega el dominio.** El servicio ya resolvió las relaciones (venta → detalle → lote → abastecedor) y entrega una fila plana.

### ¿Por qué son `record`?

Un `record` (Java 16+) es una clase pensada para guardar datos. Con una sola línea, Java genera:

- Los atributos `private final`: el objeto es **inmutable**, no cambia después de crearse.
- El constructor con todos los campos.
- Los métodos de acceso con el mismo nombre del campo. Por ejemplo, se escribe `datos.numero()` y no `datos.getNumero()`.
- `equals`, `hashCode` y `toString`.

```java
public record ItemVenta(String codigoProducto, BigDecimal cantidad) { }
```

Escrita como clase normal, esa línea serían unas 30 líneas.

### ¿Dónde se validan los datos?

Los DTO de este proyecto **casi no validan**. Por ejemplo, `DatosLoteRecibido` acepta una fecha de vencimiento pasada sin quejarse. Las reglas se comprueban en el dominio: `Lote` revisa las fechas y `Producto` revisa las cantidades. Así cada regla está escrita en un solo lugar.

La única validación que sí hacen `DatosCompra` y `DatosVenta` es estructural: que la lista no traiga elementos `null`.

## 2. DTO de entrada

### 2.1 `DatosLoteRecibido`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/DatosLoteRecibido.java)

```java
public record DatosLoteRecibido(String codigoLote, String codigoProducto, BigDecimal cantidad,
                               LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                               LocalDate fechaVencimiento) { }
```

**Qué representa:** un lote que llegó en una compra, tal como lo escribió el usuario.

**Quién lo crea:** `MenuConsola.registrarCompra()`, una vez por cada lote que el usuario ingresa en el ciclo "¿Agregar otro lote?".

**Quién lo usa:** `ServicioCompras.registrarCompra()`. El servicio busca el producto por `codigoProducto` y llama a `compra.agregarLote(...)`, y ahí `Lote` valida todas las fechas.

**Dato a notar:** `fechaSacrificio` puede ser `null` si el abastecedor es proveedor. El DTO no lo decide; lo decide `Lote`.

### 2.2 `DatosCompra`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/DatosCompra.java)

```java
public record DatosCompra(String numero, String nitAbastecedor, List<DatosLoteRecibido> lotes) {
    public DatosCompra {
        if (lotes != null) {
            for (DatosLoteRecibido lote : lotes) {
                if (lote == null) { throw new ReglaNegocioException("Los lotes recibidos no pueden ser nulos."); }
            }
            lotes = List.copyOf(lotes);
        }
    }
}
```

**Qué representa:** una compra completa: número, a quién se compró y la lista de lotes recibidos.

**Quién lo crea:** `MenuConsola.registrarCompra()` y la demostración de `Main`.

**Quién lo usa:** `ServicioCompras.registrarCompra(DatosCompra datos)`.

**Lo particular, el constructor compacto:** `public DatosCompra { ... }` no lleva paréntesis porque es un constructor compacto, propio de los `record`. Se ejecuta **antes** de asignar los campos y sirve para validar o ajustar los valores:

1. Recorre la lista y rechaza cualquier lote `null`.
2. `lotes = List.copyOf(lotes)` cambia la lista recibida por una **copia inmodificable**. Si quien creó el DTO modifica después su lista original, el DTO no se ve afectado.

Si `lotes` es `null`, no lanza error aquí: el servicio lo detecta después y responde "La compra debe contener al menos un lote."

### 2.3 `ItemVenta`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/ItemVenta.java)

```java
public record ItemVenta(String codigoProducto, BigDecimal cantidad) { }
```

**Qué representa:** un producto que pide el cliente y cuánto: kg o unidades, según el producto.

**Quién lo crea:** `MenuConsola.registrarVenta()`, una vez por cada producto ingresado.

**Quién lo usa:** `ServicioVentas.registrarVenta()`. Si llegan dos `ItemVenta` del mismo producto, el servicio suma sus cantidades.

**Dato a notar:** el ítem **no dice de qué lote sale**. El usuario no elige lotes; los elige el sistema con FEFO.

### 2.4 `DatosVenta`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/DatosVenta.java)

```java
public record DatosVenta(String numero, String documentoCliente, List<ItemVenta> items) { ... }
```

**Qué representa:** una venta completa: número, cliente y productos pedidos.

**Quién lo crea:** `MenuConsola.registrarVenta()` y la demostración de `Main`.

**Quién lo usa:** `ServicioVentas.registrarVenta(DatosVenta datos)`.

Tiene el mismo constructor compacto que `DatosCompra`: rechaza ítems `null` y guarda una copia inmodificable de la lista.

## 3. DTO de salida

### 3.1 `DestinoLote`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/DestinoLote.java)

```java
public record DestinoLote(String numeroVenta, LocalDate fecha, EstadoVenta estado,
                          String documentoCliente, String nombreCliente, BigDecimal cantidad) { }
```

**Qué representa:** una venta en la que se usó un lote. Responde a la pregunta *"¿a quién le vendimos carne de este lote?"* (trazabilidad hacia adelante).

**Quién lo crea:** `ServicioTrazabilidad.ventasDelLote(codigo, incluirAnuladas)`, una fila por cada venta que tomó cantidad del lote.

**Quién lo usa:** `MenuConsola`, en Trazabilidad → "2. Destino de un lote".

**Para qué sirve en la vida real:** si un lote resulta contaminado, esta lista dice a qué clientes hay que avisar y cuánto se llevó cada uno.

### 3.2 `OrigenVenta`

Es el más complejo; se explica en la sección 4.

## 4. A fondo: `OrigenVenta`

[Ver archivo](../src/main/java/co/trazacarne/servicio/dto/OrigenVenta.java)

```java
public record OrigenVenta(String numeroVenta, EstadoVenta estado, String codigoProducto,
                          String nombreProducto, String codigoLote, BigDecimal cantidad,
                          String nitAbastecedor, String nombreAbastecedor,
                          LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                          LocalDate fechaVencimiento) { }
```

### 4.1 Por qué es el más complejo

- Tiene **11 campos**, el que más.
- Junta datos de **5 objetos distintos** del dominio en una sola fila.
- Es el centro de la **trazabilidad hacia atrás**, el objetivo principal del proyecto: *"¿de dónde viene la carne que le vendimos a este cliente?"*
- Lo usan **tres lugares**: el menú de trazabilidad, `mostrarVenta` en la consola y la demostración de `Main`.

### 4.2 De dónde sale cada campo

Una venta no guarda directamente de qué abastecedor vino la carne. Para saberlo hay que recorrer varias relaciones:

```
Venta ──► DetalleVenta ──► AsignacionLote ──► Lote ──► Compra ──► Abastecedor
           (producto)       (cantidad)        (fechas)            (NIT, nombre)
```

| Campo | Objeto de origen | Cómo se obtiene |
|---|---|---|
| `numeroVenta` | Venta | `venta.getNumero()` |
| `estado` | Venta | `venta.getEstado()` |
| `codigoProducto` | DetalleVenta → Producto | `detalle.getProducto().getCodigo()` |
| `nombreProducto` | DetalleVenta → Producto | `detalle.getProducto().getNombre()` |
| `codigoLote` | AsignacionLote → Lote | `lote.getCodigo()` |
| `cantidad` | AsignacionLote | `asignacion.getCantidad()` |
| `nitAbastecedor` | Lote → Compra → Abastecedor | `lote.getAbastecedor().getNit()` |
| `nombreAbastecedor` | Lote → Compra → Abastecedor | `lote.getAbastecedor().getNombre()` |
| `fechaSacrificio` | Lote | `lote.getFechaSacrificio()` |
| `fechaProcesamiento` | Lote | `lote.getFechaProcesamiento()` |
| `fechaVencimiento` | Lote | `lote.getFechaVencimiento()` |

Atención a `cantidad`: es lo que salió **de ese lote**, no el total del producto. Por eso viene de `AsignacionLote` y no de `DetalleVenta`.

### 4.3 Dónde se construye

En `ServicioTrazabilidad.origenDeVenta(numero)`:

```java
for (DetalleVenta detalle : venta.getDetalles()) {               // cada producto de la venta
    for (AsignacionLote asignacion : detalle.getAsignaciones()) { // cada lote de ese producto
        Lote lote = asignacion.getLote();
        Abastecedor origen = lote.getAbastecedor();
        resultado.add(new OrigenVenta(venta.getNumero(), venta.getEstado(),
                detalle.getProducto().getCodigo(), detalle.getProducto().getNombre(),
                lote.getCodigo(), asignacion.getCantidad(), origen.getNit(), origen.getNombre(),
                lote.getFechaSacrificio(), lote.getFechaProcesamiento(), lote.getFechaVencimiento()));
    }
}
```

Son **dos ciclos anidados**, así que se genera **una fila por cada par (producto, lote)**. No es una fila por venta ni una por producto.

- Una venta de 1 producto que sale de 1 lote da 1 fila.
- Una venta de 1 producto que FEFO repartió en 2 lotes da 2 filas.
- Una venta de 2 productos, con 2 lotes cada uno, da 4 filas.

### 4.4 Ejemplo con la demostración

En `Main --demo` se compran dos lotes de carne molida (P-001) al Matadero Central y se venden 8 kg. FEFO toma 5 kg de L-001, que vence primero, y 3 kg de L-002. `origenDeVenta("V-001")` devuelve:

| numeroVenta | estado | producto | lote | cantidad | abastecedor | sacrificio | procesamiento | vencimiento |
|---|---|---|---|---|---|---|---|---|
| V-001 | REGISTRADA | P-001 Carne molida | L-001 | 5 | 9001 Matadero Central | 2026-10-01 | 2026-10-02 | 2026-10-10 |
| V-001 | REGISTRADA | P-001 Carne molida | L-002 | 3 | 9001 Matadero Central | 2026-10-01 | 2026-10-02 | 2026-10-15 |

La salida en consola de la demo lo confirma:

```
Venta V-001 | lote L-001 | cantidad: 5 kg
Venta V-001 | lote L-002 | cantidad: 3 kg
```

### 4.5 Por qué una fila "plana" y no devolver los objetos

El servicio podría devolver la `Venta` y que la consola navegara `venta.getDetalles().get(0).getAsignaciones()...`. Se usa el DTO porque:

1. **La consola no necesita conocer la estructura interna del dominio.** Si mañana `Lote` cambia la forma de guardar el abastecedor, solo cambia `ServicioTrazabilidad`; la consola sigue leyendo `origen.nitAbastecedor()`.
2. **Es una foto del momento.** El record es inmutable, así que nadie puede alterar un lote a través del resultado de una consulta.
3. **Mostrarlo es directo.** Cada fila trae todo lo necesario para imprimir una línea en la consola.

### 4.6 El campo `estado`

Se incluye para que quien consulte sepa si esa venta sigue vigente. Si la venta se anula, `origenDeVenta` sigue mostrando de qué lotes salió, pero con estado `ANULADA`. El historial de trazabilidad nunca se pierde, aunque el stock ya haya vuelto a los lotes.

## 5. Resumen

| DTO | Sentido | Lo crea | Lo recibe o lo usa | En una frase |
|---|---|---|---|---|
| `DatosLoteRecibido` | Entrada | MenuConsola | ServicioCompras | Un lote que llegó, sin validar. |
| `DatosCompra` | Entrada | MenuConsola, Main | ServicioCompras | Compra completa con sus lotes. |
| `ItemVenta` | Entrada | MenuConsola | ServicioVentas | Producto y cantidad pedidos, sin lote. |
| `DatosVenta` | Entrada | MenuConsola, Main | ServicioVentas | Venta completa con sus ítems. |
| `DestinoLote` | Salida | ServicioTrazabilidad | MenuConsola | A quién se vendió un lote. |
| `OrigenVenta` | Salida | ServicioTrazabilidad | MenuConsola, Main | De qué lote y abastecedor salió cada parte de una venta. |
