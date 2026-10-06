# Primer bloque: productos y reglas de cantidad

Este bloque implementa una parte pequeña del dominio para comprender cómo los requisitos se convierten en clases, comportamiento y pruebas. Se conserva la estructura `co.trazacarne` y se utiliza Java con Maven.

Esta guía conserva la explicación de la primera sesión. La versión actual de `Main` registra los productos mediante servicios; esa evolución se explica en [Segundo bloque: gestión y repositorios](SEGUNDO_BLOQUE_GESTION.md).

## 1. Por qué empezamos por Producto

Una compra recibe productos y una venta los entrega. Antes de escribir esas operaciones necesitamos saber qué producto se está manejando, cuánto cuesta y qué cantidades acepta.

`Producto` depende de pocos conceptos y permite demostrar los cuatro pilares de POO mediante una regla del negocio: los productos por peso aceptan cantidades decimales y los productos por unidad requieren valores enteros.

## 2. Qué archivos se crearon o modificaron

| Archivo | Responsabilidad |
|---|---|
| `Producto.java` | Datos comunes, precio, desactivación y contrato de cantidad válida. |
| `ProductoPorPeso.java` | Cantidades positivas en kilogramos con hasta tres decimales. |
| `ProductoPorUnidad.java` | Cantidades positivas que representan valores enteros. |
| `ReglaNegocioException.java` | Motivo comprensible para rechazar una operación. |
| `CantidadInvalidaException.java` | Rechazo específico de una cantidad incompatible con el producto. |
| `ProductoTest.java` | Escenarios válidos e inválidos del primer bloque. |
| `Main.java` | Demostración con un producto por peso y otro por unidad. |
| `pom.xml` | Compilación con JDK 27, pruebas JUnit y JAR ejecutable. |

Las clases del dominio están en `src/main/java/co/trazacarne/dominio`. Las excepciones están en `src/main/java/co/trazacarne/excepcion` y las pruebas en `src/test/java/co/trazacarne/dominio`.

## 3. De dónde sale cada parte

| Fuente | Decisión de código |
|---|---|
| RF-03 y HU-03: registrar productos con sus datos | Atributos de código, nombre, especie, corte y precio. |
| RN-01: identificador obligatorio | El constructor rechaza un código nulo o en blanco. |
| RN-02: conservar registros mediante desactivación | `desactivar()` cambia el estado y conserva el objeto. |
| RN-06: cantidad según forma de venta | `validarCantidad()` se implementa en cada variante. |
| RNF-02: reglas separadas del menú | Las validaciones se ejecutan en las clases del dominio. |
| RNF-05: explicar operaciones imposibles | Excepciones con mensajes de negocio. |
| Diagrama de clases: precio y subtotal | Operaciones `actualizarPrecio()` y `calcularSubtotal()`. |

El código obligatorio ya se comprueba aquí. La unicidad global del código se comprobará cuando exista `ServicioProductos` con su repositorio, porque un producto aislado no conoce a todos los demás.

## 4. Decisiones de implementación adoptadas para este bloque

Las siguientes decisiones concretan propuestas del plan y pueden revisarse si el curso o el negocio especifican otros criterios:

- El peso se expresa en kilogramos y admite hasta tres decimales significativos.
- Las unidades deben ser valores enteros positivos.
- Las cantidades y precios se representan con `BigDecimal`.
- El precio debe ser positivo y admite hasta dos decimales significativos; se conserva con escala de dos decimales.
- Los subtotales se redondean a dos decimales con `HALF_UP`: un tercer decimal de cinco o más eleva el segundo decimal.
- El ejemplo utiliza pesos colombianos (COP).
- Los textos obligatorios se validan y se eliminan espacios de sus extremos.

Los límites de precisión y el redondeo son decisiones de implementación; RN-06 por sí sola distingue unidades y peso, pero no establece esos límites.

Ejemplos:

| Dato | Resultado |
|---|---|
| Peso `1.250` | Válido. |
| Peso `1.2340` | Válido; el último cero no añade precisión. |
| Peso `1.2345` | Rechazado. |
| Unidades `3` o `3.0` | Válidas; ambas representan tres unidades. |
| Unidades `3.5` | Rechazadas. |
| Cantidad `0`, negativa o ausente | Rechazada. |
| Precio `12.3400` | Válido y conservado como `12.34`. |
| Precio `12.345` | Rechazado, sin redondearlo silenciosamente. |

Las decisiones sobre vencimiento y trazabilidad siguen pendientes para sus respectivas etapas.

## 5. Cómo leer Producto.java

### Atributos privados

`private` obliga a utilizar las operaciones que la clase ofrece. El precio no se modifica directamente desde `Main`; se cambia mediante `actualizarPrecio()`, que valida el nuevo valor.

Código, nombre, especie y corte son `final` en este bloque. Reciben un valor al construir el objeto y no tienen operaciones para cambiarlos. El precio y el estado activo sí pueden cambiar mediante operaciones específicas.

### Constructor

El constructor recibe los datos necesarios, los valida y deja el producto activo. Así se evita crear un producto sin código o con un precio inválido.

El constructor de `Producto` es `protected` porque lo utilizan sus clases hijas. Como `Producto` es abstracto, los objetos concretos se crean como `ProductoPorPeso` o `ProductoPorUnidad`.

### Contrato abstracto

```java
public abstract void validarCantidad(BigDecimal cantidad);
```

La clase base declara la operación y sus variantes la implementan. El contrato es validar una cantidad positiva de acuerdo con la forma de venta; no promete aceptar fracciones en todas las variantes.

### Comportamiento compartido

`calcularSubtotal()` llama a `validarCantidad()`, multiplica el precio por la cantidad y aplica el redondeo acordado. La validación utilizada es la del objeto concreto.

`actualizarPrecio()` valida primero y asigna después. Si el precio nuevo es inválido, la operación lanza una excepción y conserva el precio anterior.

`desactivar()` conserva el objeto y cambia su estado. Los futuros servicios impedirán nuevas operaciones con productos inactivos cuando corresponda. El cálculo puro de un subtotal no elimina ni bloquea el acceso al historial.

## 6. Para qué usamos BigDecimal

`BigDecimal` permite representar cantidades decimales exactas. Los ejemplos se construyen desde texto:

```java
BigDecimal cantidad = new BigDecimal("1.250");
```

Los métodos utilizados en este bloque tienen propósitos concretos:

| Método | Uso |
|---|---|
| `signum()` | Comprobar si el valor es positivo, cero o negativo. |
| `stripTrailingZeros()` | Ignorar ceros finales que no cambian el valor al revisar la precisión. |
| `scale()` | Consultar los decimales de la representación. |
| `multiply()` | Calcular precio por cantidad. |
| `setScale()` | Fijar los decimales de precio o subtotal. |
| `toPlainString()` | Mostrar el número sin notación científica en la demostración. |

Para unidades, después de eliminar ceros finales, una escala positiva indica una fracción. Por eso `3.000` se acepta y `3.5` se rechaza.

## 7. Los cuatro pilares en un ejemplo

```java
Producto producto = new ProductoPorPeso(
        "P-001", "Carne molida", "Bovino", "Molida",
        new BigDecimal("24000.00"));

BigDecimal subtotal = producto.calcularSubtotal(new BigDecimal("1.250"));
```

- **Encapsulación:** los atributos privados se modifican mediante operaciones que validan.
- **Abstracción:** `Producto` define lo común y el contrato de cantidad válida.
- **Herencia:** `ProductoPorPeso` y `ProductoPorUnidad` extienden `Producto`.
- **Polimorfismo:** el cálculo llama a la validación de la variante concreta, aunque la referencia sea `Producto`.

El ejemplo produce `30000.00`. Si el objeto fuera un producto por unidad, la cantidad `1.250` se rechazaría.

## 8. SOLID en este punto

La responsabilidad de estas clases es representar productos y proteger sus datos y cantidades. La salida por pantalla está en `Main`.

Las variantes respetan el contrato común y permiten usar polimorfismo sin añadir condiciones que pregunten si el objeto es de un tipo u otro.

La inversión de dependencias mediante repositorios y la estrategia FEFO se implementarán en etapas posteriores. Tener este primer bloque no significa que ya hayamos completado todos los ejemplos de SOLID del sistema.

## 9. Cómo comprobar el bloque

Ejecutar `Main` muestra dos subtotales. Ejecutar `ProductoTest` comprueba también los rechazos importantes: cantidades inválidas, precisión excesiva, textos obligatorios, precio inválido y conservación del precio anterior.

Las pruebas parametrizadas reutilizan un mismo escenario con varios datos. Por eso la cantidad de casos ejecutados es mayor que la cantidad de métodos de prueba.

Para leer las pruebas, comenzar por la validación polimórfica y por la actualización con precio inválido: muestran cómo comprobar tanto el resultado como la conservación del estado ante un rechazo.

## 10. Siguiente paso

Leer primero `Producto.java`, después sus dos variantes y finalmente `Main.java`. Continuar con `Cliente`, `Abastecedor` y el contrato de repositorio cuando las reglas de este bloque estén comprendidas.

Las ventas futuras guardarán una copia del precio aplicado en `DetalleVenta`. El subtotal de `Producto` utiliza el precio actual y todavía no representa una venta histórica.
