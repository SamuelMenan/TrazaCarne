# Segundo bloque: clientes, abastecedores y repositorios

Esta etapa completa la gestión básica de clientes, abastecedores y productos, correspondiente a RF-01, RF-02 y RF-03 y a HU-01, HU-02 y HU-03.

Al terminar se pueden registrar, consultar, listar, actualizar y desactivar esos registros. Los datos se almacenan en memoria y los identificadores duplicados se rechazan incluso si pertenecen a registros inactivos.

## 1. Por qué sigue esta etapa

Una compra necesita un abastecedor y productos; una venta necesita un cliente y productos. Antes de construir compras y ventas necesitamos poder localizar a esos participantes por sus identificadores.

El primer bloque protegía los datos de un producto aislado. Ahora podemos comprobar condiciones entre varios objetos: por ejemplo, si otro producto ya tiene el mismo código.

La consola completa se implementará después. Por ahora `Main` demuestra los servicios y las pruebas comprueban sus comportamientos directamente.

## 2. Clases nuevas

| Clase | Responsabilidad |
|---|---|
| `Cliente` | Documento fijo, nombre actualizable y estado activo. |
| `Abastecedor` | NIT y tipo fijos, nombre actualizable y estado activo. |
| `TipoAbastecedor` | Distinguir `MATADERO` y `PROVEEDOR`. |
| `FormaVenta` | Indicar qué variante de producto crear al registrarlo. |
| `Repositorio<T, ID>` | Contrato para guardar, buscar, actualizar y listar. |
| `RepositorioEnMemoria<T, ID>` | Implementar ese contrato mediante un mapa. |
| `ServicioClientes` | Coordinar la gestión de clientes. |
| `ServicioAbastecedores` | Coordinar la gestión de abastecedores. |
| `ServicioProductos` | Crear la variante de producto y coordinar su gestión. |
| `IdentificadorDuplicadoException` | Comunicar que la identidad ya está registrada. |
| `RegistroNoEncontradoException` | Comunicar que un registro solicitado no existe. |
| `RegistroInactivoException` | Comunicar que un registro no puede utilizarse como activo. |

`FormaVenta`, la operación de actualización del repositorio y la excepción de registro inexistente concretan necesidades de implementación. Los diagramas originales orientan el diseño; estas incorporaciones deben reflejarse cuando se actualice su versión.

## 3. De dónde sale cada responsabilidad

| Requisito o regla | Implementación |
|---|---|
| Documento, NIT y código obligatorios | Validación en los objetos del dominio. |
| RN-01: identificadores únicos | Servicios que comprueban existencia y repositorio que impide sobrescribir al registrar. |
| RN-02: desactivar conservando información | `desactivar()` y consultas que incluyen registros inactivos. |
| HU-01 y HU-02: actualizar información | Cambiar nombre conservando la identidad. |
| HU-03 y diagrama de productos | Registro de la variante, actualización de precio y desactivación. |
| RNF-04: almacenamiento inicialmente en memoria, separado | Interfaz de repositorio y su implementación en memoria. |
| RNF-05: mensajes comprensibles | Excepciones específicas con el motivo del rechazo. |

El documento pide identificadores obligatorios y únicos. En esta etapa se aceptan textos no vacíos y se eliminan espacios de sus extremos; no se añade una regla nueva sobre longitud, formato numérico o dígito de verificación.

## 4. Dominio: qué protege cada objeto

La decisión para este proyecto es que cada entidad conserve sus propias validaciones mediante métodos privados, como `validarTexto()`. Se acepta repetir la pequeña comprobación de texto obligatorio para mantener los requisitos de cada entidad junto a ella, siguiendo el mismo criterio que `Producto`. Los servicios comprueban los identificadores de búsqueda y las condiciones que requieren consultar otros registros. No se utiliza una clase global de validaciones.

`Cliente` conserva su documento como `final`. Puede actualizar su nombre, pero la nueva información se valida antes de asignarse. Un nombre inválido deja intacto el anterior.

`Abastecedor` conserva su NIT y su tipo como `final`. `esMatadero()` permite identificar la condición que los futuros lotes utilizarán para exigir la fecha de sacrificio.

No hay clases hijas `Matadero` y `Proveedor`, porque en esta etapa no presentan comportamientos propios que justifiquen esa herencia. La diferencia se expresa con `TipoAbastecedor`.

En ambos casos, desactivar cambia el estado y conserva la identidad y los datos. Una segunda desactivación no vuelve a insertar ni elimina registros.

Se permite corregir el nombre de un registro inactivo, conservando su estado. No se ha implementado reactivación ni cambio de documento, NIT o tipo.

## 5. Qué significan T e ID

La interfaz es genérica:

```java
Repositorio<Cliente, String>
```

En este caso, `T` se sustituye por `Cliente` e `ID` por `String`. El repositorio guarda clientes y los busca mediante documentos representados como texto.

Para productos usamos `Repositorio<Producto, String>`; para abastecedores, `Repositorio<Abastecedor, String>`. Cada instancia mantiene su propia colección. Un documento y un NIT iguales pertenecen a ámbitos de registro distintos.

El contrato tiene estas operaciones:

| Operación | Resultado o regla |
|---|---|
| `guardar(registro)` | Inserta uno nuevo; si existe su identificador, lo rechaza. |
| `actualizar(registro)` | Actualiza uno existente; si no existe, lo rechaza. |
| `buscarPorId(id)` | Devuelve un `Optional`, vacío si no hay resultado. |
| `listar()` | Devuelve una lista con registros activos e inactivos. |
| `existe(id)` | Indica si la identidad ya está registrada. |

Separar registro y actualización evita que `guardar()` reemplace silenciosamente información anterior. La actualización tampoco crea un registro desconocido.

No existe una operación de eliminación en esta primera versión.

## 6. Cómo obtiene el repositorio un identificador

La implementación genérica recibe una función que obtiene la identidad del objeto:

```java
Repositorio<Cliente, String> repositorio =
        new RepositorioEnMemoria<>(Cliente::getDocumento);
```

`Cliente::getDocumento` es una referencia a un método. Para este uso equivale a entregar una función con esta idea:

```java
cliente -> cliente.getDocumento()
```

El repositorio puede pedir el documento de un cliente sin obligar a que `Cliente` conozca mapas, bases de datos o cómo guardarse.

La misma implementación recibe `Abastecedor::getNit` o `Producto::getCodigo` para las otras colecciones.

Internamente se utiliza `LinkedHashMap` para conservar el orden de registro. La búsqueda trabaja con la identidad exacta; los objetos y los servicios normalizan los espacios de las identidades de texto.

## 7. Optional y registros inexistentes

`Optional<T>` representa que puede haber un resultado o no haberlo. El repositorio devuelve un `Optional` vacío cuando no encuentra una identidad.

El servicio interpreta esa ausencia según el caso de uso:

```java
return repositorio.buscarPorId(identificador).orElseThrow(() ->
        new RegistroNoEncontradoException("No existe el cliente solicitado."));
```

Así el repositorio expresa ausencia, y el servicio comunica al usuario que su consulta no puede completarse. Los datos nulos o en blanco se rechazan antes de tratar de realizar una búsqueda.

## 8. Qué hacen los servicios

Los servicios reciben una interfaz por constructor. Por ejemplo:

```java
public ServicioClientes(Repositorio<Cliente, String> repositorio) {
    this.repositorio = repositorio;
}
```

El constructor real comprueba además que esa dependencia no sea nula.

El registro de un cliente sigue este recorrido:

1. Crear `Cliente`, validando sus datos.
2. Obtener el documento normalizado.
3. Comprobar si ya existe.
4. Solicitar al repositorio que lo guarde.
5. Devolver el cliente registrado.

La comprobación del servicio comunica un mensaje propio del caso de uso. El repositorio también protege su contrato frente a inserciones duplicadas, aunque se utilice directamente.

La actualización localiza al objeto, ejecuta la operación del dominio y solicita su actualización al repositorio. La implementación en memoria conserva las mismas referencias de objeto utilizadas por los servicios, lo que permitirá que otras relaciones conozcan al mismo participante.

Las operaciones y las colecciones no dependen de métodos de lectura del teclado ni de salida por pantalla.

## 9. Consultar no es lo mismo que exigir un registro activo

Se ofrecen dos operaciones:

```java
Cliente historico = clientes.consultar("1001");
Cliente habilitado = clientes.consultarActivo("1001");
```

`consultar()` encuentra el registro aunque esté inactivo. Esto es necesario para conservar información histórica.

`consultarActivo()` también exige que esté activo. Si no lo está, lanza `RegistroInactivoException`. Los futuros servicios de compras y ventas podrán utilizar esta comprobación para las nuevas operaciones.

Los listados conservan activos e inactivos. Desactivar no libera un identificador para volver a registrarlo.

## 10. Qué protección tiene la lista devuelta

`listar()` devuelve una copia no modificable de la colección. Quien consulta no puede añadir o eliminar registros mediante esa lista ni acceder al mapa interno.

Los objetos contenidos siguen siendo objetos del dominio: sus operaciones permitidas pueden cambiar nombre, precio o estado. La lista protege su estructura, no convierte esos objetos en datos totalmente inmutables.

Cuando se necesiten resultados de consulta destinados únicamente a presentación, podrán introducirse DTO de salida. Los DTO de entrada para compras y ventas se añadirán en sus etapas, donde hay varios elementos y fechas que transportar.

## 11. Cómo se registran las variantes de Producto

`ServicioProductos.registrar()` recibe `FormaVenta`. Un `switch` localizado en el registro crea `ProductoPorPeso` o `ProductoPorUnidad`.

Después, las cantidades siguen validándose mediante polimorfismo. Los consumidores de `Producto` no necesitan preguntar su tipo concreto para calcular o validar.

Si se incorpora otra forma de venta, deberá ampliarse esta decisión de creación. En esta etapa se mantiene explícita para las dos formas definidas en el alcance.

## 12. POO y SOLID visibles en esta etapa

- **Encapsulación:** identidad fija y actualización mediante operaciones validadas.
- **Abstracción:** `Repositorio` define lo necesario para almacenar sin exponer un mapa.
- **Polimorfismo:** `RepositorioEnMemoria` cumple la interfaz y los productos conservan su validación por variante.
- **Responsabilidad única:** dominio protege datos; servicios coordinan; repositorio conserva registros; `Main` conecta y demuestra.
- **Inversión de dependencias:** los servicios conocen `Repositorio`, sin importar `RepositorioEnMemoria`.

El cambio futuro de almacenamiento requerirá otra implementación y decisiones sobre persistencia y transacciones. Este bloque prepara el contrato para ese cambio.

## 13. Cómo leer Main

`Main` crea tres repositorios, los entrega a tres servicios y registra un cliente, dos abastecedores y dos productos. Después utiliza el cálculo del primer bloque y demuestra una desactivación.

La última parte consulta al cliente desactivado y captura el rechazo de `consultarActivo()`. Es una demostración del comportamiento del servicio; todavía no se está registrando una venta.

La demostración se ejecuta desde el botón verde de `main`. Al cerrar el programa se pierden los registros en memoria.

## 14. Comprobaciones realizadas

La etapa añadió pruebas de participantes, repositorios y servicios. En total, con el bloque anterior, se ejecutaron 112 comprobaciones sin fallos ni errores.

Se comprobó lo siguiente:

- Normalización de identidades y rechazo de datos obligatorios ausentes.
- Actualizaciones válidas y conservación del valor anterior ante un rechazo.
- Diferencia entre matadero y proveedor.
- Registro duplicado sin reemplazar el objeto original, activo o inactivo.
- Consulta inexistente y actualización desconocida sin insertar datos.
- Listados no modificables, con orden e inclusión de inactivos.
- Separación de datos entre instancias de repositorio.
- Consultas históricas y rechazo de uso activo tras desactivación.
- Creación de productos con la validación correspondiente a su forma de venta.
- Funcionamiento de las 46 comprobaciones del primer bloque.

También se generó y ejecutó el JAR de demostración.

## 15. Orden recomendado de lectura y siguiente etapa

1. Leer `Cliente` y `Abastecedor` para identificar qué protegen.
2. Leer la interfaz `Repositorio` y comprender su contrato.
3. Leer `RepositorioEnMemoria`, centrándose primero en `guardar()` y `buscarPorId()`.
4. Leer `ServicioClientes` y seguir el recorrido de registro y consulta.
5. Comparar los otros dos servicios.
6. Leer `Main` para ver cómo se conectan.
7. Ejecutar las pruebas desde Maven con `mvn test`.

La siguiente etapa será registrar compras y crear lotes con origen, fechas y movimientos de entrada. Ahora ya existen los registros que esa operación necesita localizar.
