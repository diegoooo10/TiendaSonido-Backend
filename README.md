# Producto Service - Tienda Sonido Vivo

Microservicio unificado de **productos, categorias, precios, ofertas, inventario y reservas de stock** para Tienda Sonido Vivo.

Este servicio reemplaza la separacion anterior entre `producto-service` e `inventario-service`. Conserva la arquitectura por capas del proyecto original (Controller -> Service -> Repository), DTOs, mappers, validacion, Swagger y manejo centralizado de excepciones.

> **Alcance deliberado:** este microservicio NO implementa autenticacion, login, registro, usuarios, carrito, checkout, pedidos, API Gateway ni service discovery. El token JWT es emitido por `autenticacion-service`.

---

## 1. Stack

| Componente | Version / tecnologia |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Build | Maven Wrapper (`mvnw`) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL 8.x |
| Seguridad | Spring Security + JWT (JJWT 0.11.5) |
| Validacion | Jakarta Bean Validation |
| Documentacion | springdoc-openapi / Swagger UI |
| Utilidades | Lombok, Spring HATEOAS |

> Spring Boot 4 usa **Jackson 3** (`tools.jackson.databind.ObjectMapper`) para la serializacion de la aplicacion. El cliente JWT sigue usando Jackson 2 internamente a traves de `jjwt-jackson`.

---

## 2. Estructura del proyecto

```text
backendTiendaSonido/
|-- Dockerfile
|-- docker-compose.yml
|-- .env.example
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
|   |   |   |-- security/     # JWT, roles, CORS, handlers 401/403
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
| `JWT_SECRET` | **Si** | — | Secreto compartido con `autenticacion-service` (minimo 32 bytes) |
| `SERVER_PORT` | No | `8083` | Puerto HTTP (provisional) |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://localhost:5174,http://localhost:4173` | Origenes permitidos, separados por coma |
| `CATALOGO_CARGA_INICIAL` | No | `true` | Activa/desactiva la carga idempotente del catalogo |
| `CATALOGO_ARCHIVO` | No | `classpath:catalogo/productos.json` | Ubicacion del catalogo |
| `STOCK_CRITICO_INICIAL` | No | `3` | Umbral critico para productos nuevos (no es stock real) |
| `JPA_DDL_AUTO` | No | `update` | Estrategia Hibernate. Solo para desarrollo local |
| `JPA_SHOW_SQL` | No | `false` | Muestra SQL en consola |
| `JPA_FORMAT_SQL` | No | `true` | Formatea el SQL mostrado |

### Nota sobre `JWT_SECRET`

El valor debe ser **identico** al configurado en `autenticacion-service`. Si no coincide, todos los endpoints protegidos responden `401`. No se debe cambiar el contrato del token:

- `sub`: nombre de usuario.
- `rol`: nombre del rol como texto plano (por ejemplo `ADMIN` o `EMPLEADO`).
- Firma HMAC con el secreto compartido.

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

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/sonidovivo_productos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu_clave"
$env:JWT_SECRET="secreto-de-al-menos-32-bytes-compartido-con-autenticacion"
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

```powershell
Copy-Item .env.example .env
# Editar .env y definir MYSQL_ROOT_PASSWORD, DB_PASSWORD y JWT_SECRET
```

### 6.2 Levantar

```powershell
docker compose up --build -d
docker compose logs -f producto-service
```

Servicios:

- MySQL: `localhost:${MYSQL_PORT:-3306}`.
- API: `http://localhost:${APP_PORT:-8083}`.
- Datos persistentes en el volumen `mysql_data`.

Detener sin borrar datos:

```powershell
docker compose down
```

Detener y borrar la base:

```powershell
docker compose down -v
```

> El `Dockerfile` es multi-etapa: compila con JDK 21 y ejecuta con JRE 21 como usuario no root. La imagen oficial de MySQL no permite `MYSQL_USER=root`; use un usuario dedicado como `sonidovivo`.

> `db/init.sql` crea la base `sonidovivo_productos` con `utf8mb4`. Si cambia `MYSQL_DATABASE` en `.env`, la imagen de MySQL crea la base nueva automaticamente; el script de inicializacion solo afecta a la base con el nombre por defecto.

---

## 7. Swagger / OpenAPI

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

Para probar endpoints protegidos en Swagger:

1. Obtener un token de `autenticacion-service`.
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
| GET | `/api/productos/{codigo}` | Detalle por codigo | Publico |
| POST | `/api/productos` | Crea producto (stock inicial 0) | ADMINISTRADOR / VENDEDOR |
| PUT | `/api/productos/{codigo}` | Actualiza producto | ADMINISTRADOR / VENDEDOR |
| DELETE | `/api/productos/{codigo}` | Baja logica (`activo=false`) | ADMINISTRADOR / VENDEDOR |

### 9.2 Categorias (`/api/categorias`)

| Metodo | Ruta | Descripcion | Acceso |
|---|---|---|---|
| GET | `/api/categorias` | Lista categorias | Publico |
| GET | `/api/categorias/{id}` | Detalle de categoria | Publico |
| POST | `/api/categorias` | Crea categoria | ADMINISTRADOR |
| PUT | `/api/categorias/{id}` | Actualiza categoria | ADMINISTRADOR |
| DELETE | `/api/categorias/{id}` | Elimina solo si no tiene productos | ADMINISTRADOR |

### 9.3 Inventario (`/api/inventario`)

| Metodo | Ruta | Descripcion | Acceso |
|---|---|---|---|
| GET | `/api/inventario` | Inventario general | ADMINISTRADOR / VENDEDOR |
| GET | `/api/inventario/disponibilidad/{codigo}` | Disponibilidad publica de producto activo | Publico |
| GET | `/api/inventario/{codigo}` | Inventario por codigo | ADMINISTRADOR / VENDEDOR |
| PUT | `/api/inventario/{codigo}/stock` | Actualiza stock fisico y umbral critico | ADMINISTRADOR / VENDEDOR |
| POST | `/api/inventario/reservas` | Crea reserva idempotente | ADMINISTRADOR / VENDEDOR |
| GET | `/api/inventario/reservas/{referencia}` | Consulta reserva | ADMINISTRADOR / VENDEDOR |
| GET | `/api/inventario/reservas/pedido/{pedidoId}` | Reservas de un pedido | ADMINISTRADOR / VENDEDOR |
| POST | `/api/inventario/reservas/{referencia}/confirmar` | Confirma reserva y descuenta stock | ADMINISTRADOR / VENDEDOR |
| POST | `/api/inventario/reservas/{referencia}/liberar` | Libera reserva sin descontar stock | ADMINISTRADOR / VENDEDOR |

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

## 12. Roles y compatibilidad con autenticacion-service

Roles objetivo del proyecto:

| Rol | Permisos |
|---|---|
| `ADMINISTRADOR` | Acceso total: productos, categorias, inventario y reservas |
| `VENDEDOR` | Productos, inventario y reservas; no gestiona categorias |
| `CLIENTE` | Solo lectura del catalogo publico |

### Diferencia real detectada (importante)

`autenticacion-service` **todavia emite** los roles `ADMIN` y `EMPLEADO`, no `ADMINISTRADOR`, `VENDEDOR` ni `CLIENTE`.

Para no romper la integracion y no modificar el servicio de autenticacion, este microservicio acepta temporalmente ambos esquemas:

| Rol emitido hoy | Equivalencia aplicada |
|---|---|
| `ADMIN` | `ADMINISTRADOR` |
| `EMPLEADO` | `VENDEDOR` |

Esta compatibilidad esta centralizada en `security/Roles.java` y debe retirarse cuando `autenticacion-service` migre a los nombres objetivo. **No se invento un rol nuevo ni se cambio el contrato del token.**

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

`POST /api/productos` con `Authorization: Bearer <token>` y rol `ADMINISTRADOR` o `VENDEDOR`.

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
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Clase de prueba | Pruebas |
|---|---:|
| `CatalogoInitializerIntegrationTest` | 1 |
| `CatalogoResourceTest` | 1 |
| `ProductoServiceApplicationTests` | 1 |
| `SecurityIntegrationTest` | 13 |
| `CategoriaServiceTest` | 7 |
| `InventarioServiceTest` | 8 |
| `ProductoServiceTest` | 14 |
| `ReservaConcurrenteIntegrationTest` | 1 |
| `ReservaStockServiceTest` | 10 |
| **Total** | **56** |

Las pruebas automatizadas usan H2 en modo MySQL (`src/test/resources/application-test.properties`). Ademas se realizo una verificacion manual contra **MySQL 8.4 real**; el detalle y las evidencias estan en [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

---

## 15. Decisiones y limitaciones conocidas

1. **Sin historial de movimientos:** `MovimientoInventario` fue eliminado. Solo existe `ReservaStock` con estados `RESERVADA`, `CONFIRMADA` y `LIBERADA`.
2. **Puerto 8083 provisional:** se mantiene el puerto del `producto-service` original. El puerto 8084 del antiguo `inventario-service` queda retirado.
3. **Roles legacy:** `ADMIN` y `EMPLEADO` se aceptan temporalmente por compatibilidad. Requiere coordinacion con el equipo de autenticacion.
4. **`ddl-auto=update`:** adecuado para desarrollo local. Para produccion se recomienda migraciones versionadas (Flyway/Liquibase), no incluidas en este alcance.
5. **Sin gateway ni discovery:** el frontend consume este servicio directamente o a traves del gateway que defina el equipo de integracion.
6. **CORS:** configurado para Vite (`5173`, `5174`) y preview (`4173`). Ajustar `CORS_ALLOWED_ORIGINS` segun el entorno.
7. **Docker no ejecutado en esta maquina:** el daemon de Docker no estaba disponible durante la verificacion. El `Dockerfile` y `docker-compose.yml` se validaron sintacticamente, pero la imagen no se construyo localmente.

---

## 16. Seguridad

- No se almacenan secretos en el codigo ni en el repositorio.
- `JWT_SECRET` es obligatorio y se valida al iniciar (minimo 32 bytes).
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

# Docker
docker compose up --build -d
docker compose logs -f producto-service
docker compose down
```
