# Verificacion - Producto Service

Fecha de la verificacion de la migracion JWT y roles: **2026-11-11** (hora local del equipo).

Este documento registra lo que se ejecuto realmente y lo que no pudo verificarse.

---

## 1. Compilacion y pruebas actuales

```powershell
.\mvnw.cmd clean test
```

Resultado observado:

```text
Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

El proyecto se compilo con JDK 25 en esta maquina, apuntando a `release 21` segun `pom.xml`.

### 1.1 Resultado por clase

| Clase | Tests | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| `CatalogoInitializerIntegrationTest` | 1 | 0 | 0 | 0 |
| `CatalogoResourceTest` | 1 | 0 | 0 | 0 |
| `ProductoServiceApplicationTests` | 1 | 0 | 0 | 0 |
| `SecurityIntegrationTest` | 23 | 0 | 0 | 0 |
| `CategoriaServiceTest` | 10 | 0 | 0 | 0 |
| `InventarioServiceTest` | 8 | 0 | 0 | 0 |
| `ProductoServiceTest` | 17 | 0 | 0 | 0 |
| `ReservaConcurrenteIntegrationTest` | 3 | 0 | 0 | 0 |
| `ReservaStockServiceTest` | 10 | 0 | 0 | 0 |
| **Total** | **74** | **0** | **0** | **0** |

Las pruebas de contexto e integracion usan H2 en modo MySQL (`src/test/resources/application-test.properties`).

### 1.2 Cobertura agregada en esta iteracion

- Token Nimbus valido firmado con HS256.
- Subject igual al correo y claim `rol`.
- Token expirado, firma incorrecta, token sin rol y token malformado.
- Matriz de roles `ADMIN`, `EMPLEADO` y `CLIENTE` con accesos permitidos y denegados.
- CRUD administrativo de productos: crear, actualizar, desactivar y reactivar.
- Consulta administrativa de productos inactivos y paginacion.
- Consulta administrativa de categorias inactivas.
- Listado publico de categorias limitado a activas.
- Desactivacion de categoria con productos activos: `409 Conflict`.
- Reservas: idempotencia secuencial y concurrente, sobreventa y referencia duplicada entre productos.
- CORS con `Cache-Control` y `Pragma`.

### 1.3 Incidencia corregida durante la verificacion

La primera version del test de concurrencia con la misma referencia fallaba con:

```text
ObjectOptimisticLockingFailureException:
Query result contains conflicting version of entity already held in persistence context
```

Causa: la consulta inicial del producto cargaba `Inventario` en el contexto de persistencia y, tras esperar el bloqueo pesimista, la fila tenia una version nueva.

Correccion aplicada:

- La reserva comprueba la existencia/actividad del producto con `existsByCodigoAndActivoTrue`, sin cargar entidades.
- El inventario se obtiene directamente con `findByProductoCodigoForUpdate`.
- La referencia existente se relee con `findByReferenciaForUpdate` (current read), lo que mantiene la idempotencia concurrente.

Tras la correccion, las 74 pruebas pasan.

---

## 2. Migracion JWT

| Elemento | Estado |
|---|---|
| Dependencias JJWT | Eliminadas de `pom.xml` |
| `JwtService` y `JwtAuthenticationFilter` locales | Eliminados |
| Validacion | `NimbusJwtDecoder` con `MacAlgorithm.HS256` |
| Claim de rol | `rol` |
| Prefijo de autoridades | `ROLE_` |
| Subject | Correo del usuario |
| Expiracion | Validada por Spring Security OAuth2 Resource Server |
| Generacion de tokens | No existe en este servicio |
| Secreto | `JWT_SECRET` por variable de entorno; `.env` excluido de Git |

La configuracion de referencia de `usuario-service` se consulto en modo lectura. No se modifico ese proyecto.

### 2.1 Secreto local

Se alineo el `JWT_SECRET` del `.env` local de `producto-service` con el valor de referencia de `usuario-service` sin imprimirlo en consola. Verificacion realizada comparando longitud y hash:

```text
Longitud local: 39 bytes; longitud referencia: 39 bytes
Coincide: True
```

El valor real no se incluye en este documento, en README, en archivos de ejemplo ni en el codigo.

> Spring Boot no lee `.env` automaticamente. Para ejecucion local debe exportarse `JWT_SECRET` en la terminal o configurarse en el IDE.

---

## 3. Matriz de roles verificada

| Rol | Productos | Categorias | Inventario | Reservas |
|---|---|---|---|---|
| `ADMIN` | CRUD completo, activos e inactivos | CRUD completo, activas e inactivas | Administra | Administra |
| `EMPLEADO` | Solo lectura del catalogo publico | Solo lectura | Consulta y actualiza stock | Crea, consulta, confirma y libera |
| `CLIENTE` | Solo lectura del catalogo publico | Solo lectura | Solo disponibilidad publica | Sin acceso |

Los endpoints `/api/productos/admin/**` y `/api/categorias/admin/**` responden `403` para `EMPLEADO` y `CLIENTE`.

---

## 4. Endpoints administrativos agregados

| Metodo | Ruta | Resultado verificado |
|---|---|---|
| GET | `/api/productos/admin` | Incluye activos e inactivos |
| GET | `/api/productos/admin/paginado` | Pagina activos e inactivos |
| GET | `/api/productos/admin/{codigo}` | Devuelve productos inactivos |
| GET | `/api/categorias/admin` | Incluye activas e inactivas |

El listado publico `GET /api/categorias` solo devuelve categorias activas.

---

## 5. Regla de categorias y concurrencia

| Escenario | Resultado |
|---|---|
| `PUT /api/categorias/{id}` con `activo=false` y productos activos | `409 Conflict` con mensaje comprensible |
| Desactivar la categoria despues de dar de baja los productos | `200 OK` |
| Dos reservas simultaneas por la ultima unidad | Solo una reserva creada |
| Dos reservas simultaneas con la misma referencia y producto | Ambas terminan OK; una sola reserva y un solo descuento |
| Misma referencia simultanea con productos distintos | Solo una reserva creada; la otra recibe conflicto |

La baja de productos sigue siendo logica (`activo=false`) y conserva el registro historico.

---

## 6. Verificacion historica contra MySQL 8.4 (iteracion anterior)

La siguiente verificacion se realizo antes de la migracion de roles. Se conserva como evidencia historica del catalogo, precios, stock y reservas; **los JWT usados entonces tenian el rol `ADMINISTRADOR`, que ya no es valido** con la matriz actual.

### 6.1 Entorno

- MySQL Community Server **8.4.3** (binarios de Laragon).
- Instancia temporal aislada en un directorio temporal, puerto **3307**.
- No se utilizo ni modifico la base de datos existente del usuario.

### 6.2 Resultados observados

```text
Catalogo procesado: 51 productos en archivo, 51 creados, 0 ya existentes,
10 categorias nuevas, 51 inventarios nuevos

productos    51
categorias   10
inventarios  51
stock_total  365
reservado    0
```

Idempotencia al reiniciar:

```text
Catalogo procesado: 51 productos en archivo, 0 creados, 51 ya existentes,
0 categorias nuevas, 0 inventarios nuevos
```

Tambien se verificaron precios, ofertas, actualizacion de stock, baja logica, reservas, confirmacion, liberacion, CORS y Swagger contra esa instancia temporal.

La instancia temporal fue detenida y eliminada al finalizar. No queda ningun proceso MySQL ni aplicacion en ejecucion por parte de esta verificacion.

---

## 7. Verificaciones no realizadas

| Elemento | Estado | Motivo |
|---|---|---|
| Build de imagen Docker | **No ejecutado** | El daemon de Docker no estaba disponible (`dockerDesktopLinuxEngine` no encontrado) |
| `docker compose up` | **No ejecutado** | Misma causa |
| Prueba contra la base MySQL real del usuario | **No ejecutada en esta iteracion** | Se uso H2 para las pruebas automatizadas y se evito tocar datos existentes |
| Migraciones Flyway/Liquibase | **No implementadas** | Fuera del alcance solicitado; se usa `ddl-auto=update` en desarrollo |
| Ruta de `usuario-service` en el API Gateway | **Pendiente del responsable del Gateway** | El Gateway no expone actualmente `/api/v1/tienda-sonido/**` para `usuario-service`; no se modifico ese proyecto |

---

## 8. Conclusiones

- La aplicacion compila y las **74 pruebas automatizadas pasan**.
- La validacion JWT migro a Spring Security OAuth2 Resource Server con Nimbus y HS256, sin generacion de tokens.
- La matriz de roles quedo limitada a `ADMIN`, `EMPLEADO` y `CLIENTE`.
- Los endpoints administrativos y la regla `409` de categorias estan cubiertos por pruebas.
- La concurrencia de reservas quedo cubierta con tres escenarios.
- Docker no pudo verificarse en ejecucion porque el daemon no estaba disponible.
