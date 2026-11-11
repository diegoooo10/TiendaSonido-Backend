# Verificacion - Producto Service

Fecha de verificacion: **2026-11-10 / 2026-11-11** (hora local del equipo).

Este documento registra lo que se ejecuto realmente y lo que no pudo verificarse.

---

## 1. Compilacion

```powershell
.\mvnw.cmd clean package
```

Resultado observado:

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
Building jar: ...\target\producto-service-0.0.1-SNAPSHOT.jar
BUILD SUCCESS
```

El JAR generado se ejecuto posteriormente contra MySQL 8.4.

---

## 2. Pruebas automatizadas

Comando:

```powershell
.\mvnw.cmd test
```

Resultado por clase:

| Clase | Tests | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| `CatalogoInitializerIntegrationTest` | 1 | 0 | 0 | 0 |
| `CatalogoResourceTest` | 1 | 0 | 0 | 0 |
| `ProductoServiceApplicationTests` | 1 | 0 | 0 | 0 |
| `SecurityIntegrationTest` | 13 | 0 | 0 | 0 |
| `CategoriaServiceTest` | 7 | 0 | 0 | 0 |
| `InventarioServiceTest` | 8 | 0 | 0 | 0 |
| `ProductoServiceTest` | 14 | 0 | 0 | 0 |
| `ReservaConcurrenteIntegrationTest` | 1 | 0 | 0 | 0 |
| `ReservaStockServiceTest` | 10 | 0 | 0 | 0 |
| **Total** | **56** | **0** | **0** | **0** |

Las pruebas de contexto e integracion usan H2 en modo MySQL. Esto no sustituye una prueba contra MySQL real; por eso se agrego la verificacion de la seccion 3.

### Incidencia corregida durante la verificacion

Spring Boot 4 usa Jackson 3. El codigo inicial importaba `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2), por lo que no existia un bean `ObjectMapper` y 16 pruebas fallaban al crear el contexto.

Correccion aplicada:

- Dependencia `spring-boot-starter-jackson`.
- Imports migrados a `tools.jackson.databind.ObjectMapper` y `tools.jackson.core.type.TypeReference` en el codigo de la aplicacion y en `CatalogoResourceTest`.

Tras la correccion, las 56 pruebas pasaron.

---

## 3. Verificacion contra MySQL 8.4 real

### Entorno

- MySQL Community Server **8.4.3** (binarios de Laragon).
- Instancia temporal aislada en un directorio temporal, puerto **3307**.
- No se utilizo ni modifico la base de datos existente del usuario.
- Aplicacion ejecutada con el JAR empaquetado y variables de entorno reales:

```text
DB_URL=jdbc:mysql://127.0.0.1:3307/sonidovivo_productos?...
DB_USERNAME=root
DB_PASSWORD=(vacio, solo instancia temporal)
JWT_SECRET=secreto-de-verificacion-de-32-bytes-o-mas
SERVER_PORT=18083
```

### 3.1 Carga inicial del catalogo

Log observado en el primer arranque:

```text
Catalogo procesado: 51 productos en archivo, 51 creados, 0 ya existentes, 10 categorias nuevas, 51 inventarios nuevos
```

Consultas directas a MySQL:

```text
productos    51
categorias   10
inventarios  51
stock_total  365
reservado    0
```

API publica:

```text
GET /api/productos   -> 51 elementos
GET /api/categorias  -> 10 elementos
GET /api/productos/ofertas -> []
```

Tambien se verifico que los nombres con tildes se almacenan y devuelven en UTF-8 correctamente (por ejemplo `Guitarra Acústica Folk`, `Afinador de Clip`).

### 3.1.1 Comparacion exacta con el catalogo original

Se comparo `src/main/resources/catalogo/productos.json` contra el archivo original del frontend `src/data/productos.js`, campo por campo (`codigo`, `categoria`, `nombre`, `marca`, `modelo`, `stock`, `precio`, `descripcion`, `imagen`):

```text
JS_PRODUCTOS=51 JSON_PRODUCTOS=51
CODIGOS_UNICOS=51 CATEGORIAS=10 STOCK_TOTAL=365
OFERTAS=0
DIFERENCIAS=0
```

No se inventaron productos, precios, codigos ni stock.

### 3.2 Idempotencia al reiniciar

Se detuvo la aplicacion y se inicio de nuevo contra la misma base. Log observado:

```text
Catalogo procesado: 51 productos en archivo, 0 creados, 51 ya existentes, 0 categorias nuevas, 0 inventarios nuevos
```

Consultas posteriores:

```text
productos    51
categorias   10
inventarios  51
stock_total  365
```

Conclusion: la carga inicial no duplica datos ni sobrescribe stock.

### 3.3 Precios y ofertas

Se creo un producto temporal `ZZTEST` con:

```json
{
  "precio": 1000.00,
  "precioOferta": 800.00,
  "enOferta": true
}
```

Resultado:

- `POST /api/productos` -> `201 Created`.
- `precioEfectivo` devuelto: **800.00**.
- `GET /api/productos/ofertas` incluyo el producto.
- Intento de crear una oferta con `precioOferta=1200` y `precio=1000` -> `400 Bad Request` con mensaje `El precioOferta debe ser menor al precio normal`.
- `PUT /api/inventario/ZZTEST/stock` con `stockFisico=5` -> `200 OK`, `stockDisponible=5`.
- `DELETE /api/productos/ZZTEST` -> `204 No Content`.
- `GET /api/productos/ZZTEST` posterior -> `404 Not Found` (baja logica).

### 3.4 Reservas de stock

Producto usado: `AC006` (stock inicial 20).

| Operacion | Resultado |
|---|---|
| `POST /api/inventario/reservas` (cantidad 2) | `201 Created`, `stockDisponible=18` |
| Repetir exactamente la misma reserva | `200 OK`, sin cambios de stock |
| Reutilizar referencia con cantidad 3 | `409 Conflict`, mensaje de referencia reutilizada |
| `POST .../confirmar` | `200 OK`, estado `CONFIRMADA` |
| `GET /api/inventario/AC006` | `stockFisico=18`, `stockReservado=0`, `stockDisponible=18` |
| `POST .../liberar` sobre reserva confirmada | `409 Conflict` |
| `GET /api/inventario/reservas/{referencia}` | estado `CONFIRMADA` |

### 3.5 Seguridad, CORS y documentacion

| Verificacion | Resultado |
|---|---|
| `GET /api/inventario` sin token | `401` con JSON de error |
| `GET /api/inventario` con JWT `ADMINISTRADOR` | `200` |
| `POST /api/productos` con JWT `CLIENTE` | `403` |
| `POST /api/productos` con JWT `ADMINISTRADOR` | `201` |
| `GET /api/categorias` con `Origin: http://localhost:5173` | `Access-Control-Allow-Origin: http://localhost:5173` |
| `GET /v3/api-docs` | `200` |
| `GET /swagger-ui.html` | `302` (redireccion a la UI) |
| `DELETE /api/categorias/1` con productos asociados | `409`, no elimina la categoria |

El JWT de verificacion se genero con el mismo contrato de `autenticacion-service`:

```json
{ "sub": "verificador", "rol": "ADMINISTRADOR", "iat": 0, "exp": 0 }
```

firmado con HS256 y el secreto compartido. No se modifico `autenticacion-service`.

### 3.6 Conteo final despues de las pruebas manuales

Las pruebas manuales modificaron la instancia temporal (producto temporal y una reserva confirmada):

```text
total_productos    52
productos activos  51
categorias         10
stock_fisico_total 368  (365 - 2 confirmadas + 5 del producto temporal)
reservado_total    0
reservas           1 (PED-VERIF-1-AC006, CONFIRMADA)
```

La base temporal fue detenida y eliminada al finalizar. No queda ningun proceso MySQL ni aplicacion en ejecucion.

---

## 4. Verificaciones no realizadas

| Elemento | Estado | Motivo |
|---|---|---|
| Build de imagen Docker | **No ejecutado** | El daemon de Docker no estaba disponible (`dockerDesktopLinuxEngine` no encontrado) |
| `docker compose up` | **No ejecutado** | Misma causa |
| Prueba con Laragon sobre la base real del usuario | **No ejecutada** | Se uso una instancia temporal aislada para no tocar datos existentes |
| Migraciones Flyway/Liquibase | **No implementadas** | Fuera del alcance solicitado; se usa `ddl-auto=update` en desarrollo |
| Roles `ADMINISTRADOR`/`VENDEDOR`/`CLIENTE` emitidos por autenticacion-service | **Pendiente en autenticacion-service** | Ese servicio aun emite `ADMIN` y `EMPLEADO`; aqui se aceptan ambos esquemas |

---

## 5. Conclusiones

- La aplicacion compila y las **56 pruebas automatizadas pasan**.
- La carga del catalogo oficial fue verificada contra **MySQL 8.4 real**: 51 productos, 10 categorias, 51 inventarios y 365 unidades, con idempotencia al reiniciar.
- Los flujos de precios, ofertas, stock, reservas, confirmacion, liberacion, seguridad, CORS y Swagger fueron ejercitados contra MySQL real.
- Las unicas verificaciones pendientes son las relacionadas con Docker (daemon no disponible) y la migracion de nombres de rol en `autenticacion-service`.
