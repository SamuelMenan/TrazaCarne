# Bloque 7: consola, arranque y pruebas de uso

Esta guía explica los archivos que permiten utilizar TrazaCarne desde el teclado. Léela después de las guías de compras, inventario, ventas y trazabilidad: el menú llama a esas operaciones, pero no implementa sus reglas.

## 1. De dónde sale este código

Las historias de usuario describen cosas que el vendedor necesita hacer: registrar participantes, recibir mercancía, vender, anular y consultar el recorrido de un lote. Los servicios ya ofrecen esas operaciones. Faltaba una forma de elegirlas, introducir sus datos y ver el resultado.

Por eso construimos estos archivos:

| Archivo | Responsabilidad |
|---|---|
| `src/main/java/co/trazacarne/consola/MenuConsola.java` | Leer el teclado, invocar servicios y mostrar respuestas. |
| `src/main/java/co/trazacarne/Main.java` | Crear los objetos, conectar sus dependencias y arrancar el programa. |
| `src/test/java/co/trazacarne/consola/MenuConsolaTest.java` | Comprobar escenarios introduciendo texto como si lo escribiera un usuario. |

La decisión de arquitectura es esta:

```text
Usuario escribe una opción y los datos
                ↓
MenuConsola convierte texto en datos Java
                ↓
Servicio ejecuta el caso de uso
                ↓
Dominio protege sus reglas y repositorios conservan los registros
                ↓
MenuConsola presenta el resultado o el mensaje de excepción
```

La consola comprueba si puede interpretar un número o una fecha. El negocio comprueba si ese número es una cantidad permitida, si hay stock o si las fechas cumplen las reglas. Ambas comprobaciones tienen propósitos diferentes.

## 2. Archivo `MenuConsola.java`: sus dependencias

```java
private final Scanner entrada;
private final PrintStream salida;
private final ServicioClientes clientes;
```

- `Scanner` lee texto. En la ejecución normal leerá de `System.in`, el teclado.
- `PrintStream` escribe texto. Normalmente será `System.out`, la consola.
- `ServicioClientes` permite ejecutar operaciones de clientes.
- `private` impide acceder directamente a esos atributos desde otra clase.
- `final` impide sustituir cada referencia después del constructor. No significa que los clientes o los registros del servicio sean inmutables.

Los demás atributos siguen el mismo patrón: `abastecedores`, `productos`, `compras`, `ventas`, `inventario` y `trazabilidad` son los servicios disponibles para los casos de uso.

El constructor los recibe:

```java
public MenuConsola(Scanner entrada, PrintStream salida, ServicioClientes clientes,
                   ServicioAbastecedores abastecedores, ServicioProductos productos,
                   ServicioCompras compras, ServicioVentas ventas,
                   ServicioInventario inventario, ServicioTrazabilidad trazabilidad)
```

Dentro asignamos cada argumento a su atributo:

```java
this.entrada = Objects.requireNonNull(entrada);
```

`this.entrada` es el atributo del objeto; `entrada` es lo recibido como parámetro. `requireNonNull()` comprueba que la dependencia exista. Si alguien conecta mal el programa y entrega `null`, el error aparece al construir el menú.

El menú no crea repositorios ni servicios. Eso se hace en `Main`. Además, como el lector y la salida también se reciben desde afuera, las pruebas pueden proporcionar un texto preparado y recoger las respuestas sin usar un teclado real.

## 3. `iniciar()`: mantener abierto el menú principal

```java
public void iniciar() {
    salida.println("TrazaCarne | Inventario y trazabilidad");
    salida.println("Los datos se conservan durante esta sesión en memoria.");
    try {
        boolean continuar = true;
        while (continuar) {
            // Se muestran las opciones.
            switch (leerTexto("Opción: ")) {
                case "1" -> menuClientes();
                case "2" -> menuAbastecedores();
                case "3" -> menuProductos();
                case "4" -> menuCompras();
                case "5" -> menuVentas();
                case "6" -> menuInventario();
                case "7" -> menuTrazabilidad();
                case "0" -> continuar = false;
                default -> salida.println("Opción desconocida. Inténtalo de nuevo.");
            }
        }
    } catch (FinEntradaException fin) {
        salida.println("\nEntrada finalizada.");
    }
    salida.println("Sesión finalizada.");
}
```

Este fragmento omite solo las dos líneas que imprimen las opciones; la lógica es la del archivo.

1. `println()` muestra un texto y pasa a la siguiente línea.
2. `continuar` empieza en `true`: inicialmente queremos aceptar operaciones.
3. `while (continuar)` repite el menú mientras esa variable sea verdadera.
4. `leerTexto()` devuelve lo escrito por el usuario.
5. `switch` selecciona el submenú correspondiente. Las opciones se comparan como texto: `"1"`, `"2"`, etc.
6. La opción `"0"` asigna `false` a `continuar`. En la próxima comprobación del `while`, el ciclo termina.
7. `default` atiende cualquier opción desconocida. Después volvemos a mostrar el menú.
8. El `catch` se utiliza si la entrada se agotó, por ejemplo cuando una prueba entrega un texto y se llega a su final.
9. Al salir, se imprime una despedida una sola vez.

Cada submenú tiene su propio ciclo. Por eso `0` dentro de clientes vuelve al menú principal; `0` en el menú principal cierra el programa.

## 4. `leerTexto()`: todas las entradas se leen como líneas

```java
private String leerTexto(String pregunta) {
    salida.print(pregunta);
    salida.flush();
    if (!entrada.hasNextLine()) {
        throw new FinEntradaException();
    }
    return entrada.nextLine().strip();
}
```

Línea por línea:

- `print(pregunta)` presenta la pregunta sin salto de línea, para escribir junto a ella.
- `flush()` solicita que el texto pendiente se muestre antes de esperar la respuesta.
- `hasNextLine()` indica si existe otra línea para leer.
- Si no existe, se lanza una señal interna de fin de entrada.
- `nextLine()` toma la línea completa.
- `strip()` elimina espacios externos, conservando los internos.

Usamos siempre `nextLine()`. Mezclar `nextInt()` con `nextLine()` suele causar confusión con el salto de línea pendiente; aquí convertimos los valores después de leer el texto.

Al final del archivo está esta clase pequeña:

```java
private static final class FinEntradaException extends RuntimeException {
}
```

Está dentro de `MenuConsola`, por eso no es un archivo nuevo ni una excepción del negocio. `private` limita su uso al menú. Solo comunica que ya no hay entrada. La captura exterior de `iniciar()` cierra todos los submenús sin seguir preguntando indefinidamente.

Si la entrada termina a mitad de una compra, aún no se ha llamado a `registrarCompra()` y no se registra una compra parcial.

## 5. `leerDecimal()`: convertir texto en `BigDecimal`

```java
private BigDecimal leerDecimal(String pregunta) {
    while (true) {
        try {
            return new BigDecimal(leerTexto(pregunta).replace(',', '.'));
        } catch (NumberFormatException error) {
            salida.println("Escribe un número válido, por ejemplo 1.250; sin separadores de miles.");
        }
    }
}
```

- `while (true)` repite la lectura hasta devolver un valor o terminar la entrada.
- `replace(',', '.')` permite escribir `1,250` o `1.250` como cantidad decimal.
- `new BigDecimal(...)` crea el número a partir del texto; no utiliza un `double` intermedio.
- Si se escribe `mucho`, la conversión lanza `NumberFormatException`.
- El `catch` muestra cómo escribir el dato y el ciclo vuelve a pedirlo.
- Cuando la conversión funciona, `return` devuelve el número y termina este método.

No deben introducirse separadores de miles. Escribe `24000.00` para un precio de veinticuatro mil. `24.000` se interpretaría como veinticuatro, no como veinticuatro mil.

Este método acepta un número negativo como número correctamente escrito. La entidad o el servicio decide si es una cantidad válida para la operación. Cuando el negocio la rechaza, se vuelve al submenú y se puede iniciar nuevamente la operación.

## 6. Las otras funciones de lectura

### `leerEnteroEnRango()`

```java
int valor = Integer.parseInt(leerTexto(pregunta));
if (valor >= minimo && valor <= maximo) {
    return valor;
}
```

`parseInt()` convierte texto a un entero. Si el texto no es entero, se captura `NumberFormatException`. Si el número existe pero está fuera del rango, tampoco se devuelve. En ambos casos se indica el rango y se vuelve a pedir el dato.

La forma de venta y el tipo de abastecedor utilizan el rango de 1 a 2. La consulta de próximos vencimientos admite de 0 a 36500 días para mantener el dato de consulta dentro de un rango manejable.

### `leerFecha()`

```java
String texto = leerTexto(pregunta);
if (opcional && texto.isEmpty()) {
    return null;
}
try {
    return LocalDate.parse(texto);
} catch (DateTimeParseException error) {
    // Se muestra el formato y se vuelve a preguntar.
}
```

- `opcional` indica si se permite dejar la respuesta vacía.
- `&&` significa que ambas condiciones deben cumplirse.
- Una fecha opcional vacía se representa mediante `null`.
- `LocalDate.parse()` utiliza el formato `AAAA-MM-DD` y rechaza fechas inexistentes como `2026-02-30`.
- Si falla el formato, se pide otra fecha.

La consola comprueba la existencia y formato de cada fecha. `Lote` comprueba el orden entre sacrificio, procesamiento y vencimiento.

### `leerSiNo()`

```java
String respuesta = leerTexto(pregunta);
if (respuesta.equalsIgnoreCase("s")) {
    return true;
}
if (respuesta.equalsIgnoreCase("n")) {
    return false;
}
```

`equalsIgnoreCase()` permite escribir `s` o `S`, `n` o `N`. Cualquier otra respuesta produce el mensaje `Responde s o n.` y una nueva pregunta.

## 7. `ejecutar()`: manejar un rechazo del negocio

```java
private void ejecutar(Runnable operacion) {
    try {
        operacion.run();
    } catch (ReglaNegocioException error) {
        salida.println("No se pudo completar la operación: " + error.getMessage());
    }
}
```

`Runnable` representa una acción que no recibe argumentos y no devuelve un resultado. `operacion.run()` ejecuta esa acción.

Por ejemplo, el submenú entrega una acción con esta forma:

```java
ejecutar(() -> {
    clientes.desactivar(leerTexto("Documento: "));
    salida.println("Cliente desactivado; su historial se conserva.");
});
```

- `() -> { ... }` es una lambda: una acción que se proporciona a otro método.
- `ejecutar()` la invoca dentro de su `try`.
- Si el cliente no existe, el servicio lanza una excepción de negocio.
- El mensaje de éxito no se imprime, porque la excepción interrumpe esa acción.
- Se captura el error, se muestra su mensaje y el submenú sigue abierto.

El `catch` es de `ReglaNegocioException`, la clase padre. También recibe sus variantes específicas. No se captura cualquier `Exception`: errores de programación o dependencias mal conectadas deben investigarse, no presentarse como si fueran un dato incorrecto del usuario.

## 8. Submenús de clientes y abastecedores

Los métodos `menuClientes()` y `menuAbastecedores()` utilizan esta secuencia:

```java
String opcion = leerTexto("Opción: ");
if (opcion.equals("0")) {
    continuar = false;
} else {
    ejecutar(() -> {
        // El switch llama al servicio elegido.
    });
}
```

La salida del submenú se decide antes de ejecutar una operación. Los rechazos de los servicios se manejan dentro de `ejecutar()`.

| Opción | Clientes | Abastecedores |
|---|---|---|
| 1 | Lee documento y nombre; llama a `clientes.registrar()`. | Lee NIT, nombre y tipo; llama a `abastecedores.registrar()`. |
| 2 | Llama a `consultar()` y muestra el cliente. | Llama a `consultar()` y muestra el abastecedor. |
| 3 | Llama a `listar()` y muestra todos. | Llama a `listar()` y muestra todos. |
| 4 | Lee identificador y nuevo nombre; llama a `actualizar()`. | Igual, utilizando el NIT. |
| 5 | Llama a `desactivar()`. | Llama a `desactivar()`. |

Estas consultas incluyen registros inactivos, porque son consultas históricas. Las operaciones que requieren registros activos lo comprueban en los servicios correspondientes.

En el registro de un abastecedor usamos:

```java
tipo == 1 ? TipoAbastecedor.MATADERO : TipoAbastecedor.PROVEEDOR
```

Es el operador ternario: si `tipo == 1`, produce `MATADERO`; en caso contrario, produce `PROVEEDOR`. La función de lectura ya garantizó que solo se eligiera 1 o 2.

## 9. `menuProductos()` y `registrarProducto()`

El submenú de productos tiene registro, consulta, listado, actualización de precio y desactivación. Los datos de registro son más numerosos, por eso la opción 1 delega en un método privado:

```java
String codigo = leerTexto("Código: ");
String nombre = leerTexto("Nombre: ");
String especie = leerTexto("Especie: ");
String corte = leerTexto("Tipo de corte: ");
int forma = leerEnteroEnRango("Forma (1. Por peso en kg, 2. Por unidad): ", 1, 2);
BigDecimal precio = leerDecimal("Precio por kg o unidad (sin separadores de miles): ");
productos.registrar(codigo, nombre, especie, corte,
        forma == 1 ? FormaVenta.POR_PESO : FormaVenta.POR_UNIDAD, precio);
```

Cada línea obtiene un dato. La llamada final los entrega al servicio; ese servicio crea la variante de producto correspondiente.

La consola no instancia `ProductoPorPeso` ni valida cuántos decimales admite su cantidad. Ese comportamiento ya pertenece al dominio.

En la opción 4 sucede lo mismo: la consola lee el nuevo precio y `Producto.actualizarPrecio()` comprueba su regla.

## 10. `menuCompras()` y `registrarCompra()`

El submenú permite registrar, consultar y listar compras. Para el registro primero se lee la cabecera:

```java
String numero = leerTexto("Número de compra: ");
String nit = leerTexto("NIT del abastecedor: ");
Abastecedor abastecedor = abastecedores.consultarActivo(nit);
List<DatosLoteRecibido> lotes = new ArrayList<>();
```

- La cabecera identifica la compra y su abastecedor.
- `consultarActivo()` permite conocer si es un matadero y rechaza un abastecedor inexistente o inactivo.
- `ArrayList` es una lista que permite añadir elementos mientras se llenan los datos.

Dentro del ciclo se leen los datos de cada lote y se construye su DTO:

```java
lotes.add(new DatosLoteRecibido(codigo, producto, cantidad,
        sacrificio, procesamiento, vencimiento));
agregar = leerSiNo("¿Agregar otro lote? (s/n): ");
```

`add()` añade el DTO a la lista. Todavía no añade un lote al inventario. Al responder `s`, el ciclo recoge otro lote. Al responder `n`, termina.

La fecha de sacrificio solo puede quedar vacía en un proveedor que no sea matadero. La expresión `!abastecedor.esMatadero()` produce `true` cuando se permite omitirla. Este ajuste de la pregunta no reemplaza la validación del dominio.

Después de terminar la captura se ejecuta una sola operación:

```java
Compra compra = compras.registrarCompra(new DatosCompra(numero, nit, lotes));
salida.println("Compra registrada: " + compra.getNumero());
mostrarCompra(compra);
```

El DTO de compra transporta la cabecera y todos los lotes. El servicio valida la compra completa y registra las entradas. El mensaje de éxito se imprime después de que esa operación termina.

## 11. `menuVentas()` y `registrarVenta()`

La venta también se captura primero y se ejecuta después:

```java
String numero = leerTexto("Número de venta: ");
String documento = leerTexto("Documento del cliente: ");
List<ItemVenta> items = new ArrayList<>();
```

Se añade cada solicitud:

```java
items.add(new ItemVenta(leerTexto("Código del producto: "),
        leerDecimal("Cantidad (kg o unidades): ")));
```

Un `ItemVenta` contiene el código y la cantidad pedida. No contiene asignaciones de lotes: esas las prepara el inventario mediante FEFO.

Cuando se han recogido todos los productos:

```java
Venta venta = ventas.registrarVenta(new DatosVenta(numero, documento, items));
salida.println("Venta registrada: " + venta.getNumero());
mostrarVenta(venta);
```

El servicio comprueba el cliente, los productos, las cantidades y el stock total; el dominio y el inventario registran la venta. La consola recibe la venta final.

La opción 4 usa `ventas.anularVenta(numero)`. La devolución a los lotes originales y la comprobación de segunda anulación ya están en la operación de negocio. El menú solamente recibe el número y muestra el resultado.

## 12. `menuInventario()`: consultas y pérdidas

| Opción | Método llamado | Resultado mostrado |
|---|---|---|
| 1 | `disponible(codigo)` y `existencias(codigo)` | Total vendible y lotes del producto. |
| 2 | `lotes()` | Todos los lotes registrados. |
| 3 | `lote(codigo)` | Un lote y sus datos. |
| 4 | `proximosAVencer(dias)` | Lotes incluidos en el intervalo consultado. |
| 5 | `registrarPerdida(lote, cantidad, motivo)` | Confirmación de la pérdida registrada. |
| 6 | `movimientos(lote)` | Fecha, tipo, cantidad, referencia y motivo de cada movimiento. |

Una cantidad restante de un lote no equivale necesariamente a cantidad vendible: si está vencido, el servicio no la incluye en `disponible()`. Por eso la primera opción presenta el total calculado por el servicio.

En la pérdida se lee una cantidad y un motivo. El servicio y el lote impiden cantidades inválidas o superiores a las existencias y conservan el movimiento. La consola no resta directamente ninguna cantidad.

## 13. `menuTrazabilidad()`: recorrer las relaciones

La primera opción recibe un número de venta:

```java
trazabilidad.origenDeVenta(leerTexto("Número de venta: "))
```

Cada resultado contiene producto, lote, cantidad, abastecedor y fechas de origen. El menú lee los campos de ese resultado mediante accesores de un `record`, como `origen.codigoLote()`.

La segunda opción recibe un lote y pregunta si se deben incluir las ventas anuladas:

```java
String lote = leerTexto("Código del lote: ");
boolean incluir = leerSiNo("¿Incluir ventas anuladas? (s/n): ");
trazabilidad.ventasDelLote(lote, incluir)
```

Así se puede consultar tanto el destino vigente como el historial completo. La consola imprime el estado de cada venta para que una venta anulada no se confunda con una salida vigente.

## 14. Los métodos de presentación

`mostrarCliente()`, `mostrarAbastecedor()` y `mostrarProducto()` leen los getters y forman una línea. No cambian los objetos.

`mostrarCompra()` imprime número, fecha y abastecedor; después muestra sus lotes.

`mostrarLote()` imprime código, producto, cantidad recibida, cantidad restante y fechas. `toPlainString()` convierte `BigDecimal` a una representación decimal sin notación científica.

`mostrarVenta()` imprime número, fecha, cliente, estado y total. Después consulta el origen para mostrar cuánto salió de cada lote. Eso también funciona para una venta anulada, conservando el estado visible.

Para no repetir el mismo recorrido de listas utilizamos:

```java
private <T> void mostrarLista(List<T> registros, Consumer<T> mostrar) {
    if (registros.isEmpty()) {
        salida.println("No hay registros.");
    }
    for (T registro : registros) {
        mostrar.accept(registro);
    }
}
```

- `<T>` declara un tipo genérico: este método sirve para listas de clientes, lotes o resultados de trazabilidad.
- `List<T>` es la lista del tipo seleccionado en cada llamada.
- `Consumer<T>` es una acción que recibe un objeto y no devuelve un resultado.
- `isEmpty()` comprueba si no hay elementos.
- `for (T registro : registros)` recorre uno por uno.
- `accept(registro)` ejecuta la acción de presentación sobre el elemento actual.

Esta llamada:

```java
mostrarLista(clientes.listar(), this::mostrarCliente);
```

significa: obtiene los clientes y, por cada uno, llama al método `mostrarCliente()` de este menú. `this::mostrarCliente` es una referencia al método; su alternativa sería `cliente -> mostrarCliente(cliente)`.

## 15. Archivo `Main.java`: preparar la fecha

```java
boolean demostracion = args.length == 1 && args[0].equals("--demo");
ZoneId zona = ZoneId.of("America/Bogota");
Clock reloj = demostracion
        ? Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), zona)
        : Clock.system(zona);
```

- `args` son los argumentos recibidos al ejecutar el programa.
- `args.length == 1` comprueba que se entregó un único argumento.
- `&&` evita acceder a `args[0]` si no existe ese argumento.
- Si se escribió `--demo`, se utiliza un reloj fijo para que la demostración siga funcionando en el futuro con sus fechas de ejemplo.
- En el uso normal se utiliza la fecha y hora actuales de Bogotá.
- `Clock` se entrega a los servicios para obtener una misma fuente de tiempo. Ninguna prueba necesita cambiar el reloj del computador.

El reloj fijo apunta al 5 de octubre de 2026. No pretende ser la fecha real del usuario cuando se ejecuta la demostración.

## 16. `Main`: crear repositorios y servicios

```java
Repositorio<Cliente, String> repositorioClientes =
        new RepositorioEnMemoria<>(Cliente::getDocumento);
```

La variable utiliza la interfaz `Repositorio`; la implementación concreta es `RepositorioEnMemoria`. `Cliente::getDocumento` le indica cómo obtener la clave de cada cliente. Se crean otros repositorios para NIT, código de producto, número de compra y número de venta.

Después se conectan los objetos:

```java
ServicioClientes clientes = new ServicioClientes(repositorioClientes);
ServicioAbastecedores abastecedores = new ServicioAbastecedores(repositorioAbastecedores);
ServicioProductos productos = new ServicioProductos(repositorioProductos);
Inventario inventario = new Inventario(new EstrategiaFEFO());
ServicioCompras compras = new ServicioCompras(
        repositorioCompras, abastecedores, productos, inventario, reloj);
ServicioVentas ventas = new ServicioVentas(
        repositorioVentas, clientes, productos, inventario, reloj);
```

El orden importa porque un objeto debe existir antes de entregarlo al constructor de otro. Primero creamos catálogos e inventario; después las operaciones que los necesitan.

Compras, ventas, consultas y trazabilidad comparten el mismo `inventario`. Si cada servicio recibiera uno diferente, una compra no aumentaría las existencias que consulta una venta.

El repositorio de ventas también se comparte entre `ServicioVentas` y `ServicioTrazabilidad`. Por eso la trazabilidad puede recorrer lo que registró el servicio de ventas.

Este lugar de ensamblaje demuestra inyección de dependencias sin Spring Boot. El método `main()` prepara las piezas; las reglas se ejecutan en sus clases.

## 17. Arranque normal y `--demo`

```java
MenuConsola menu = new MenuConsola(new Scanner(System.in), System.out, clientes,
        abastecedores, productos, compras, ventas, consultasInventario, trazabilidad);
menu.iniciar();
```

En el arranque normal se conectan el teclado y la pantalla con el menú. El programa comienza sin registros de ejemplo: tú registras los datos.

Con `--demo`, `ejecutarDemostracion()` hace lo siguiente:

1. Registra una cliente, un matadero y un producto por peso.
2. Compra dos lotes de 5 y 20 kg, con distintos vencimientos.
3. Registra una venta de 8 kg.
4. Consulta su origen: FEFO asigna 5 kg del primero y 3 kg del segundo.
5. Muestra el total de `192000.00 COP` y las existencias de 17 kg.
6. Anula la venta.
7. Muestra que los lotes vuelven a 5 y 20 kg, con 25 kg vendibles.

El método utiliza los mismos servicios que el menú. No cambia el inventario mediante setters para simular una operación.

En el bucle de la demostración aparece `var origen`. `var` permite que Java deduzca el tipo a partir de la lista recorrida. El tipo sigue siendo concreto y se comprueba al compilar; no es una variable sin tipo.

## 18. Archivo `MenuConsolaTest.java`: probar sin teclado

La prueba crea los mismos tipos de objetos que `Main`, pero con un reloj fijo. Las dependencias son atributos del objeto de prueba para consultar los resultados después de usar el menú.

Este es el método auxiliar que ejecuta una sesión:

```java
private String ejecutar(String entradas) {
    ByteArrayOutputStream resultado = new ByteArrayOutputStream();
    MenuConsola menu = new MenuConsola(new Scanner(entradas),
            new PrintStream(resultado, true, StandardCharsets.UTF_8), clientes, abastecedores,
            productos, compras, ventas, consultaInventario, trazabilidad);

    assertDoesNotThrow(menu::iniciar);

    return resultado.toString(StandardCharsets.UTF_8);
}
```

1. `ByteArrayOutputStream` recoge lo impreso en memoria.
2. `new Scanner(entradas)` lee una cadena en lugar del teclado.
3. `PrintStream` dirige la salida a ese recipiente y utiliza UTF-8 para conservar tildes.
4. Se conecta el menú con los servicios de la prueba.
5. `assertDoesNotThrow()` comprueba que la sesión termina sin una excepción sin manejar.
6. Se devuelve todo el texto impreso para comprobarlo.

Las entradas grandes utilizan un bloque de texto Java, delimitado por tres comillas. Cada línea representa una respuesta: una opción, un código, una cantidad o una fecha. No se necesita automatizar IntelliJ ni mover el ratón.

### Prueba `flujoCompletoPorConsolaConservaTrazabilidadYMovimientos()`

Registra catálogos desde el menú, recibe dos lotes, vende 8 kg, consulta ambas direcciones de trazabilidad, anula, registra una pérdida y consulta movimientos.

Después comprueba el estado de la venta, las cantidades finales, el cliente relacionado y los mensajes visibles. Comprueba que la venta se repartió en 5 y 3 kg y que no apareció un rechazo.

### Prueba `formatosInvalidosSePidenDeNuevoYFinDeEntradaCierraLaSesion()`

Escribe texto donde se espera una opción, una opción fuera del rango, un precio mal escrito, una cantidad mal escrita, una fecha inexistente y una respuesta diferente de `s` o `n`. Después proporciona respuestas válidas.

La prueba comprueba que los mensajes de ayuda aparecen y la compra se registra una sola vez. El final del texto también comprueba el cierre por fin de entrada.

### Prueba `entradaAgotadaMientrasSeLlenaUnaCompraNoRegistraDatosParciales()`

Los catálogos se preparan antes de entrar al menú. La entrada termina durante la captura de fechas de un lote. Como no se completó el DTO ni se llamó al servicio, no queda ninguna compra ni lote registrado.

### Prueba `rechazoDeNegocioPermiteOtraOperacionSinConsumirInventario()`

Con 10 kg disponibles, intenta vender 12 kg. El menú muestra el rechazo. Después registra otra venta de 8 kg. Solo existe la segunda venta y quedan 2 kg.

Esta prueba verifica que el menú continúa funcionando después de una excepción de negocio y que la operación rechazada no consume cantidades.

### Prueba `opcionesDeActualizarConsultarListarYDesactivarConservanLosCatalogos()`

Recorre los tres submenús de catálogos, modifica los nombres de cliente y abastecedor, cambia el precio del producto, consulta y lista los registros y finalmente los desactiva.

Después comprueba que se conservaron los registros con sus nuevos datos y quedaron inactivos. Así verificamos que las opciones de edición y desactivación están conectadas al servicio y que una consulta histórica sigue funcionando.

### Por qué usamos `compareTo()` en algunas comprobaciones

```java
assertEquals(0, new BigDecimal("5").compareTo(cantidadDisponible));
```

`BigDecimal.equals()` también considera la escala: `5` y `5.000` pueden no ser iguales según ese método. `compareTo()` compara su valor numérico: devuelve 0 cuando representan la misma cantidad. Para existencias interesa el valor, no cómo se escribió.

## 19. Cómo ejecutar

En IntelliJ ejecuta el botón verde de `Main.main()`. Para la demostración agrega `--demo` en los argumentos de la configuración de ejecución.

Desde una terminal situada en la carpeta que contiene `pom.xml`:

```powershell
mvn test
mvn package
java -jar target/trazacarne-1.0-SNAPSHOT.jar
```

La demostración se ejecuta así:

```powershell
java -jar target/trazacarne-1.0-SNAPSHOT.jar --demo
```

Puedes ejecutar solo las pruebas de consola con:

```powershell
mvn "-Dtest=MenuConsolaTest" test
```

El proyecto necesita el JDK indicado en `pom.xml`. Al cerrar la sesión se pierden los registros en memoria. Guardar datos entre ejecuciones requeriría una implementación persistente de los repositorios y del almacenamiento del inventario.

## 20. Cómo estudiar este bloque

Primero sigue una operación corta: opción Clientes, Desactivar. Busca su `case`, sigue la llamada al servicio y comprueba dónde se lanza la excepción si el documento no existe. Después vuelve a `ejecutar()` para ver cómo se presenta.

Luego sigue una venta: observa cómo se llena `List<ItemVenta>`, cómo se construye `DatosVenta` y cómo se llama al servicio una sola vez al final. Continúa desde ahí en la guía de ventas.

Por último lee `Main` para ver de dónde salen los objetos que el menú utiliza. `new` crea las piezas y los constructores las conectan. El menú presenta opciones; las clases del negocio deciden qué operaciones son válidas.
