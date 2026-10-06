# Decisiones de diseño (etapas 1 a 5)

El código sigue el diagrama de clases (Figura 5). Esta tabla registra cada diferencia y su origen.

## Ajustes que pide el plan de implementación

| Cambio respecto al diagrama | Motivo | Origen |
|---|---|---|
| Cantidades y precios con `BigDecimal` en vez de `double` | Decimales exactos | Plan, sección 6 |
| `EstrategiaAsignacion.asignar(cantidad, candidatos, hoy)` devuelve `List<AsignacionLote>` y no descuenta | Preparar toda la operación antes de aplicar cambios; una venta se acepta completa o se rechaza sin cambios | Plan, etapa 5 ("Ajuste propuesto al diseño") |
| `Inventario.planificar(producto, cantidad, hoy)` | Consecuencia del ajuste anterior; el descuento se aplicará en la etapa 6 | Plan, etapa 5 |
| `MovimientoInventario` guarda `referencia` (número de compra o venta) | Auditar qué operación originó el movimiento | Plan, etapa 4 |
| `ServicioCompras.registrarCompra(numero, nit, fecha, lotesRecibidos)` recibe la fecha | `Compra` la exige en su constructor; fechas fijas para pruebas reproducibles | Diagrama (constructor de `Compra`) y plan, sección 6 |

## Detalles de implementación (no cambian el diagrama)

| Elemento | Para qué |
|---|---|
| `Validar` (clase de paquete en `dominio`) | Evitar repetir la validación de textos y datos obligatorios en cada clase |
| `LoteRecibido` (record en `servicio`) | Tipo de los `lotesRecibidos` que el diagrama nombra sin definir |
| `forma` en `ServicioProductos.registrar` acepta "PESO" o "UNIDAD" | Corresponde al discriminador `formaVenta` del modelo de datos |
| Los servicios llaman a `existe()` antes de `guardar()` | `guardar` del diagrama reemplaza; la unicidad (RN-01) se comprueba en el servicio |

## Decisiones de negocio adoptadas

- Un lote está vencido desde su fecha de vencimiento: ese día ya no se vende.
- FEFO desempata lotes con el mismo vencimiento por su código.
- Los mensajes de error terminan en punto y usan lenguaje del negocio (RNF-05).
