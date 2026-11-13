# TP Integrador · DAW1 — Taller mecánico

**Alumno/a:** Martínez Cánepa, Gabriel Ernesto · **Legajo:** 510143
**Enunciado elegido:** taller

## Descripción

API REST para registrar clientes, vehículos y órdenes de trabajo de un taller mecánico.
Permite seguir las reparaciones, cargar costos y consumos de repuestos, y consultar el historial y la facturación mensual.

## Cómo ejecutarlo

Requisitos: Java 21 (Maven viene incluido con `mvnw`).

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`. La base H2 se guarda en `data/taller.mv.db`, conserva los datos entre reinicios y no se sube al repositorio.

## Modelo

| Entidad         | Atributos principales                                                                                                      | Relaciones                                    |
| --------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| Cliente         | id, nombre, telefono                                                                                                       | 1 a N con Vehiculo                            |
| Vehiculo        | id, patente, marca, modelo                                                                                                 | N a 1 con Cliente; 1 a N con Orden            |
| Orden           | id, descripcion, estado, costoFinal, fechaIngreso, fechaInicioReparacion, fechaTerminacion, fechaEntrega, fechaCancelacion | N a 1 con Vehiculo; 1 a N con ConsumoRepuesto |
| Repuesto        | id, nombre, precio, stock                                                                                                  | 1 a N con ConsumoRepuesto                     |
| ConsumoRepuesto | id, cantidad                                                                                                               | N a 1 con Orden y Repuesto                    |

Estados de Orden: `INGRESADO → EN_REPARACION → TERMINADO → ENTREGADO` e `INGRESADO → CANCELADO`.

Las relaciones se devuelven en JSON como `clienteId`, `vehiculoId`, `ordenId` y `repuestoId`. Las fechas usan formato ISO. Las órdenes con consumos devuelven `costoRepuestos` (suma de precio por cantidad); una vez terminadas devuelven `horasEnReparacion` entre el inicio y el fin de la reparación, conservadas después de entregar.

## Endpoints

| Método | Ruta                                       | Descripción                                                                | Respuestas         |
| ------ | ------------------------------------------ | -------------------------------------------------------------------------- | ------------------ |
| POST   | /api/clientes                              | Crea un cliente                                                            | 201, 400           |
| GET    | /api/clientes/{id}/vehiculos               | Lista los vehículos del cliente                                            | 200, 400, 404      |
| POST   | /api/vehiculos                             | Crea un vehículo                                                           | 201, 400, 404, 409 |
| GET    | /api/vehiculos                             | Lista vehículos                                                            | 200                |
| GET    | /api/vehiculos/{id}                        | Busca un vehículo por id                                                   | 200, 400, 404      |
| PUT    | /api/vehiculos/{id}                        | Modifica un vehículo                                                       | 200, 400, 404, 409 |
| DELETE | /api/vehiculos/{id}                        | Borra un vehículo sin órdenes                                              | 204, 400, 404, 409 |
| GET    | /api/vehiculos/patente/{patente}/historial | Lista las órdenes de la más nueva a la más vieja                           | 200                |
| POST   | /api/ordenes                               | Crea una orden en estado INGRESADO                                         | 201, 400, 404, 409 |
| GET    | /api/ordenes                               | Lista órdenes; admite filtro por estado, por ejemplo ?estado=EN_REPARACION | 200, 400           |
| PUT    | /api/ordenes/{id}                          | Modifica descripción y costoFinal                                          | 200, 400, 404      |
| PATCH  | /api/ordenes/{id}/estado                   | Cambia el estado con {"estado":"EN_REPARACION"}                            | 200, 400, 404, 409 |
| POST   | /api/ordenes/{id}/repuestos                | Registra un consumo con repuestoId y cantidad y descuenta stock            | 201, 400, 404, 409 |
| GET    | /api/ordenes/facturacion?mes=2026-11       | Devuelve cantidadOrdenes y totalFacturado por mes de entrega               | 200, 400           |
| POST   | /api/repuestos                             | Crea un repuesto con nombre, precio y stock                                | 201, 400           |
| GET    | /api/repuestos                             | Lista repuestos                                                            | 200                |

## Reglas de negocio

1. Los campos obligatorios se validan con `400`: nombre y teléfono del cliente; patente, marca, modelo y cliente del vehículo; vehículo y descripción de la orden; nombre, precio y stock del repuesto; repuesto y cantidad positiva del consumo.
2. La patente debe tener formato `ABC123` o `AB123CD` (`400`) y ser única (`409`). Al actualizar se permite conservar la propia.
3. Un vehículo puede tener una sola orden abierta: cualquier estado distinto de `ENTREGADO` o `CANCELADO`. Crear otra mientras siga abierta devuelve `409`.
4. Saltar estados, retroceder o cancelar fuera de `INGRESADO` devuelve `409`.
5. Entregar una orden sin `costoFinal` devuelve `409`.
6. Borrar un vehículo con órdenes asociadas devuelve `409` y conserva sus datos, sin borrar órdenes en cascada.
7. Registrar un consumo y descontar stock se realiza en una misma transacción. Si no alcanza el stock, devuelve `409` sin modificar datos.
8. Al actualizar una orden con consumos, un `costoFinal` inferior a `costoRepuestos` devuelve `400` sin guardar cambios.

## Pruebas

Las pruebas unitarias, de integración y de API están en `src/test/java/ar/edu/undec/taller/`, en los paquetes `model`, `service`, `repository` y `controller`. Cubren los escenarios obligatorios y los cinco extras.

```bash
./mvnw -B -ntp test
```

Las pruebas de Spring usan el perfil `test` con H2 en memoria, sin afectar los datos de desarrollo. Los resultados quedan en `target/surefire-reports/`.
