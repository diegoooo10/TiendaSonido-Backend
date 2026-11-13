# Producto Service - Tienda Sonido Vivo

Microservicio unificado de **productos, categorias, precios, ofertas, inventario y reservas de stock** para Tienda Sonido Vivo.

Este servicio reemplaza la separacion anterior entre `producto-service` e `inventario-service`. Conserva la arquitectura por capas del proyecto original (Controller -> Service -> Repository), DTOs, mappers, validacion, Swagger y manejo centralizado de excepciones.

> **Alcance deliberado:** este microservicio NO implementa autenticacion, login, registro, usuarios, carrito, checkout, pedidos, API Gateway ni service discovery. El token JWT es emitido por `usuario-service`.

---

## 1. Stack

| Componente | Version / tecnologia |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Build | Maven Wrapper (`mvnw`) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL 8.x |
| Seguridad | Spring Security OAuth2 Resource Server + Nimbus JWT (HS256) |
| Validacion | Jakarta Bean Validation |
| Documentacion | springdoc-openapi / Swagger UI |
| Utilidades | Lombok, Spring HATEOAS |

> Spring Boot 4 usa **Jackson 3** (`tools.jackson.databind.ObjectMapper`) para la serializacion de la aplicacion. La validacion JWT la realiza Nimbus (`NimbusJwtDecoder`); este servicio no genera tokens.

---

## 2. Estructura del proyecto

```text
backendTiendaSonido/
|-- Dockerfile
|-- docker-compose.yml
|-- .env.example              # Referencia local (sin secretos reales)
|-- .env.docker.example       # Referencia para Docker Compose
|-- .gitignore                # Excluye .env y .env.* reales
|-- db/
|   `-- init.sql
|-- pom.xml
|-- src/
|   |-- main/
|   |   |-- java/producto_service/
|   |   |   |-- config/       # Catalogo inicial, OpenAPI, propiedades
|   |   |   |-- controller/   # /api/productos, /api/categorias, /api/inventario
|   |   |   |-- dto/          # Request/Response DTOs
|   |   |   |-- exception/    # Excepciones de negocio y handler global
|   |   |   |-- mapper/       # Entidad <-> DTO
|   |   |   |-- model/        # Producto, Categoria, Inventario, ReservaStock
|   |   |   |-- repository/   # Repositorios JPA
|   |   |   |-- security/     # Nimbus JWT, roles, CORS, handlers 401/403
|   |   |   `-- service/      # Reglas de negocio y transacciones
|   |   `-- resources/
|   |       |-- application.properties
|   |       `-- catalogo/productos.json   # Catalogo oficial (51 productos)
|   `-- test/                 # Pruebas unitarias e integracion
`-- docs/
    `-- VERIFICACION.md       # Evidencia de pruebas y verificacion MySQL
```

---

## 3. Requisitos

- JDK 21.
- MySQL 8.x local (Laragon, XAMPP, MySQL Installer o contenedor) **o** Docker con Compose.
- No es necesario instalar Maven: se incluye `mvnw` / `mvnw.cmd`.

Comprobar Java:

```powershell
java -version
```

---

## 4. Configuracion

Toda la configuracion sensible se toma de variables de entorno. **No hay secretos reales en el repositorio.**

| Variable | Obligatoria | Valor por defecto | Descripcion |
|---|---:|---|---|
| `DB_URL` | No | `jdbc:mysql://localhost:3306/sonidovivo_productos?...` | URL JDBC de MySQL |
| `DB_USERNAME` | No | `root` | Usuario de base de datos |
| `DB_PASSWORD` | No | vacio | Clave de base de datos |
| `DB_POOL_SIZE` | No | `10` | Tamano maximo del pool Hikari |
| `DB_POOL_MIN_IDLE` | No | `2` | Conexiones minimas inactivas |
| `JWT_SECRET` | **Si** | — | Secreto HS256 compartido con `usuario-service` (minimo 32 bytes) |
| `SERVER_PORT` | No | `8083` | Puerto HTTP (provisional) |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://localhost:5174,http://localhost:4173` | Origenes permitidos, separados por coma |
| `CATALOGO_CARGA_INICIAL` | No | `true` | Activa/desactiva la carga idempotente del catalogo |
| `CATALOGO_ARCHIVO` | No | `classpath:catalogo/productos.json` | Ubicacion del catalogo |
| `STOCK_CRITICO_INICIAL` | No | `3` | Umbral critico para productos nuevos (no es stock real) |
| `JPA_DDL_AUTO` | No | `update` | Estrategia Hibernate. Solo para desarrollo local |
| `JPA_SHOW_SQL` | No | `false` | Muestra SQL en consola |
| `JPA_FORMAT_SQL` | No | `true` | Formatea el SQL mostrado |

### Nota sobre `JWT_SECRET`

El valor debe ser **identico** al configurado en `usuario-service`. Si no coincide, todos los endpoints protegidos responden `401`.

Spring Boot **no lee automaticamente el archivo `.env`**. Para ejecucion local, exporte `JWT_SECRET` en la terminal o configúrelo en el IDE. El `.env` es solo una referencia y esta excluido de Git.

Contrato del token emitido por `usuario-service`:

- Algoritmo: **HS256**.
- `sub`: correo del usuario.
- `rol`: nombre del rol como texto plano: `ADMIN`, `EMPLEADO` o `CLIENTE`.
- `iat` / `exp`: emision y expiracion; la expiracion se valida.
- Autoridades internas: prefijo `ROLE_` (`ROLE_ADMIN`, `ROLE_EMPLEADO`, `ROLE_CLIENTE`).

Este servicio solo valida tokens; no genera tokens ni implementa login/registro.

---

## 5. Ejecucion local

### 5.1 Crear la base de datos

Con MySQL en ejecucion:

```powershell
mysql -u root -p < db/init.sql
```

O manualmente:

```sql
CREATE DATABASE IF NOT EXISTS sonidovivo_productos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Las tablas las crea Hibernate al iniciar (`ddl-auto=update`). El script no crea tablas ni inserta datos.

### 5.2 Variables de entorno en PowerShell

> Recuerde: `mvnw spring-boot:run` **no lee `.env`**. Las variables deben existir en la sesion de la terminal.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/sonidovivo_productos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu_clave"
$env:JWT_SECRET="secreto-de-al-menos-32-bytes-identico-al-de-usuario-service"
$env:SERVER_PORT="8083"
```

### 5.3 Ejecutar

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Compilar y ejecutar el JAR:

```powershell
.\mvnw.cmd clean package
java -jar target\producto-service-0.0.1-SNAPSHOT.jar
```

La API queda disponible en `http://localhost:8083`.

---

## 6. Docker

### 6.1 Preparar variables

La configuracion de Docker esta separada de la local:

```powershell
Copy-Item .env.docker.example .env.docker
# Editar .env.docker y definir MYSQL_ROOT_PASSWORD, DB_PASSWORD y JWT_SECRET
```

`.env` (local) y `.env.docker` (Docker) estan excluidos de Git. Solo los archivos `*.example` se versionan.

### 6.2 Levantar

```powershell
docker compose --env-file .env.docker up --build -d
docker compose --env-file .env.docker logs -f producto-service
```

Servicios:

- MySQL: `localhost:${MYSQL_PORT:-3306}`.
- API: `http://localhost:${APP_PORT:-8083}`.
- Datos persistentes en el volumen `mysql_data`.

Detener sin borrar datos:

```powershell
docker compose --env-file .env.docker down
```

Detener y borrar la base (operacion destructiva, solo para entornos descartables):

```powershell
docker compose --env-file .env.docker down -v
```

> El `Dockerfile` es multi-etapa: compila con JDK 21 y ejecuta con JRE 21 como usuario no root. La imagen oficial de MySQL no permite `MYSQL_USER=root`; use un usuario dedicado como `sonidovivo`.

> `db/init.sql` crea la base `sonidovivo_productos` con `utf8mb4`. Si cambia `MYSQL_DATABASE` en `.env.docker`, la imagen de MySQL crea la base nueva automaticamente; el script de inicializacion solo afecta a la base con el nombre por defecto.

---

## 7. Swagger / OpenAPI

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

Para probar endpoints protegidos en Swagger:

1. Obtener un token de `usuario-service`.
2. Pulsar **Authorize**.
3. Pegar el token (sin la palabra `Bearer`).

---

## 8. Catalogo oficial

El archivo `src/main/resources/catalogo/productos.json` fue generado a partir del catalogo real del frontend (`productos.js`). Contiene:

- **51 productos**
- **10 categorias**
- **51 codigos unicos**
- **365 unidades de stock fisico total**
- **Sin ofertas precargadas** (`precioOferta = null`, `enOferta = false`)

La carga es **idempotente**:

- Los productos se identifican por `codigo` (normalizado a mayusculas).
- Si el producto ya existe, no se sobrescriben nombre, precio ni stock.
- Si falta el inventario, se crea con el stock exacto del catalogo.
- Reiniciar la aplicacion no duplica productos, categorias ni inventarios.

Los productos creados por API nacen con `stockFisico = 0`; el stock real solo se modifica por el endpoint de inventario o por la carga inicial del catalogo.

---

## 9. Endpoints

Base URL local: `http://localhost:8083`

### 9.1 Productos (`/api/productos`)

| Metodo | Ruta | Descripcion | Acceso |
|---|---|---|---|
| GET | `/api/productos` | Lista productos activos. Filtros: `q`, `categoriaId`, `marca`, `enOferta` | Publico |
| GET | `/api/productos/paginado` | Lista paginada. Parametros: `page`, `size` (max 100) | Publico |
| GET | `/api/productos/ofertas` | Productos activos con `enOferta=true` | Publico |
| GET | `/api/productos/categoria/{categoriaId}` | Productos activos de una categoria | Publico |
| GET | `/api/productos/{codigo}` | Detalle por codigo (solo activos) | Publico |
| GET | `/api/productos/admin` | Lista productos activos e inactivos | ADMIN |
| GET | `/api/productos/admin/paginado` | Lista paginada de activos e inactivos (`page`, `size`) | ADMIN |
| GET | `/api/productos/admin/{codigo}` | Detalle incluyendo productos inactivos | ADMIN |
| POST | `/api/productos` | Crea producto (stock inicial 0) | ADMIN |
| PUT | `/api/productos/{codigo}` | Actualiza o reactiva producto | ADMIN |
| DELETE | `/api/productos/{codigo}` | Baja logica (`activo=false`) | ADMIN |

### 9.2 Categorias (`/api/categorias`)

| Metodo | Ruta | Descripcion | Acceso |
|---|---|---|---|
| GET | `/api/categorias` | Lista solo categorias activas | Publico |
| GET | `/api/categorias/admin` | Lista categorias activas e inactivas | ADMIN |
| GET | `/api/categorias/{id}` | Detalle de categoria | Publico |
| POST | `/api/categorias` | Crea categoria | ADMIN |
| PUT | `/api/categorias/{id}` | Actualiza categoria. Desactivar con productos activos devuelve `409` | ADMIN |
| DELETE | `/api/categorias/{id}` | Elimina solo si no tiene productos | ADMIN |

### 9.3 Inventario (`/api/inventario`)

| Metodo | Ruta | Descripcion | Acceso |
|---|---|---|---|
| GET | `/api/inventario` | Inventario general | ADMIN / EMPLEADO |
| GET | `/api/inventario/disponibilidad/{codigo}` | Disponibilidad publica de producto activo | Publico |
| GET | `/api/inventario/{codigo}` | Inventario por codigo | ADMIN / EMPLEADO |
| PUT | `/api/inventario/{codigo}/stock` | Actualiza stock fisico y umbral critico | ADMIN / EMPLEADO |
| POST | `/api/inventario/reservas` | Crea reserva idempotente | ADMIN / EMPLEADO |
| GET | `/api/inventario/reservas/{referencia}` | Consulta reserva | ADMIN / EMPLEADO |
| GET | `/api/inventario/reservas/pedido/{pedidoId}` | Reservas de un pedido | ADMIN / EMPLEADO |
| POST | `/api/inventario/reservas/{referencia}/confirmar` | Confirma reserva y descuenta stock | ADMIN / EMPLEADO |
| POST | `/api/inventario/reservas/{referencia}/liberar` | Libera reserva sin descontar stock | ADMIN / EMPLEADO |

---

## 10. Reglas de precio

El backend es la fuente de verdad del precio efectivo.

- `precio` es obligatorio y debe ser mayor a 0, con maximo 2 decimales.
- `precioOferta` es opcional.
- Si `enOferta=true`, `precioOferta` es obligatorio.
- `precioOferta` debe ser **menor** que `precio`.
- `precioEfectivo` se calcula asi:
  - `precioOferta` si `enOferta=true` y `precioOferta` no es nulo.
  - `precio` en cualquier otro caso.
- Si `enOferta=false`, la oferta no se aplica aunque `precioOferta` tenga valor.
- El frontend **no debe calcular** el precio final: debe usar `precioEfectivo`.

---

## 11. Inventario y reservas

### Campos

| Campo | Significado |
|---|---|
| `stockFisico` | Unidades fisicas en bodega |
| `stockReservado` | Unidades apartadas por reservas activas |
| `stockDisponible` | `stockFisico - stockReservado` (nunca negativo) |
| `stockCritico` | Umbral para alertas de bajo stock |
| `fechaActualizacion` | Ultima modificacion del inventario |

### Operaciones

1. **Reservar** (`POST /api/inventario/reservas`)
   - Verifica producto activo e inventario.
   - Bloquea el inventario con `PESSIMISTIC_WRITE` y usa `@Version` para control de concurrencia.
   - Rechaza con `409` si `cantidad > stockDisponible`.
   - Incrementa `stockReservado`.

2. **Confirmar** (`POST /api/inventario/reservas/{referencia}/confirmar`)
   - Descuenta `cantidad` de `stockFisico` y de `stockReservado`.
   - Idempotente si la reserva ya estaba `CONFIRMADA`.
   - Rechaza con `409` si la reserva estaba `LIBERADA`.

3. **Liberar** (`POST /api/inventario/reservas/{referencia}/liberar`)
   - Descuenta `cantidad` de `stockReservado` sin tocar `stockFisico`.
   - Idempotente si la reserva ya estaba `LIBERADA`.
   - Rechaza con `409` si la reserva estaba `CONFIRMADA`.

### Idempotencia de reservas

`referencia` es la clave idempotente:

- Misma referencia + mismo `pedidoId` + mismo `codigoProducto` + misma `cantidad` -> devuelve la reserva existente con `200`.
- Misma referencia + datos distintos -> `409 Conflict`.
- Reserva nueva -> `201 Created`.

`ReservaStock` representa **solo el estado actual de una reserva**, no un historial de movimientos. La funcionalidad de `MovimientoInventario` fue eliminada por completo.

---

## 12. Roles y permisos

Los unicos roles reales son los emitidos por `usuario-service` en el claim `rol`: `ADMIN`, `EMPLEADO` y `CLIENTE`.

| Rol | Permisos |
|---|---|
| `ADMIN` | Acceso total: consultar/crear/actualizar/desactivar/reactivar productos; consultar activos e inactivos; administrar categorias activas e inactivas; administrar inventario y reservas |
| `EMPLEADO` | Consultar catalogo, categorias y disponibilidad; consultar inventario; actualizar stock; crear, consultar, confirmar y liberar reservas. No puede crear, actualizar ni eliminar productos ni administrar categorias |
| `CLIENTE` | Solo lectura del catalogo publico: productos, categorias, ofertas y disponibilidad. No modifica productos, categorias ni inventario |

Los endpoints `/api/productos/admin/**` y `/api/categorias/admin/**` son exclusivos de `ADMIN`; `EMPLEADO` y `CLIENTE` reciben `403`.

La configuracion esta centralizada en `security/Roles.java` y `security/SecurityConfig.java`. No se aceptan roles inexistentes (`ADMINISTRADOR`, `VENDEDOR`) ni se generan tokens en este servicio.

---

## 13. Ejemplos JSON

### 13.1 Producto (respuesta)

`GET /api/productos/GA001`

```json
{
  "id": 1,
  "codigo": "GA001",
  "nombre": "Guitarra Acústica Folk",
  "descripcion": "Tapa de abeto, aros y fondo de meranti. Ideal para iniciantes.",
  "marca": "Yamaha",
  "modelo": "F310",
  "imagen": "img/guitarra.jpg",
  "precio": 129990.00,
  "precioOferta": null,
  "enOferta": false,
  "precioEfectivo": 129990.00,
  "activo": true,
  "categoriaId": 1,
  "categoriaNombre": "Guitarras Acústicas",
  "stockDisponible": 8,
  "stock": 8,
  "disponible": true,
  "_links": {
    "self": { "href": "http://localhost:8083/api/productos/GA001" },
    "categoria": { "href": "http://localhost:8083/api/categorias/1" },
    "disponibilidad": { "href": "http://localhost:8083/api/inventario/disponibilidad/GA001" }
  }
}
```

> `stock` es un alias de compatibilidad para el frontend; equivale a `stockDisponible`.

### 13.2 Crear producto

`POST /api/productos` con `Authorization: Bearer <token>` y rol `ADMIN`.

```json
{
  "codigo": "GA001",
  "nombre": "Guitarra Acústica Folk",
  "descripcion": "Tapa de abeto, aros y fondo de meranti.",
  "marca": "Yamaha",
  "modelo": "F310",
  "imagen": "img/guitarra.jpg",
  "precio": 129990.00,
  "precioOferta": 109990.00,
  "enOferta": true,
  "activo": true,
  "categoriaId": 1
}
```

Respuesta `201 Created`: mismo formato que el detalle, con `precioEfectivo = 109990.00` y `stockDisponible = 0`.

### 13.3 Actualizar stock

`PUT /api/inventario/GA001/stock`

```json
{
  "stockFisico": 20,
  "stockCritico": 3
}
```

Respuesta `200 OK`:

```json
{
  "id": 1,
  "productoId": 1,
  "codigoProducto": "GA001",
  "nombreProducto": "Guitarra Acústica Folk",
  "stockFisico": 20,
  "stockReservado": 0,
  "stockDisponible": 20,
  "stockCritico": 3,
  "bajoStockCritico": false,
  "fechaActualizacion": "2026-11-10T15:30:00"
}
```

### 13.4 Reservar stock

`POST /api/inventario/reservas`

```json
{
  "pedidoId": "PED-2026-0001",
  "codigoProducto": "GA001",
  "cantidad": 2,
  "referencia": "PED-2026-0001-GA001"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "referencia": "PED-2026-0001-GA001",
  "pedidoId": "PED-2026-0001",
  "codigoProducto": "GA001",
  "cantidad": 2,
  "estado": "RESERVADA",
  "stockDisponible": 18,
  "fechaCreacion": "2026-11-10T15:30:00",
  "fechaActualizacion": "2026-11-10T15:30:00"
}
```

Reintentar exactamente la misma solicitud devuelve `200 OK` sin modificar el stock.

Reutilizar la referencia con otros datos:

```json
{
  "timestamp": "2026-11-10T15:31:00",
  "codigo": 409,
  "error": "Conflicto",
  "mensaje": "La referencia ya fue utilizada con otros datos de reserva",
  "path": "/api/inventario/reservas"
}
```

### 13.5 Confirmar reserva

`POST /api/inventario/reservas/PED-2026-0001-GA001/confirmar`

```json
{
  "id": 1,
  "referencia": "PED-2026-0001-GA001",
  "pedidoId": "PED-2026-0001",
  "codigoProducto": "GA001",
  "cantidad": 2,
  "estado": "CONFIRMADA",
  "stockDisponible": 18,
  "fechaCreacion": "2026-11-10T15:30:00",
  "fechaActualizacion": "2026-11-10T15:32:00"
}
```

### 13.6 Error de validacion

```json
{
  "timestamp": "2026-11-10T15:33:00",
  "codigo": 400,
  "error": "Datos invalidos",
  "mensaje": "Revise los campos enviados",
  "path": "/api/productos",
  "errores": [
    "precio: El precio debe ser mayor a 0",
    "categoriaId: Debe asignar una categoria"
  ]
}
```

### 13.7 No autorizado / sin permisos

`401`:

```json
{
  "timestamp": "2026-11-10T15:34:00",
  "codigo": 401,
  "error": "No autorizado",
  "mensaje": "Debe enviar un token JWT valido en el header Authorization",
  "path": "/api/inventario"
}
```

`403`:

```json
{
  "timestamp": "2026-11-10T15:34:00",
  "codigo": 403,
  "error": "Acceso denegado",
  "mensaje": "El rol del token no tiene permisos para esta operacion",
  "path": "/api/productos"
}
```

---

## 14. Pruebas

Ejecutar toda la suite:

```powershell
.\mvnw.cmd test
```

Resultado verificado:

```text
Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Clase de prueba | Pruebas |
|---|---:|
| `CatalogoInitializerIntegrationTest` | 1 |
| `CatalogoResourceTest` | 1 |
| `ProductoServiceApplicationTests` | 1 |
| `SecurityIntegrationTest` | 23 |
| `CategoriaServiceTest` | 10 |
| `InventarioServiceTest` | 8 |
| `ProductoServiceTest` | 17 |
| `ReservaConcurrenteIntegrationTest` | 3 |
| `ReservaStockServiceTest` | 10 |
| **Total** | **74** |

Las pruebas automatizadas usan H2 en modo MySQL (`src/test/resources/application-test.properties`). En una iteracion anterior se realizo una verificacion manual contra **MySQL 8.4 real**; el detalle, las evidencias y las limitaciones actuales estan en [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

---

## 15. Decisiones y limitaciones conocidas

1. **Sin historial de movimientos:** `MovimientoInventario` fue eliminado. Solo existe `ReservaStock` con estados `RESERVADA`, `CONFIRMADA` y `LIBERADA`.
2. **Puerto 8083 provisional:** se mantiene el puerto del `producto-service` original. El puerto 8084 del antiguo `inventario-service` queda retirado.
3. **Roles:** solo se aceptan `ADMIN`, `EMPLEADO` y `CLIENTE`, los roles reales de `usuario-service`. Se eliminaron los roles inexistentes `ADMINISTRADOR` y `VENDEDOR`.
4. **Baja logica de productos:** `DELETE` marca `activo=false` y conserva el historico. `ADMIN` puede consultar y reactivar el producto.
5. **Categorias:** el listado publico solo devuelve activas; `ADMIN` consulta todas en `/api/categorias/admin`. Desactivar una categoria con productos activos devuelve `409 Conflict`.
6. **Reservas concurrentes:** la referencia tiene restriccion unica. La reserva bloquea el inventario y relee la referencia con bloqueo pesimista, por lo que dos reintentos simultaneos de la misma referencia y producto son idempotentes. Si la misma referencia se usa a la vez con productos distintos, solo una reserva se crea y la otra recibe `409`.
7. **`ddl-auto=update`:** adecuado para desarrollo local. Para produccion se recomienda migraciones versionadas (Flyway/Liquibase), no incluidas en este alcance.
8. **Sin gateway ni discovery:** el frontend consume este servicio directamente o a traves del gateway que defina el equipo de integracion.
9. **CORS:** configurado para Vite (`5173`, `5174`) y preview (`4173`). Se permiten `Cache-Control` y `Pragma` en preflight. Ajustar `CORS_ALLOWED_ORIGINS` segun el entorno.
10. **Docker no ejecutado en esta maquina:** el daemon de Docker no estaba disponible durante la verificacion. El `Dockerfile`, `docker-compose.yml` y `.env.docker.example` se revisaron, pero la imagen no se construyo localmente.

---

## 16. Seguridad

- No se almacenan secretos reales en el codigo ni en el repositorio.
- `JWT_SECRET` es obligatorio y se valida al iniciar (minimo 32 bytes).
- La validacion JWT usa `NimbusJwtDecoder` con HS256, claim `rol`, subject = correo y validacion de expiracion.
- Los endpoints de escritura requieren JWT valido y rol autorizado.
- CSRF deshabilitado por ser API stateless.
- Sesiones stateless; no se usa `HttpSession`.
- Respuestas `401` y `403` en JSON, sin exponer detalles internos.
- Errores internos devuelven `500` con mensaje generico y se registran en el log del servidor.

---

## 17. Comandos utiles

```powershell
# Compilar sin tests
.\mvnw.cmd -DskipTests compile

# Ejecutar tests
.\mvnw.cmd test

# Empaquetar
.\mvnw.cmd clean package

# Ejecutar JAR
java -jar target\producto-service-0.0.1-SNAPSHOT.jar

# Docker (configuracion separada)
docker compose --env-file .env.docker up --build -d
docker compose --env-file .env.docker logs -f producto-service
docker compose --env-file .env.docker down
```
