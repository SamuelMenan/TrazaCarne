# TrazaCarne: índice de guías y orden de lectura

La primera versión está implementada. Las nuevas guías explican cada archivo mediante fragmentos del código y un recorrido por sus atributos, constructor, métodos, comprobaciones, excepciones y resultados.

Cada bloque tiene un propósito distinto. La numeración de estas guías agrupa el código para estudiarlo; las etapas del plan separaban tareas de desarrollo con más detalle.

## 1. Lectura recomendada

| Orden | Guía | Archivos y conceptos que explica |
|---|---|---|
| 1 | [Productos y POO](PRIMER_BLOQUE_PRODUCTOS.md) | Producto, variantes, cantidades, precio y excepciones iniciales. |
| 2 | [Clientes, abastecedores y repositorios](SEGUNDO_BLOQUE_GESTION.md) | Participantes, enums, interfaz de almacenamiento, implementación y servicios de gestión. |
| 3 | [Compras y lotes](BLOQUE_03_COMPRAS_Y_LOTES.md) | DTO de entrada, servicio, compra, lote, fechas y registro completo. |
| 4 | [Inventario y FEFO](BLOQUE_04_INVENTARIO_Y_FEFO.md) | Asignaciones, estrategia, cantidades, movimientos, entradas, salidas, devoluciones y pérdidas. |
| 5 | [Ventas y anulación](BLOQUE_05_VENTAS_Y_ANULACION.md) | DTO de venta, detalles, precio histórico, estados, registro y devolución. |
| 6 | [Trazabilidad y consultas](BLOQUE_06_TRAZABILIDAD_Y_CONSULTAS.md) | Servicios de consulta y DTO de resultados; origen y destino, anuladas y existencias. |
| 7 | [Consola y Main](BLOQUE_07_CONSOLA_Y_MAIN.md) | Menús, conversiones, excepciones, fin de entrada, dependencias y demostración. |
| 8 | [Pruebas y verificación](BLOQUE_08_PRUEBAS_Y_VERIFICACION.md) | Escenario fijo, pruebas por archivo y método, aserciones y fallos sin cambios parciales. |

Las guías 3 a 8 acompañan los bloques completados en esta entrega. Las guías 1 y 2 conservan la explicación de las sesiones anteriores y se enlazan para seguir el recorrido desde el inicio.

## 2. Cómo estudiar un archivo

1. Abrir el enlace a su archivo Java desde la guía.
2. Leer qué requisito o responsabilidad motivó su creación.
3. Identificar sus atributos y cuáles pueden cambiar.
4. Seguir su constructor para ver cómo nace un objeto válido.
5. Elegir un método y recorrer sus instrucciones con el ejemplo propuesto.
6. Comprobar qué devuelve y en qué condición lanza una excepción.
7. Localizar qué otra clase lo llama.
8. Ejecutar la prueba relacionada y comparar el resultado.

Para empezar con los archivos nuevos, seguir esta cadena concreta:

```text
DatosCompra
  -> ServicioCompras.registrarCompra()
  -> Compra.agregarLote()
  -> Lote (validación y origen)
  -> Compra.registrar()
  -> Inventario.registrarEntrada()
```

Después repetir el ejercicio con `ServicioVentas.registrarVenta()` y sus asignaciones. Las guías muestran los fragmentos y explican qué significa cada paso.

## 3. Qué cambios concretaron el diseño

- Los DTO de entrada usan `record`; sus accesores se escriben como `numero()` o `cantidad()`.
- FEFO devuelve un plan sin descontar cantidades. Inventario valida el plan completo antes de aplicarlo.
- `BORRADOR` representa una venta preparada que todavía no afecta las existencias.
- Descuentos, reposiciones y escritura de movimientos se coordinan dentro de `Inventario`.
- `Lote.descontar()` y `Lote.reponer()` tienen acceso de paquete para evitar cambios directos desde consola o servicios.
- Las identidades, origen, fechas, detalles y precio histórico se conservan sin setters generales.
- Cada entidad mantiene sus propias validaciones; se conserva la decisión de no utilizar una clase global de validaciones.
- `Clock` se entrega a los servicios para obtener un instante consistente y permitir pruebas reproducibles.
- Las consultas históricas muestran ventas anuladas con su estado; el filtro de vigentes permite excluirlas.

Estas decisiones y contratos están documentados en las guías. Los diagramas originales deberán reflejar esas firmas, DTO y el estado de borrador cuando se actualice su representación gráfica; la actualización de los archivos originales de diagramas no forma parte de esta entrega de código y Markdown.

## 4. Evidencia de funcionamiento

La suite completa ejecutó **158 comprobaciones sin fallos ni errores**. Se verifican las clases anteriores, el flujo de compras y ventas, FEFO, anulación, pérdidas, trazabilidad y las entradas de consola.

El programa también dispone de una demostración independiente del día de ejecución:

```powershell
mvn package
java -jar target/trazacarne-1.0-SNAPSHOT.jar --demo
```

Recibe 5 y 20 kg, vende 8 kg con asignaciones de 5 y 3, conserva un total de `192000.00 COP` y restaura las cantidades al anular.

La ejecución normal abre el menú y comienza sin registros cargados:

```powershell
java -jar target/trazacarne-1.0-SNAPSHOT.jar
```

El [README](../README.md) contiene las instrucciones para IntelliJ, Maven y el uso del menú. El [plan](PLAN_IMPLEMENTACION.md) conserva la ruta seguida y el estado de las etapas.
