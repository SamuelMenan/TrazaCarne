# Bloque 5: ventas y anulación, explicado archivo por archivo

Este bloque une productos, clientes, lotes e inventario. Registra una venta completa, conserva el precio aplicado y guarda de qué lotes salió cada cantidad. También permite anular una venta devolviendo exactamente a esos lotes, una sola vez.

Antes de leerlo conviene conocer [compras y lotes](BLOQUE_03_COMPRAS_Y_LOTES.md) e [inventario y FEFO](BLOQUE_04_INVENTARIO_Y_FEFO.md). FEFO prepara asignaciones; registrar una venta es el paso que las aplica.

## 1. De dónde sale el código

El plan relaciona ventas con RF-05, HU-05 y RN-05 a RN-10. La anulación corresponde a HU-06, RN-10 y RN-11. Esas necesidades se traducen así:

| Necesidad | Archivo o método que la implementa |
|---|---|
| Recibir productos y cantidades solicitadas | `DatosVenta` e `ItemVenta`. |
| Conservar cantidad, precio y origen por producto | `DetalleVenta`. |
| Representar una operación para un cliente | `Venta`. |
| Rechazar stock insuficiente sin descuentos parciales | Planificación de todos los detalles y validación del inventario. |
| Devolver a los mismos lotes | `Venta.anular()` e `Inventario.devolver()`. |
| Impedir segunda anulación | Estado de `Venta` y `VentaYaAnuladaException`. |
| Conservar historial | Mantener venta y movimientos; cambiar estado. |

## 2. Archivo `ItemVenta.java`

[Abrir ItemVenta.java](../src/main/java/co/trazacarne/servicio/dto/ItemVenta.java).

```java
package co.trazacarne.servicio.dto;

import java.math.BigDecimal;

public record ItemVenta(String codigoProducto, BigDecimal cantidad) { }
```

Agrupa una línea solicitada por el usuario. No es todavía un detalle de venta registrado: contiene un código y una cantidad, sin precio ni asignaciones de lote.

`record` genera constructor y métodos de lectura `codigoProducto()` y `cantidad()`. `BigDecimal` conserva la cantidad decimal exacta.

Ejemplo:

```java
ItemVenta item = new ItemVenta("P-001", new BigDecimal("8"));
```

El DTO no consulta el producto. El servicio utiliza su código para buscarlo y le pide validar la cantidad.

## 3. Archivo `DatosVenta.java`

[Abrir DatosVenta.java](../src/main/java/co/trazacarne/servicio/dto/DatosVenta.java).

```java
public record DatosVenta(String numero, String documentoCliente, List<ItemVenta> items) {
    public DatosVenta {
        if (items != null) {
            for (ItemVenta item : items) {
                if (item == null) {
                    throw new ReglaNegocioException("Los productos solicitados no pueden ser nulos.");
                }
            }
            items = List.copyOf(items);
        }
    }
}
```

El paquete `servicio.dto` expresa su función de transportar datos. Importa `List` y la excepción para entradas estructuralmente inválidas.

El constructor compacto revisa que no haya elementos nulos y protege la lista mediante `List.copyOf`. Una lista nula o vacía será rechazada en el servicio con un mensaje específico. Java asigna los componentes después del cuerpo del constructor.

Ejemplo:

```java
DatosVenta datos = new DatosVenta(
        "V-001", "1001", List.of(new ItemVenta("P-001", new BigDecimal("8"))));
```

Los accesores son `numero()`, `documentoCliente()` e `items()`. No hay setters. Cambiar posteriormente la lista original no cambia el contenido del DTO.

## 4. Archivo `DetalleVenta.java`

[Abrir DetalleVenta.java](../src/main/java/co/trazacarne/dominio/DetalleVenta.java).

### 4.1. Atributos

```java
public final class DetalleVenta {
    private final Producto producto;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final List<AsignacionLote> asignaciones;
```

La clase pertenece al dominio e importa cantidades, redondeo, listas y conjuntos. `final` en la clase impide herencia; en los atributos impide reasignarlos después de construir el detalle.

Conserva el producto, la cantidad total solicitada, el precio que se aplicó y el reparto entre lotes. El producto puede actualizar su precio después; `precioUnitario` seguirá teniendo el valor que se obtuvo al preparar este detalle.

### 4.2. Comprobaciones iniciales del constructor

```java
if (producto == null || asignaciones == null || asignaciones.isEmpty()) {
    throw new ReglaNegocioException("El detalle requiere producto y asignaciones de origen.");
}
producto.validarCantidad(cantidad);
BigDecimal suma = BigDecimal.ZERO;
Set<String> codigos = new HashSet<>();
```

Exige producto, origen y cantidad válida. `suma` empieza en cero para acumular lo asignado. `codigos` permite detectar un lote repetido en el detalle.

### 4.3. Revisar cada asignación

```java
for (AsignacionLote asignacion : asignaciones) {
    if (asignacion == null) {
        throw new ReglaNegocioException("Las asignaciones no pueden ser nulas.");
    }
    Lote lote = asignacion.getLote();
    if (!lote.getProducto().getCodigo().equals(producto.getCodigo())
            || !codigos.add(lote.getCodigo())) {
        throw new ReglaNegocioException("Los lotes del detalle deben ser del producto y no repetirse.");
    }
    suma = suma.add(asignacion.getCantidad());
}
```

`asignacion` es el elemento actual. El método extrae su lote y exige que sea del producto del detalle. No se permite vender carne molida usando una asignación de otro producto.

`codigos.add()` devuelve `false` si el código ya estaba, de modo que `!codigos.add(...)` detecta la repetición. `suma.add(...)` devuelve una nueva cantidad acumulada; hay que reasignarla porque `BigDecimal` es inmutable.

### 4.4. Total asignado y precio histórico

```java
if (suma.compareTo(cantidad) != 0) {
    throw new ReglaNegocioException("Las asignaciones deben sumar la cantidad del detalle.");
}
this.producto = producto;
this.cantidad = cantidad;
this.precioUnitario = producto.getPrecio();
this.asignaciones = List.copyOf(asignaciones);
```

`compareTo() != 0` significa que los números son diferentes. Se rechaza tanto una suma menor como una mayor que lo vendido. Para 8 kg, 5 + 3 es válido; 5 + 2 no.

`precioUnitario` guarda una referencia a un `BigDecimal` inmutable. Cuando el producto actualiza precio, reasigna su atributo a otro valor: no modifica el valor conservado aquí.

La copia de lista impide añadir o quitar asignaciones después. No clona los lotes: mantiene el vínculo con los objetos originales del inventario.

### 4.5. `calcularSubtotal()`

```java
return precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
```

Multiplica el precio guardado por la cantidad y redondea el subtotal a dos decimales. `HALF_UP` redondea al valor más próximo y, en un empate, aumenta la magnitud. Por ejemplo, `1.005` se convierte en `1.01`.

En esta implementación cada detalle se redondea primero, y el total suma esos subtotales. Se utiliza COP como moneda de presentación; las clases no implementan conversión entre monedas.

### 4.6. Getters

`getProducto`, `getCantidad`, `getPrecioUnitario` y `getAsignaciones` permiten leer los datos. La lista devuelta ya está protegida desde el constructor.

## 5. Archivo `EstadoVenta.java`

[Abrir EstadoVenta.java](../src/main/java/co/trazacarne/dominio/EstadoVenta.java).

```java
public enum EstadoVenta {
    BORRADOR, REGISTRADA, ANULADA
}
```

`BORRADOR` se añadió para representar claramente una venta preparada que todavía no ha descontado inventario.

```text
BORRADOR ── registrar correctamente ──→ REGISTRADA
REGISTRADA ── devolver correctamente ──→ ANULADA
```

No hay una operación que devuelva una venta anulada al estado registrado. Un fallo durante el registro deja el borrador; un fallo de anulación conserva el estado registrado.

## 6. Archivo `Venta.java`

[Abrir Venta.java](../src/main/java/co/trazacarne/dominio/Venta.java).

### 6.1. Datos y estado

```java
private final String numero;
private final LocalDate fecha;
private final Cliente cliente;
private final List<DetalleVenta> detalles;
private EstadoVenta estado = EstadoVenta.BORRADOR;
```

El número, día, cliente y detalles permanecen fijos. El estado cambia únicamente mediante las operaciones autorizadas.

Los imports incluyen excepciones de negocio, cantidades, fechas y un `Set` para detectar productos repetidos.

### 6.2. Constructor

Comprueba número no vacío, fecha, cliente y al menos un detalle. Después valida la lista:

```java
Set<String> productos = new HashSet<>();
for (DetalleVenta detalle : detalles) {
    if (detalle == null || !productos.add(detalle.getProducto().getCodigo())) {
        throw new ReglaNegocioException("Los detalles no pueden ser nulos ni repetir productos.");
    }
}
```

Una venta del dominio contiene un único detalle por producto. Eso no impide que el usuario lo solicite en varias líneas: el servicio las agrupa antes de construir esta entidad.

Al terminar, el constructor elimina espacios externos del número y guarda `List.copyOf(detalles)`. Crear este objeto no genera movimientos todavía.

### 6.3. `registrar(inventario, instante)`

```java
if (estado != EstadoVenta.BORRADOR || inventario == null) {
    throw new ReglaNegocioException("Solo una venta en borrador puede registrarse en un inventario.");
}
if (!cliente.estaActivo()) {
    throw new RegistroInactivoException("El cliente está inactivo.");
}
```

Exige un inventario y un borrador. También exige cliente activo para una nueva operación. Recorre los detalles y exige productos activos.

```java
inventario.registrarSalida(this, instante);
estado = EstadoVenta.REGISTRADA;
```

Entrega esta venta al inventario. **Primero** se aplican descuentos y movimientos; **después** cambia el estado. Si el inventario rechaza disponibilidad, la segunda línea no se ejecuta.

### 6.4. `anular(inventario, instante)`

```java
if (estado == EstadoVenta.ANULADA) {
    throw new VentaYaAnuladaException("La venta " + numero + " ya fue anulada.");
}
if (estado != EstadoVenta.REGISTRADA || inventario == null) {
    throw new ReglaNegocioException("Solo una venta registrada puede anularse.");
}
inventario.devolver(this, instante);
estado = EstadoVenta.ANULADA;
```

La primera condición distingue específicamente la segunda anulación. La siguiente rechaza un borrador o falta de inventario.

El inventario recupera las asignaciones originales para reponer. Una vez completado, cambia el estado. No exige cliente o producto activo porque la operación revierte un hecho histórico.

Una devolución a un lote vencido sigue siendo una devolución válida. El lote recupera cantidad, pero `puedeVenderse()` continúa devolviendo `false`.

### 6.5. `calcularTotal()`

```java
BigDecimal total = new BigDecimal("0.00");
for (DetalleVenta detalle : detalles) {
    total = total.add(detalle.calcularSubtotal());
}
return total;
```

Acumula los subtotales guardados. No utiliza el precio actual de cada producto. Una venta anulada conserva sus detalles y total para consulta histórica; el estado indica que fue revertida.

### 6.6. Getters

`getNumero`, `getFecha`, `getCliente`, `getDetalles` y `getEstado` ofrecen lecturas. No hay `setEstado` ni métodos para añadir detalles a una venta creada, evitando alterar silenciosamente sus cantidades u origen.

## 7. Archivo `VentaYaAnuladaException.java`

[Abrir VentaYaAnuladaException.java](../src/main/java/co/trazacarne/excepcion/VentaYaAnuladaException.java).

```java
public class VentaYaAnuladaException extends ReglaNegocioException {
    public VentaYaAnuladaException(String mensaje) {
        super(mensaje);
    }
}
```

Identifica un intento de anular nuevamente. El mensaje explica cuál venta está anulada. La condición se comprueba en `Venta`; la excepción transporta el rechazo y detiene el flujo normal.

## 8. Archivo `ServicioVentas.java`

[Abrir ServicioVentas.java](../src/main/java/co/trazacarne/servicio/ServicioVentas.java).

### 8.1. Dependencias e imports

El servicio necesita repositorio de ventas, servicio de clientes, servicio de productos, inventario y reloj. Son atributos `private final`, entregados por constructor y comprobados con `Objects.requireNonNull`.

Importa los DTO para recibir solicitudes y las entidades para construir el resultado. `LinkedHashMap` conserva el orden de los productos solicitados y ayuda a agruparlos. `LocalDateTime.now(reloj)` toma un instante consistente de operación.

### 8.2. Inicio de `registrarVenta(datos)`

Primero rechaza DTO ausente o lista sin productos, normaliza número y detecta un número ya utilizado. Después busca al cliente activo y captura el instante.

Si el cliente no existe o está desactivado, los servicios anteriores lanzan la excepción. No hace falta repetir aquí las condiciones de esa búsqueda.

### 8.3. Agrupar líneas repetidas sin ocultar cantidades inválidas

```java
Map<String, Producto> porCodigo = new LinkedHashMap<>();
Map<String, BigDecimal> cantidades = new LinkedHashMap<>();
for (ItemVenta item : datos.items()) {
    Producto producto = productos.consultarActivo(item.codigoProducto());
    producto.validarCantidad(item.cantidad());
    porCodigo.put(producto.getCodigo(), producto);
    cantidades.merge(producto.getCodigo(), item.cantidad(), BigDecimal::add);
}
```

Los mapas tienen la misma clave, código de producto:

- `porCodigo` conserva el objeto encontrado.
- `cantidades` conserva el total solicitado para ese código.

Antes de sumar, valida cada línea. Esto evita que `10` y `-2` se conviertan en una solicitud aparentemente válida de `8`: la línea negativa debe rechazarse.

`merge()` añade la cantidad si el código no estaba, o suma si ya estaba. `BigDecimal::add` es una referencia al método utilizado para esa suma.

Ejemplo:

```text
Items:       P-001 → 6, P-001 → 6
Cantidades:  P-001 → 12
```

Con solo 10 kg disponibles, FEFO debe rechazar 12. No se evalúan dos solicitudes independientes de 6 utilizando el mismo stock.

### 8.4. Planificar todos los detalles

```java
List<DetalleVenta> detalles = new ArrayList<>();
for (Map.Entry<String, BigDecimal> entrada : cantidades.entrySet()) {
    Producto producto = porCodigo.get(entrada.getKey());
    List<AsignacionLote> asignaciones = inventario.planificar(
            producto, entrada.getValue(), instante.toLocalDate());
    detalles.add(new DetalleVenta(producto, entrada.getValue(), asignaciones));
}
```

`entrada.getKey()` es el código y `entrada.getValue()` la cantidad total. El servicio obtiene el objeto producto, pide el reparto y crea el detalle con precio y origen.

Este bucle prepara objetos. No descuenta mercancía. Si falta stock para el último producto, los anteriores tampoco han sido descontados.

### 8.5. Registrar y devolver el resultado

```java
Venta venta = new Venta(numero, instante.toLocalDate(), cliente, detalles);
venta.registrar(inventario, instante);
repositorio.guardar(venta);
return venta;
```

Con todos los detalles listos, construye el borrador, aplica la operación y guarda la venta. El inventario vuelve a validar todo antes de modificar las cantidades.

La aplicación actual funciona en memoria para un usuario de consola. El esquema previene efectos parciales en los rechazos esperados de negocio; si después se incorpora persistencia o concurrencia, habrá que adaptar el registro de varias colecciones a una transacción de esa infraestructura.

### 8.6. `anularVenta(numero)`

```java
Venta venta = consultar(numero);
venta.anular(inventario, LocalDateTime.now(reloj));
repositorio.actualizar(venta);
return venta;
```

Busca la venta, solicita su anulación, actualiza el registro y devuelve el objeto. No borra la venta. Si no existe, `consultar()` lanza `RegistroNoEncontradoException`. Si estaba anulada, `Venta` lanza `VentaYaAnuladaException` antes de modificar inventario.

### 8.7. `consultar`, `listar` y `validarNumero`

`consultar(numero)` valida el texto y utiliza `buscarPorId(...).orElseThrow(...)`. Devuelve la venta conservada con su estado actual, incluso si está anulada. El flujo de registro de este servicio solo guarda una venta después de registrarla correctamente; no guarda borradores.

`listar()` devuelve las ventas conservadas, incluyendo anuladas. `validarNumero()` es una comprobación privada que exige texto y devuelve `strip()`, siguiendo las validaciones locales adoptadas para el proyecto.

## 9. Ejemplo de 8 kg y su anulación

Con cliente `1001`, producto `P-001`, 5 kg en `L-001` y 20 kg en `L-002`:

```java
Venta venta = ventas.registrarVenta(new DatosVenta(
        "V-001", "1001",
        List.of(new ItemVenta("P-001", new BigDecimal("8")))));

BigDecimal total = venta.calcularTotal();
ventas.anularVenta("V-001");
```

Si el precio es `24000.00` por kg, el total es `192000.00`. La venta asigna 5 y 3 kg. Después de registrarla, quedan 0 y 17; después de anular, quedan 5 y 20.

Cambiar posteriormente el precio a `25000.00` no altera el total histórico de `192000.00`.

| Intento inválido | Quién lo detecta | Efecto |
|---|---|---|
| Venta vacía | Servicio | No se registra. |
| `3.5` unidades | Producto | No se prepara el detalle. |
| Dos líneas que suman más que el stock | FEFO | No se descuenta. |
| Número de venta repetido | Servicio | No duplica salidas. |
| Asignaciones de otro producto o suma incorrecta | Detalle | No se construye. |
| Registrar otra vez la misma entidad | Venta | Conserva el registro anterior. |
| Anular una venta inexistente | Consulta del servicio | No hay devoluciones. |
| Anular por segunda vez | Venta | No repone ni añade movimientos. |

## 10. Orden de lectura y verificación

Lee `ItemVenta` → `DatosVenta` → `DetalleVenta` → `EstadoVenta` → `Venta` → `ServicioVentas`. Para la modificación real de cantidades, vuelve a `Inventario.registrarSalida()` y `Inventario.devolver()` en el bloque 4.

Ejecuta `mvn test` desde la carpeta de `pom.xml`. Las comprobaciones más útiles son stock insuficiente en el último producto, productos repetidos, precio histórico, anulación exacta, segunda anulación y devolución después del vencimiento.

Continúa con [Bloque 6: trazabilidad y consultas](BLOQUE_06_TRAZABILIDAD_Y_CONSULTAS.md), donde utilizamos las relaciones conservadas por la compra y la venta para reconstruir el recorrido de la mercancía.
