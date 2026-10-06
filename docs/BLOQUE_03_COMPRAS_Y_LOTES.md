# Bloque 3: compras, lotes y fechas, explicado archivo por archivo

Este bloque permite recibir mercancía, conservar quién la entregó y registrar de qué producto es cada lote. Primero se construye y valida toda la compra; después se incorporan los lotes al inventario. Una compra con datos inválidos debe rechazarse antes de registrar sus entradas.

La guía explica el código implementado. Para seguirla, abre el archivo enlazado y lee el fragmento antes de continuar con su explicación. Las clases anteriores `Producto`, `Abastecedor`, sus servicios y `Repositorio` son las dependencias de este bloque.

## 1. De dónde sale este bloque

El plan relaciona compras con RF-04, HU-04, RN-01, RN-03 a RN-06 y RN-10. En términos concretos necesitamos:

- Identificar cada compra y cada lote sin repetir sus códigos.
- Saber qué abastecedor entregó la mercancía.
- Conservar producto, cantidad recibida y fechas de cada lote.
- Exigir fecha de sacrificio cuando el origen sea un matadero.
- Validar cantidades mediante el producto, aprovechando sus variantes por peso y por unidad.
- Registrar una entrada de inventario por cada lote recibido.
- Conservar esas relaciones para consultar posteriormente la trazabilidad.

Las decisiones adicionales adoptadas para completar los casos que el plan dejó pendientes son:

| Tema | Decisión implementada |
|---|---|
| Fecha de recepción | La fecha actual del reloj entregado al servicio. No se pide al usuario. |
| Sacrificio y procesamiento | El sacrificio puede coincidir con el procesamiento, pero no ser posterior. |
| Procesamiento y recepción | El procesamiento puede coincidir con la recepción, pero no ser posterior. |
| Procesamiento y vencimiento | El procesamiento debe ser anterior al vencimiento. |
| Recepción y vencimiento | El vencimiento debe ser posterior a la recepción. |
| Día de vencimiento | El lote se considera vencido desde ese día. |
| Proveedor que no es matadero | Puede omitir sacrificio. Si lo proporciona, debe ser coherente con procesamiento. |
| Cambios posteriores | El origen, el producto y las fechas del lote no tienen setters. |

Estas decisiones completan la implementación; no son afirmaciones de que todas estuvieran explícitas en el documento original.

## 2. Recorrido de una compra

```text
Consola crea DatosCompra
       ↓
ServicioCompras busca abastecedor y productos activos
       ↓
Compra crea y reúne lotes
       ↓
Lote valida cantidades, origen y fechas
       ↓
Inventario comprueba la compra completa y registra entradas
       ↓
Repositorio conserva la compra registrada
```

Los DTO llevan datos introducidos por el usuario. Las entidades representan objetos reales del negocio. El servicio transforma los primeros en las segundas buscando las referencias correctas.

## 3. Archivo `DatosLoteRecibido.java`

[Abrir DatosLoteRecibido.java](../src/main/java/co/trazacarne/servicio/dto/DatosLoteRecibido.java).

```java
package co.trazacarne.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosLoteRecibido(String codigoLote, String codigoProducto, BigDecimal cantidad,
                               LocalDate fechaSacrificio, LocalDate fechaProcesamiento,
                               LocalDate fechaVencimiento) { }
```

`package` indica que es un DTO del servicio. Los `import` permiten usar `BigDecimal` para cantidades y `LocalDate` para fechas sin hora.

Un `record` es una forma de declarar un objeto cuyo propósito principal es agrupar datos. Java genera el constructor, métodos de lectura, `equals`, `hashCode` y una representación textual. Los componentes del record no se reasignan después de construirlo.

Por ejemplo:

```java
DatosLoteRecibido datos = new DatosLoteRecibido(
        "L-001", "P-001", new BigDecimal("5.000"),
        LocalDate.of(2026, 10, 4),
        LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 7));

String codigo = datos.codigoLote();
BigDecimal cantidad = datos.cantidad();
```

Un record utiliza `codigoLote()` y `cantidad()`, no `getCodigoLote()` y `getCantidad()`. Esos métodos se llaman accesores y devuelven el componente correspondiente.

Aquí todavía tenemos códigos de texto, no objetos `Producto` o `Compra`. El servicio buscará el producto a partir de `codigoProducto()` y creará la compra. Las fechas y cantidades se validan en `Lote`: crear este DTO no equivale a registrar mercancía.

## 4. Archivo `DatosCompra.java`

[Abrir DatosCompra.java](../src/main/java/co/trazacarne/servicio/dto/DatosCompra.java).

Agrupa el número de compra, NIT de origen y la lista de lotes solicitados:

```java
public record DatosCompra(String numero, String nitAbastecedor, List<DatosLoteRecibido> lotes) {
    public DatosCompra {
        if (lotes != null) {
            for (DatosLoteRecibido lote : lotes) {
                if (lote == null) {
                    throw new ReglaNegocioException("Los lotes recibidos no pueden ser nulos.");
                }
            }
            lotes = List.copyOf(lotes);
        }
    }
}
```

`List<DatosLoteRecibido>` significa una lista cuyos elementos son de ese tipo. Los signos `<...>` indican el tipo de sus elementos, no una comparación.

`public DatosCompra { ... }` es un **constructor compacto del record**. Permite revisar o ajustar los parámetros antes de que Java los asigne a los componentes.

El recorrido es:

1. Si la lista existe, recorrerla con `for`.
2. En cada vuelta, `lote` representa un elemento.
3. Si un elemento es `null`, lanzar una excepción con un mensaje de negocio.
4. Crear una copia no modificable con `List.copyOf(lotes)`.

La copia evita que otra parte haga `listaOriginal.clear()` y cambie después los datos de la compra. Tampoco se permite añadir a la lista obtenida mediante `datos.lotes()`.

La lista puede llegar como `null` o vacía a este constructor; el servicio comprueba esos casos y ofrece el mensaje «La compra debe contener al menos un lote». Aquí se protege principalmente la estructura de la lista. La regla de cada lote sigue en su entidad.

Ejemplo de creación:

```java
DatosCompra datosCompra = new DatosCompra(
        "C-001", "9001", List.of(datos));
```

`List.of(datos)` construye una lista con ese lote recibido. La compra puede contener varios: `List.of(loteUno, loteDos)`.

## 5. Archivo `Compra.java`

[Abrir Compra.java](../src/main/java/co/trazacarne/dominio/Compra.java).

### 5.1. Paquete, imports y atributos

Está en `co.trazacarne.dominio`, porque representa una compra del negocio. Importa excepciones para comunicar fallos, `BigDecimal` para cantidades, tipos de fecha y colecciones.

```java
private final String numero;
private final LocalDate fecha;
private final Abastecedor abastecedor;
private final List<Lote> lotes = new ArrayList<>();
private boolean registrada;
```

- `private` impide modificar esos atributos directamente desde otra clase.
- `final` impide reasignar el número, fecha, abastecedor y referencia de la lista.
- `ArrayList` permite añadir lotes mientras se prepara la compra.
- `final` en la lista no impide añadir elementos: impide sustituir la lista por otra.
- `registrada` distingue una compra preparada de una que ya produjo entradas. Como no se le asigna un valor al declarar el atributo, Java la inicia en `false`.

### 5.2. Constructor

```java
public Compra(String numero, LocalDate fecha, Abastecedor abastecedor) {
    if (numero == null || numero.isBlank()) {
        throw new ReglaNegocioException("El número de compra es obligatorio.");
    }
    if (fecha == null || abastecedor == null) {
        throw new ReglaNegocioException("La compra requiere fecha y abastecedor.");
    }
    if (!abastecedor.estaActivo()) {
        throw new RegistroInactivoException("El abastecedor está inactivo.");
    }
    this.numero = numero.strip();
    this.fecha = fecha;
    this.abastecedor = abastecedor;
}
```

Primero valida; luego asigna los atributos. `||` significa «o» y `!` significa «no». `strip()` elimina espacios externos del número. `this.numero` es el atributo; `numero` sin `this` es el parámetro.

Si el abastecedor está desactivado, el objeto no se construye. Esta validación local protege la entidad aunque alguien intente crearla directamente sin pasar por el servicio.

### 5.3. `agregarLote(...)`

```java
if (registrada) {
    throw new ReglaNegocioException("Una compra registrada no permite añadir lotes.");
}
Lote lote = new Lote(codigo, this, producto, cantidad,
        sacrificio, procesamiento, vencimiento);
```

No se permiten nuevos lotes después del registro. `this` se entrega al constructor del lote para que conserve la compra a la que pertenece.

Después comprueba duplicados dentro de esta compra:

```java
for (Lote existente : lotes) {
    if (existente.getCodigo().equals(lote.getCodigo())) {
        throw new IdentificadorDuplicadoException(
                "El lote " + lote.getCodigo() + " se repite en la compra.");
    }
}
lotes.add(lote);
return lote;
```

`existente` representa cada lote ya añadido. `equals()` compara el contenido de los códigos. Se utiliza para textos en lugar de `==`, que compara referencias.

El nuevo lote se añade únicamente después de comprobar los anteriores. `return lote` devuelve el objeto creado; no registra todavía una entrada de inventario.

Hay dos ámbitos de unicidad: `Compra` detecta duplicados dentro de su lista; `Inventario` detecta códigos utilizados en compras anteriores.

### 5.4. `registrar(...)`

```java
if (registrada) {
    throw new ReglaNegocioException("La compra " + numero + " ya fue registrada.");
}
if (lotes.isEmpty() || inventario == null) {
    throw new ReglaNegocioException("La compra requiere lotes y un inventario.");
}
inventario.registrarEntrada(this, instante);
registrada = true;
```

No se permite registrar dos veces porque eso duplicaría mercancía y movimientos. `isEmpty()` pregunta si la lista tiene cero elementos.

`instante` es una fecha con hora, necesaria para el historial. `fecha` de la compra contiene solo el día.

El inventario valida la entrada completa antes de aplicar cambios. Si lanza una excepción, Java no llega a `registrada = true`. Si termina correctamente, la compra queda marcada como registrada.

El método es `void`: ejecuta una operación y no devuelve un resultado.

### 5.5. Métodos de lectura

| Método | Qué devuelve |
|---|---|
| `getNumero()` | Número normalizado de compra. |
| `getFecha()` | Día de recepción. |
| `getAbastecedor()` | Referencia al origen de la compra. |
| `getLotes()` | Copia no modificable de la lista de lotes. |
| `estaRegistrada()` | `true` después de registrar las entradas. |

`List.copyOf(lotes)` protege la estructura de la lista, pero no convierte cada lote en una copia. Los lotes siguen siendo los mismos objetos que consulta el inventario; sus cantidades cambian mediante operaciones controladas.

## 6. Archivo `Lote.java`

[Abrir Lote.java](../src/main/java/co/trazacarne/dominio/Lote.java).

### 6.1. Atributos y acceso al constructor

```java
private final String codigo;
private final Compra compra;
private final Producto producto;
private final BigDecimal cantidadRecibida;
private BigDecimal cantidadDisponible;
private final LocalDate fechaSacrificio;
private final LocalDate fechaProcesamiento;
private final LocalDate fechaVencimiento;
```

`cantidadRecibida` conserva la cantidad original. `cantidadDisponible` cambia con ventas, devoluciones y pérdidas. El origen y las fechas son `final` porque una venta posterior debe poder reconstruir los hechos originales.

El constructor empieza con `Lote(...)`, sin `public`. Eso es **acceso de paquete**: se puede llamar desde `co.trazacarne.dominio`, pero no directamente desde consola o servicios. El camino utilizado para construirlo es `compra.agregarLote(...)`.

### 6.2. Validaciones iniciales

```java
if (codigo == null || codigo.isBlank() || compra == null || producto == null) {
    throw new ReglaNegocioException("El lote requiere código, compra y producto.");
}
if (!producto.estaActivo()) {
    throw new RegistroInactivoException("El producto está inactivo.");
}
producto.validarCantidad(cantidad);
```

Se exige identidad, compra, producto activo y cantidad válida. La última línea utiliza polimorfismo: un producto por peso comprueba sus decimales; uno por unidad rechaza fracciones. `Lote` no necesita repetir esas reglas.

### 6.3. Fechas obligatorias y origen

```java
if (procesamiento == null || vencimiento == null) {
    throw new FechasInvalidasException("Procesamiento y vencimiento son obligatorios.");
}
if (compra.getAbastecedor().esMatadero() && sacrificio == null) {
    throw new FechasInvalidasException("Un lote de matadero requiere fecha de sacrificio.");
}
```

`&&` significa «y»: se exige sacrificio cuando se cumplen ambas condiciones, origen matadero y fecha ausente.

### 6.4. Orden de las fechas

```java
if (sacrificio != null && sacrificio.isAfter(procesamiento)) {
    throw new FechasInvalidasException("El sacrificio no puede ser posterior al procesamiento.");
}
if (!procesamiento.isBefore(vencimiento) || procesamiento.isAfter(compra.getFecha())) {
    throw new FechasInvalidasException(
            "El procesamiento debe preceder al vencimiento y no superar la recepción.");
}
if (!vencimiento.isAfter(compra.getFecha())) {
    throw new FechasInvalidasException("No se puede recibir un lote vencido.");
}
```

`isAfter()` significa «es posterior» e `isBefore()` significa «es anterior». Anteponer `!` invierte la respuesta.

Con recepción el 6 de octubre, una combinación válida sería sacrificio el 4, procesamiento el 5 y vencimiento el 7. Procesamiento el 7 se rechaza porque supera la recepción. Vencimiento el 6 también se rechaza: el lote ya está vencido ese día.

Solo después de validar se asignan los atributos. Disponible comienza con la misma cantidad que recibida.

### 6.5. `descontar(cantidad)`

```java
producto.validarCantidad(cantidad);
if (cantidad.compareTo(cantidadDisponible) > 0) {
    throw new StockInsuficienteException(
            "El lote " + codigo + " no tiene cantidad suficiente.");
}
cantidadDisponible = cantidadDisponible.subtract(cantidad);
```

El método tampoco es `public`: el inventario, situado en el mismo paquete, lo utiliza y registra el movimiento correspondiente.

`compareTo()` devuelve un número positivo cuando el primer valor es mayor, cero cuando son iguales y negativo cuando es menor. Se utiliza para comparar numéricamente `BigDecimal` sin confundir escalas como `5.0` y `5.00`.

`subtract()` devuelve el resultado de una resta. `BigDecimal` es inmutable: no cambia por dentro, por eso reasignamos el resultado a `cantidadDisponible`.

### 6.6. `reponer(cantidad)`

```java
producto.validarCantidad(cantidad);
BigDecimal resultado = cantidadDisponible.add(cantidad);
if (resultado.compareTo(cantidadRecibida) > 0) {
    throw new ReglaNegocioException(
            "La reposición supera lo recibido en el lote " + codigo + ".");
}
cantidadDisponible = resultado;
```

Calcula primero el resultado y valida que no supere la cantidad original. Solo después cambia el disponible. Por ejemplo, después de vender 3 de 5 kg quedan 2; reponer 3 devuelve el lote a 5.

### 6.7. Vencimiento y disponibilidad para venta

```java
public boolean estaVencido(LocalDate hoy) {
    if (hoy == null) {
        throw new FechasInvalidasException("La fecha de consulta es obligatoria.");
    }
    return !fechaVencimiento.isAfter(hoy);
}
```

Devuelve `true` si el vencimiento es igual o anterior a `hoy`. Un lote que vence el 7 se puede utilizar el 6, pero ya está vencido el 7.

```java
public boolean puedeVenderse(LocalDate hoy) {
    return !estaVencido(hoy) && !compra.getFecha().isAfter(hoy)
            && producto.estaActivo() && cantidadDisponible.signum() > 0;
}
```

Para venderlo debe estar vigente, ya recibido, asociado a un producto activo y tener cantidad positiva. `signum()` devuelve `-1`, `0` o `1` según el signo de la cantidad.

El abastecedor puede ser desactivado después de entregar mercancía: eso conserva el origen histórico y no convierte automáticamente el lote en inválido para vender.

### 6.8. Métodos de lectura

Los getters devuelven código, compra, producto, cantidades y fechas. `getAbastecedor()` hace una pequeña navegación:

```java
public Abastecedor getAbastecedor() {
    return compra.getAbastecedor();
}
```

No almacena una segunda copia del abastecedor: lo obtiene de la compra, que es la fuente de esa relación.

## 7. Archivo `FechasInvalidasException.java`

[Abrir FechasInvalidasException.java](../src/main/java/co/trazacarne/excepcion/FechasInvalidasException.java).

```java
public class FechasInvalidasException extends ReglaNegocioException {
    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
```

Identifica los errores de fechas y conserva el mensaje recibido mediante la clase padre. No compara fechas por sí misma: `Lote` detecta el incumplimiento y lanza este tipo de aviso. La consola puede capturarlo como una `ReglaNegocioException` y mostrar su mensaje.

## 8. Archivo `ServicioCompras.java`

[Abrir ServicioCompras.java](../src/main/java/co/trazacarne/servicio/ServicioCompras.java).

### 8.1. Dependencias y constructor

```java
private final Repositorio<Compra, String> repositorio;
private final ServicioAbastecedores abastecedores;
private final ServicioProductos productos;
private final Inventario inventario;
private final Clock reloj;
```

El repositorio guarda compras por número. Los servicios buscan los participantes. El inventario registra lotes y movimientos. El reloj proporciona la fecha de operación.

El constructor recibe estos objetos y los comprueba con `Objects.requireNonNull(...)`. No crea repositorios nuevos por dentro: `Main` entrega las mismas dependencias que utilizan los demás servicios.

`Clock` evita que cada entidad consulte directamente el reloj del computador. En una prueba podemos entregar un reloj fijo y repetir el mismo escenario cualquier día.

### 8.2. `registrarCompra(datos)` paso a paso

```java
if (datos == null || datos.lotes() == null || datos.lotes().isEmpty()) {
    throw new ReglaNegocioException("La compra debe contener al menos un lote.");
}
String numero = validarNumero(datos.numero());
if (repositorio.existe(numero)) {
    throw new IdentificadorDuplicadoException("Ya existe la compra " + numero + ".");
}
```

Comprueba contenido, normaliza número y detecta si ese número ya se utiliza. `||` deja de evaluar cuando encuentra una condición verdadera, por eso no intenta llamar a `lotes()` sobre `datos == null`.

```java
Abastecedor origen = abastecedores.consultarActivo(datos.nitAbastecedor());
LocalDateTime instante = LocalDateTime.now(reloj);
Compra compra = new Compra(numero, instante.toLocalDate(), origen);
```

Busca un origen existente y activo. Captura un único instante para la operación; de él obtiene el día de recepción. Esto evita que fecha y hora se obtengan de lecturas diferentes.

```java
for (DatosLoteRecibido recibido : datos.lotes()) {
    Producto producto = productos.consultarActivo(recibido.codigoProducto());
    compra.agregarLote(recibido.codigoLote(), producto, recibido.cantidad(),
            recibido.fechaSacrificio(), recibido.fechaProcesamiento(),
            recibido.fechaVencimiento());
}
```

`recibido` representa cada DTO. El servicio busca su producto y entrega los datos a la compra, que crea el lote. Si falla el último lote, los anteriores solo están en esa compra preparada: todavía no produjeron entradas de inventario.

```java
compra.registrar(inventario, instante);
repositorio.guardar(compra);
return compra;
```

Una vez preparados todos, se registra la entrada completa, se conserva la compra y se devuelve. La protección frente a cambios parciales cubre los rechazos de negocio de esta aplicación en memoria y de un solo usuario; no constituye una transacción de base de datos ni gestión de concurrencia.

### 8.3. `consultar`, `listar` y `validarNumero`

`consultar(numero)` normaliza el número y utiliza `buscarPorId(...).orElseThrow(...)`. Si hay compra, la devuelve. Si no hay, la lambda `() ->` crea una `RegistroNoEncontradoException`.

`listar()` devuelve las compras del repositorio. Las compras no se borran para conservar sus lotes y origen.

`validarNumero()` es un método privado del servicio: rechaza un número nulo o en blanco y devuelve `numero.strip()`. Sigue la decisión del proyecto de mantener validaciones locales; no existe una clase global de validaciones.

## 9. Ejemplo completo y errores que puedes seguir

Suponiendo que ya existen el abastecedor `9001`, el producto `P-001` y el servicio de compras, con reloj fijado al 6 de octubre de 2026:

```java
DatosLoteRecibido primero = new DatosLoteRecibido(
        "L-001", "P-001", new BigDecimal("5"),
        LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 7));
DatosLoteRecibido segundo = new DatosLoteRecibido(
        "L-002", "P-001", new BigDecimal("20"),
        LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 15));

Compra compra = compras.registrarCompra(
        new DatosCompra("C-001", "9001", List.of(primero, segundo)));
```

El resultado es una compra registrada con dos lotes. El inventario tendrá 5 y 20 kg, y un movimiento `ENTRADA` por lote. El precio del producto no interviene en el registro de cantidad comprada: esta primera versión no desarrolla contabilidad de costes de compra.

| Cambio en el ejemplo | Resultado esperado |
|---|---|
| Repetir `C-001` | `IdentificadorDuplicadoException`; no duplica entradas. |
| Repetir `L-001` dentro de la compra | `IdentificadorDuplicadoException` durante la preparación. |
| Usar un código de lote de una compra anterior | El inventario rechaza toda la entrada. |
| Sacrificio ausente con matadero | `FechasInvalidasException`. |
| Vencimiento el día de recepción | `FechasInvalidasException`. |
| Cantidad `3.5` en un producto por unidad | `CantidadInvalidaException` desde el producto. |
| Abastecedor o producto inexistente/inactivo | El servicio de gestión rechaza la búsqueda. |

## 10. Qué estudiar primero y cómo comprobarlo

Lee en este orden: `DatosLoteRecibido` → `DatosCompra` → `Lote` → `Compra` → `ServicioCompras`. Después continúa con [Bloque 4: inventario y FEFO](BLOQUE_04_INVENTARIO_Y_FEFO.md), que explica dónde se aplican las entradas y cómo quedan registradas.

Desde la carpeta de `pom.xml`, ejecuta:

```powershell
mvn test
```

Las comprobaciones relevantes son recepción válida, fechas incoherentes, lotes duplicados, cantidades incompatibles y rechazos que conservan el inventario. Para entender un fallo, sigue la primera condición que se cumple y la excepción lanzada; las líneas posteriores de esa operación ya no se ejecutan.
