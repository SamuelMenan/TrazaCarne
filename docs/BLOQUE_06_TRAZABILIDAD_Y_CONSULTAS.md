# Bloque 6: trazabilidad y consultas, explicado archivo por archivo

Este bloque permite preguntar cuánto queda, qué lotes vencen pronto y qué movimientos ocurrieron. También reconstruye el origen de una venta y las ventas y clientes que recibieron cantidades de un lote.

La información necesaria se conservó en los bloques anteriores. No intentamos deducir el origen usando únicamente la cantidad actual: una venta mantiene sus asignaciones incluso cuando un lote queda agotado o la venta se anula.

## 1. De dónde salen estas operaciones

Las consultas de inventario y pérdidas corresponden a RF-06 y HU-07; el control de cantidades e historial se conecta con RN-07 y RN-10. La trazabilidad corresponde a RF-07, HU-08, RN-03, RN-09 y RN-10, según el plan.

Los recorridos son:

```text
Origen:
Venta → DetalleVenta → AsignacionLote → Lote → Compra → Abastecedor

Destino:
Lote ← AsignacionLote ← DetalleVenta ← Venta → Cliente
```

Una consulta histórica conserva ventas anuladas indicando su estado. Para consultar solo ventas vigentes, el servicio ofrece un parámetro explícito que las excluye.

## 2. Archivo `ServicioInventario.java`

[Abrir ServicioInventario.java](../src/main/java/co/trazacarne/servicio/ServicioInventario.java).

### 2.1. Paquete, imports y dependencias

```java
package co.trazacarne.servicio;
```

El servicio pertenece a la capa que coordina operaciones. Importa `Inventario`, `Lote` y `MovimientoInventario` del dominio; `BigDecimal` para el resultado de cantidades; tipos de fecha y reloj; y colecciones para devolver resultados.

```java
private final Inventario inventario;
private final ServicioProductos productos;
private final Clock reloj;
```

Recibe el inventario compartido, el servicio que busca productos y el reloj. `private final` protege las referencias para que el servicio no cambie de dependencias después de construirse.

El constructor comprueba las tres con `Objects.requireNonNull`. Esto evita crear un servicio incapaz de funcionar porque falta un objeto al conectar la aplicación.

### 2.2. `existencias(codigoProducto)`

```java
public List<Lote> existencias(String codigoProducto) {
    return inventario.lotesDe(productos.consultar(codigoProducto));
}
```

La lectura se evalúa de dentro hacia afuera:

1. Buscar el producto mediante `productos.consultar(...)`.
2. Entregar ese producto a `inventario.lotesDe(...)`.
3. Devolver la lista de sus lotes.

Utiliza `consultar`, no `consultarActivo`, porque revisar existencias históricas de un producto desactivado debe seguir siendo posible.

El resultado incluye lotes agotados o vencidos. Cada uno permite consultar su cantidad disponible física, cantidad recibida y fechas. No significa que todos se puedan vender.

### 2.3. `disponible(codigoProducto)`

```java
public BigDecimal disponible(String codigoProducto) {
    return inventario.disponible(
            productos.consultar(codigoProducto), LocalDate.now(reloj));
}
```

Busca el producto y toma el día actual del reloj. El inventario suma solo lotes que pueden venderse ese día.

Ejemplo: existen 5 kg vencidos y 20 kg vigentes. `existencias()` muestra ambos lotes; `disponible()` devuelve 20. Si el producto está inactivo, la consulta sigue permitida, pero lo vendible es cero porque `Lote.puedeVenderse()` exige producto activo.

### 2.4. `lotes()` y `lote(codigo)`

```java
public List<Lote> lotes() { return inventario.getLotes(); }
public Lote lote(String codigo) { return inventario.consultarLote(codigo); }
```

El primero devuelve todos los lotes registrados. El segundo busca uno por código y utiliza las validaciones del inventario: si el código está vacío o no existe, se lanza la excepción correspondiente.

No repetimos aquí esas comprobaciones porque ya están dentro del método llamado.

### 2.5. `proximosAVencer(dias)`

```java
return inventario.proximosAVencer(LocalDate.now(reloj), dias);
```

Toma la fecha actual y solicita los lotes vigentes con cantidad que vencen dentro de la ventana indicada. Un valor negativo de `dias` es rechazado por el inventario.

Con fecha 6 de octubre y ventana de 2 días se consideran vencimientos del 7 y 8. Los que vencen el 6 ya están vencidos. La lista queda ordenada por vencimiento y código.

### 2.6. `registrarPerdida(codigoLote, cantidad, motivo)`

```java
inventario.registrarPerdida(
        lote(codigoLote), cantidad, motivo, LocalDateTime.now(reloj));
```

Busca el lote original, obtiene el instante y solicita la operación al inventario. La validación de motivo, cantidad, disponibilidad y orden temporal está en `Inventario.registrarPerdida()`, explicada en el [bloque 4](BLOQUE_04_INVENTARIO_Y_FEFO.md).

El método es `void` porque ejecuta una operación sin devolver un objeto. La pérdida crea un movimiento; no borra el lote ni sus ventas anteriores.

### 2.7. `movimientos(codigoLote)`

```java
Lote lote = lote(codigoLote);
List<MovimientoInventario> resultado = new ArrayList<>();
for (MovimientoInventario movimiento : inventario.getMovimientos()) {
    if (movimiento.getLote() == lote) {
        resultado.add(movimiento);
    }
}
return List.copyOf(resultado);
```

Primero obtiene el lote original. Después recorre el historial y reúne los movimientos que lo mencionan.

Aquí `==` compara referencias a objetos: se busca el mismo lote registrado, no un texto. Eso es distinto de comparar códigos `String`, para los que usamos `equals()`.

Devuelve una lista no modificable y conserva el orden del historial. Después de compra, venta y anulación de un lote, se pueden ver entrada, salida y devolución, cada una con cantidad, fecha, referencia y motivo.

## 3. Archivo `OrigenVenta.java`

[Abrir OrigenVenta.java](../src/main/java/co/trazacarne/servicio/dto/OrigenVenta.java).

```java
public record OrigenVenta(String numeroVenta, EstadoVenta estado, String codigoProducto,
                          String nombreProducto, String codigoLote, BigDecimal cantidad,
                          String nitAbastecedor, String nombreAbastecedor,
                          LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                          LocalDate fechaVencimiento) { }
```

Este es un DTO **de salida**. Sus imports permiten utilizar el estado del dominio, cantidades y fechas. No contiene reglas de descuento ni reposición: transporta una fila ya calculada por el servicio.

Java genera constructor y accesores para los componentes:

| Componente y accesor | Significado |
|---|---|
| `numeroVenta()` | Venta consultada. |
| `estado()` | Registrada o anulada en el momento de consultar. |
| `codigoProducto()` y `nombreProducto()` | Producto del detalle. |
| `codigoLote()` | Lote del que salió esa parte. |
| `cantidad()` | Cantidad tomada de ese lote, no el total del detalle. |
| `nitAbastecedor()` y `nombreAbastecedor()` | Origen conservado por la compra. |
| Accesores de fechas | Sacrificio, procesamiento y vencimiento originales del lote. |

Para una venta de 8 kg repartidos 5 + 3, se devuelven dos objetos `OrigenVenta`. Una fila por asignación facilita mostrar el reparto sin perder información.

Los nombres se leen de los objetos cuando se consulta. Por ejemplo, si se actualizó el nombre del abastecedor, la fila muestra ese nombre actual. El NIT, el lote y las fechas mantienen la relación histórica; esta versión no guarda un histórico de todos los nombres anteriores.

## 4. Archivo `DestinoLote.java`

[Abrir DestinoLote.java](../src/main/java/co/trazacarne/servicio/dto/DestinoLote.java).

```java
public record DestinoLote(String numeroVenta, LocalDate fecha, EstadoVenta estado,
                          String documentoCliente, String nombreCliente, BigDecimal cantidad) { }
```

Agrupa una fila por venta que utilizó el lote consultado. Sus accesores son `numeroVenta()`, `fecha()`, `estado()`, `documentoCliente()`, `nombreCliente()` y `cantidad()`.

La cantidad es lo que salió de ese lote para esa venta. Una venta de 8 kg puede mostrar 5 kg al consultar `L-001` y 3 kg al consultar `L-002`.

Si la venta está anulada, esa cantidad sigue describiendo el hecho original. No es un balance neto después de devoluciones: el campo `estado()` indica que la operación fue revertida, y los movimientos contienen la devolución.

Como `OrigenVenta`, este record de salida se construye con información de objetos ya validados. No necesita repetir las reglas de sus entidades.

## 5. Archivo `ServicioTrazabilidad.java`

[Abrir ServicioTrazabilidad.java](../src/main/java/co/trazacarne/servicio/ServicioTrazabilidad.java).

### 5.1. Dependencias, constructor e imports

```java
private final Repositorio<Venta, String> ventas;
private final Inventario inventario;
```

Recibe el mismo repositorio de ventas y el mismo inventario utilizados por los otros servicios. El constructor los exige con `Objects.requireNonNull`.

Los imports del dominio permiten recorrer las relaciones. Los DTO permiten construir resultados de salida. `BigDecimal`, listas y mapas se utilizan para acumular cantidades y eliminar clientes repetidos.

No necesita reloj: responde sobre hechos guardados, no decide si hoy puede venderse un lote. Tampoco descuenta ni repone cantidades.

### 5.2. `origenDeVenta(numero)`

Primero valida el número y busca la venta:

```java
if (numero == null || numero.isBlank()) {
    throw new ReglaNegocioException("El número de venta es obligatorio.");
}
Venta venta = ventas.buscarPorId(numero.strip()).orElseThrow(() ->
        new RegistroNoEncontradoException("No existe la venta " + numero.strip() + "."));
```

`strip()` quita espacios. `orElseThrow()` devuelve el resultado si existe o utiliza la lambda `() ->` para crear la excepción si falta.

Después recorre los detalles y sus asignaciones:

```java
List<OrigenVenta> resultado = new ArrayList<>();
for (DetalleVenta detalle : venta.getDetalles()) {
    for (AsignacionLote asignacion : detalle.getAsignaciones()) {
        Lote lote = asignacion.getLote();
        Abastecedor origen = lote.getAbastecedor();
        resultado.add(new OrigenVenta(venta.getNumero(), venta.getEstado(),
                detalle.getProducto().getCodigo(), detalle.getProducto().getNombre(),
                lote.getCodigo(), asignacion.getCantidad(), origen.getNit(), origen.getNombre(),
                lote.getFechaSacrificio(), lote.getFechaProcesamiento(), lote.getFechaVencimiento()));
    }
}
return List.copyOf(resultado);
```

Cada variable representa una parte del recorrido:

- `venta`: operación consultada.
- `detalle`: producto vendido.
- `asignacion`: parte de su cantidad procedente de un lote.
- `lote`: objeto que conserva fechas y compra.
- `origen`: abastecedor obtenido a través de esa compra.
- `resultado`: filas que se devolverán al consumidor.

Un doble `for` no significa una segunda consulta de inventario: navega listas conservadas en la venta. La cantidad sale de `asignacion.getCantidad()`, por lo que el resultado sigue siendo correcto aunque el lote ahora tenga cero.

No exige que cliente, producto o abastecedor estén activos. Su desactivación no elimina relaciones históricas. Una venta anulada conserva sus filas con estado `ANULADA`.

### 5.3. `ventasDelLote(codigo, incluirAnuladas)`

Primero consulta el lote y después recorre todas las ventas guardadas. El filtro es:

```java
if (venta.getEstado() == EstadoVenta.BORRADOR
        || !incluirAnuladas && venta.getEstado() == EstadoVenta.ANULADA) {
    continue;
}
```

`continue` salta a la siguiente vuelta del bucle. Omite borradores porque todavía no movieron mercancía. Omite anuladas solo cuando se solicita `incluirAnuladas = false`.

`&&` tiene prioridad sobre `||`: se interpreta como «es borrador, o es anulada y no queremos incluir anuladas».

Luego obtiene cuánto utilizó esa venta del lote:

```java
BigDecimal cantidad = cantidadDelLote(venta, lote);
if (cantidad.signum() > 0) {
    resultado.add(new DestinoLote(venta.getNumero(), venta.getFecha(), venta.getEstado(),
            venta.getCliente().getDocumento(), venta.getCliente().getNombre(), cantidad));
}
```

Si la cantidad es positiva, construye la fila con cliente, estado y cantidad. Si vale cero, esa venta no utilizó el lote y no aparece. Devuelve `List.copyOf(resultado)` para proteger la lista.

### 5.4. `ventasDelLote(codigo)`

```java
public List<DestinoLote> ventasDelLote(String codigo) {
    return ventasDelLote(codigo, true);
}
```

Este es un caso de **sobrecarga de métodos**: mismo nombre, distintos parámetros. La versión corta llama a la completa y elige incluir anuladas por defecto.

Uso:

```java
List<DestinoLote> historial = trazabilidad.ventasDelLote("L-001");
List<DestinoLote> vigentes = trazabilidad.ventasDelLote("L-001", false);
```

La primera consulta conserva el historial; la segunda muestra únicamente ventas registradas vigentes que utilizaron ese lote.

### 5.5. `clientesDelLote(codigo)`

```java
Lote lote = inventario.consultarLote(codigo);
Map<String, Cliente> resultado = new LinkedHashMap<>();
for (Venta venta : ventas.listar()) {
    if (venta.getEstado() != EstadoVenta.BORRADOR
            && cantidadDelLote(venta, lote).signum() > 0) {
        resultado.put(venta.getCliente().getDocumento(), venta.getCliente());
    }
}
return List.copyOf(resultado.values());
```

El mapa utiliza el documento del cliente como clave. Si el mismo cliente participó en tres ventas del lote, las tres usan esa clave y el resultado contiene un solo cliente.

Incluye relaciones de ventas anuladas porque esta es una consulta histórica. Para ver cantidades y distinguir sus estados utiliza `ventasDelLote()`. `clientesDelLote()` solo proporciona los clientes distintos, sin cantidades ni estados de venta.

`resultado.values()` obtiene los clientes del mapa y `List.copyOf()` los convierte en una lista protegida.

### 5.6. Método privado `cantidadDelLote(venta, lote)`

```java
BigDecimal cantidad = BigDecimal.ZERO;
for (DetalleVenta detalle : venta.getDetalles()) {
    for (AsignacionLote asignacion : detalle.getAsignaciones()) {
        if (asignacion.getLote() == lote) {
            cantidad = cantidad.add(asignacion.getCantidad());
        }
    }
}
return cantidad;
```

Empieza en cero y suma solo asignaciones que apuntan al lote consultado. Utiliza identidad de objeto porque inventario y ventas comparten los mismos lotes originales.

Es privado porque sirve de apoyo a las consultas del servicio. No es necesario ofrecerlo directamente a la consola. Si la venta no utilizó el lote, devuelve cero.

## 6. Ejemplo de salida que puedes comprobar

Tras comprar 5 kg en `L-001`, 20 en `L-002` y vender 8 kg al cliente `1001`:

```java
List<OrigenVenta> origen = trazabilidad.origenDeVenta("V-001");
List<DestinoLote> destino = trazabilidad.ventasDelLote("L-001");
List<MovimientoInventario> movimientos = servicioInventario.movimientos("L-001");
```

Resultado conceptual:

| Consulta | Información relevante |
|---|---|
| Origen de `V-001` | Dos filas: 5 kg de `L-001` y 3 kg de `L-002`, con abastecedor y fechas. |
| Destino de `L-001` | Una fila para `V-001`, cliente `1001`, cantidad 5 kg. |
| Movimientos de `L-001` | Entrada de 5 y salida de 5. |

Después de anular:

| Consulta | Resultado |
|---|---|
| Origen de `V-001` | Conserva las dos filas y muestra estado `ANULADA`. |
| `ventasDelLote("L-001")` | Conserva la fila anulada. |
| `ventasDelLote("L-001", false)` | Excluye esa fila. |
| Movimientos de `L-001` | Entrada, salida y devolución de 5 kg. |
| `clientesDelLote("L-001")` | Conserva al cliente `1001` como relación histórica. |

La consulta no modifica cantidades, ventas ni estados. Una lista vacía significa que el lote existe pero no tiene destinos para el filtro utilizado. Un código de lote inexistente produce una excepción; no se confunde con «sin ventas».

## 7. Errores y límites prácticos

| Operación | Entrada inválida | Resultado |
|---|---|---|
| Consultar disponibilidad | Producto inexistente | Excepción del servicio de productos. |
| Consultar lote o sus movimientos | Lote inexistente | `RegistroNoEncontradoException`. |
| Próximos vencimientos | Días negativos | `ReglaNegocioException`. |
| Registrar pérdida | Motivo vacío, cantidad inválida o superior al stock | Rechazo sin descuento ni movimiento. |
| Consultar origen | Número vacío o inexistente | Excepción con mensaje de negocio. |

Las cantidades disponibles se consultan para la fecha actual. Los movimientos y asignaciones conservan hechos históricos; esta versión no reconstruye un inventario completo «tal como estaba» en cualquier fecha pasada.

Los datos permanecen en memoria durante la sesión. Reiniciar la aplicación comienza otra sesión y no recupera compras o ventas de la anterior. El almacenamiento persistente queda fuera del alcance inicial.

## 8. Orden de lectura y verificación

Lee primero `ServicioInventario`, siguiendo sus llamadas a `Inventario`. Después revisa los dos records de salida y recorre `ServicioTrazabilidad.origenDeVenta()`. Finalmente estudia el filtro de anuladas y el mapa que evita clientes repetidos.

Ejecuta `mvn test` desde la carpeta de `pom.xml`. Los escenarios más útiles son origen repartido entre lotes, destino con ventas anuladas y vigentes, cliente repetido, relaciones después de desactivar participantes, pérdida y próximos vencimientos.

La consola y `Main` conectan estas operaciones con las opciones que ve el vendedor. Su explicación está en [Bloque 7: consola y arranque](BLOQUE_07_CONSOLA_Y_MAIN.md).
