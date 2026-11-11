# API Gateway - Tienda Sonido Vivo

Gateway reactivo (Spring Cloud Gateway + WebFlux) que actúa como punto de entrada
único para el frontend de **Tienda Sonido Vivo**. Enruta las solicitudes HTTP hacia
el microservicio de Productos sin modificar los paths originales.

- Puerto del Gateway: **8080**
- Destino actual: **producto-service** en `http://localhost:8083`
- No tiene base de datos propia.
- No implementa autenticación propia: reenvía el header `Authorization` tal cual.

---

## 1. ¿Qué es y para qué sirve?

Un API Gateway es el único punto de exposición HTTP del sistema. El frontend React
habla con `http://localhost:8080` y el Gateway decide a qué microservicio enviar
cada solicitud.

Ventajas en Tienda Sonido Vivo:

- Una sola URL pública para el frontend.
- Las rutas internas (`/api/productos`, `/api/categorias`, `/api/inventario`)
  quedan centralizadas.
- CORS se resuelve en un único lugar.
- Facilita incorporar después los microservicios de Pedidos y Usuarios sin cambiar
  el frontend.
- La seguridad seguirá delegada a cada microservicio (o se integrará en el Gateway
  cuando exista el contrato definitivo de Diego).

---

## 2. Arquitectura general

```text
Frontend React/Vite
  http://localhost:5173 | 5174 | 4173
              |
              |  /api/productos/**      /api/categorias/**      /api/inventario/**
              v
+---------------------------------------------------------------+
|  API Gateway (gatewayTiendaSonido)                            |
|  Spring Boot 4.0.6 + Spring Cloud Gateway WebFlux             |
|  http://localhost:8080                                        |
|  - CORS centralizado                                          |
|  - Reenvío de headers (Authorization incluido)                |
|  - Actuator: /actuator/health                                 |
+---------------------------------------------------------------+
              |
              |  rutas estáticas (sin StripPrefix ni RewritePath)
              v
+---------------------------------------------------------------+
|  producto-service                                             |
|  Spring Boot 4.0.6 + MySQL (Laragon)                          |
|  http://localhost:8083                                        |
|  - Productos, categorías, precios e inventario                |
|  - Swagger: http://localhost:8083/swagger-ui.html             |
+---------------------------------------------------------------+

Futuro (aún no implementado):
  /api/pedidos/**  -> microservicio de Pedidos  (Ignacio)
  /api/usuarios/** -> microservicio de Usuarios (Diego)
```

El Gateway **no** usa Eureka, Service Discovery ni balanceo de carga. Las rutas son
estáticas y están definidas en `application.properties`.

---

## 3. Tecnologías utilizadas

| Componente | Versión |
|---|---|
| Java (compilación) | 21 |
| Maven Wrapper | 3.3.4 / Maven 3.9.14 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.2 (Oakwood) |
| Spring Cloud Gateway Server WebFlux | 5.0.2 |
| Spring Boot Actuator | 4.0.6 |
| Servidor | Netty (reactivo) |

La combinación Spring Boot 4.0.x + Spring Cloud 2025.1.x es la documentada como
compatible por Spring Cloud. No se incluyeron Spring MVC, JPA, Hibernate ni MySQL.

Dependencias declaradas en `pom.xml`:

- `spring-cloud-starter-gateway-server-webflux`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test` (solo tests)

---

## 4. Estructura del proyecto

```text
gatewayTiendaSonido/
├── src/
│   ├── main/
│   │   ├── java/com/sonidovivo/gateway/
│   │   │   ├── GatewayApplication.java
│   │   │   └── config/CorsConfig.java
│   │   └── resources/application.properties
│   └── test/java/com/sonidovivo/gateway/
│       ├── GatewayApplicationTests.java
│       ├── RouteConfigurationTest.java
│       └── GatewayRoutingIntegrationTest.java
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 5. Requisitos previos

- JDK 21 o superior (`java -version`). El proyecto compila con `--release 21`.
- El microservicio de Productos debe estar levantado en el puerto 8083 para que el
  enrutamiento real funcione.
- No se necesita base de datos para el Gateway.

Si el microservicio de Productos no está levantado, el Gateway igualmente arranca;
las llamadas a `/api/**` devolverán un error de conexión al destino (por ejemplo
`503`) y `/actuator/health` seguirá respondiendo `UP`. Es importante distinguir:

- **Error del Gateway**: no arranca, no enruta, rechaza CORS, falla el puerto 8080.
- **Error de conexión al destino**: el Gateway responde `503` porque
  `localhost:8083` no está disponible.

---

## 6. Cómo ejecutar el proyecto desde VS Code

1. Abrir la carpeta `gatewayTiendaSonido` en VS Code.
2. Verificar que VS Code use un JDK 21+ (por ejemplo con la extensión
   *Extension Pack for Java*).
3. Abrir una terminal integrada en la raíz del proyecto.
4. Ejecutar el wrapper de Maven.

En Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux/macOS:

```bash
./mvnw spring-boot:run
```

También se puede ejecutar el JAR empaquetado:

```powershell
.\mvnw.cmd clean package
java -jar target/gateway-tienda-sonido-0.0.1-SNAPSHOT.jar
```

El Gateway queda escuchando en:

```text
http://localhost:8080
```

Para detenerlo: `Ctrl + C`.

---

## 7. Rutas configuradas

Todas las rutas conservan el path original. No se usan `StripPrefix`,
`RewritePath` ni `SetPath`.

| ID | Ruta pública | Destino | Ejemplo |
|---|---|---|---|
| `productos` | `/api/productos/**` | `http://localhost:8083` | `/api/productos/MI001` |
| `categorias` | `/api/categorias/**` | `http://localhost:8083` | `/api/categorias` |
| `inventario` | `/api/inventario/**` | `http://localhost:8083` | `/api/inventario/disponibilidad/MI001` |

Configuración real (`src/main/resources/application.properties`):

```properties
spring.cloud.gateway.server.webflux.routes[0].id=productos
spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:8083
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/productos/**

spring.cloud.gateway.server.webflux.routes[1].id=categorias
spring.cloud.gateway.server.webflux.routes[1].uri=http://localhost:8083
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/categorias/**

spring.cloud.gateway.server.webflux.routes[2].id=inventario
spring.cloud.gateway.server.webflux.routes[2].uri=http://localhost:8083
spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/api/inventario/**
```

> Nota: en Spring Cloud Gateway 5.x el prefijo correcto es
> `spring.cloud.gateway.server.webflux.*`. El prefijo antiguo
> `spring.cloud.gateway.routes` corresponde a la librería tradicional y no aplica
> a este starter.

---

## 8. Cómo probar las rutas

### 8.1 Con Swagger del microservicio

El Swagger del microservicio sigue disponible directamente en:

```text
http://localhost:8083/swagger-ui.html
```

El Gateway solo expone las rutas `/api/**`, por lo que la UI de Swagger no se
enruta a través del puerto 8080. Para probar a través del Gateway:

1. Abrir Swagger en `http://localhost:8083/swagger-ui.html` para conocer los
   endpoints y payloads reales.
2. Repetir la llamada contra `http://localhost:8080` con el mismo path.

### 8.2 Con Postman

Crear una colección con `baseUrl = http://localhost:8080`:

| Método | URL |
|---|---|
| GET | `{{baseUrl}}/api/productos` |
| GET | `{{baseUrl}}/api/productos/MI001` |
| GET | `{{baseUrl}}/api/categorias` |
| GET | `{{baseUrl}}/api/inventario/disponibilidad/MI001` |

Para probar el reenvío de seguridad, agregar el header:

```text
Authorization: Bearer <token>
```

El Gateway no valida ni modifica ese token: lo entrega al microservicio destino.

### 8.3 Con curl

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/productos
curl -i http://localhost:8080/api/productos/MI001
curl -i http://localhost:8080/api/categorias
curl -i http://localhost:8080/api/inventario/disponibilidad/MI001
curl -i -H "Authorization: Bearer token-de-prueba" http://localhost:8080/api/productos
```

---

## 9. CORS

El Gateway configura CORS de forma explícita en `CorsConfig.java`:

- Orígenes permitidos:
  - `http://localhost:5173`
  - `http://localhost:5174`
  - `http://localhost:4173`
- Métodos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- Headers permitidos: `Authorization`, `Content-Type`, `Accept`, `Origin`,
  `X-Requested-With`, `Cache-Control`, `Pragma`
- `allowCredentials = false` (el frontend usa `Bearer` token, no cookies).
- `maxAge = 3600`

Los orígenes se pueden sobrescribir sin recompilar:

```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:5174,http://localhost:4173"
.\mvnw.cmd spring-boot:run
```

### Coordinación con el CORS del microservicio de Productos

El microservicio también puede agregar cabeceras CORS. Para evitar cabeceras
duplicadas (`Access-Control-Allow-Origin` repetido), el Gateway aplica:

```properties
spring.cloud.gateway.server.webflux.default-filters[0]=DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_FIRST
```

Recomendaciones:

1. Mantener una sola política CORS como fuente de verdad. Se recomienda que el
   Gateway sea el punto CORS del frontend.
2. Si el backend sigue agregando CORS, el filtro de deduplicación evita valores
   duplicados en la respuesta.
3. No usar `Access-Control-Allow-Origin: *` junto con credenciales. En este
   proyecto no se habilitan credenciales y los orígenes son explícitos.
4. Si en el futuro el frontend usa cookies, cambiar `allowCredentials` a `true`
   **solo** manteniendo orígenes explícitos y coordinando la política con el
   microservicio.

---

## 10. Monitorización (Actuator)

Endpoint disponible:

```text
http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Solo se expone `health`:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
management.endpoint.health.probes.enabled=true
```

No se exponen `env`, `beans`, `heapdump` ni otra información sensible.

---

## 11. Seguridad JWT (estado actual y futuro)

### Estado actual

- No hay login, usuarios, contraseñas ni secretos JWT en el Gateway.
- No se valida ni se firma ningún token.
- El header `Authorization` y los demás headers se reenvían sin modificar.
- La autorización sigue siendo responsabilidad de cada microservicio.

### Cuando Diego entregue el microservicio de Usuarios/Autenticación

La integración deberá hacerse cuando exista el contrato real (issuer, algoritmo,
JWKS o secreto compartido, claims y roles). Pasos recomendados:

1. Confirmar el contrato JWT: emisor, audiencia, expiración, claims y roles.
2. Decidir dónde se valida:
   - **Opción A (recomendada para el Gateway)**: validar el JWT en el Gateway con
     `spring-boot-starter-oauth2-resource-server`, usando el `issuer-uri` o
     `jwk-set-uri` que entregue Diego.
   - **Opción B**: dejar la validación en cada microservicio y limitar el Gateway
     al enrutamiento.
3. Si se valida en el Gateway, agregar la dependencia de seguridad reactiva y una
   `SecurityWebFilterChain` con `oauth2ResourceServer().jwt()`.
4. Configurar las rutas públicas (por ejemplo login/registro) y proteger el resto.
5. No reutilizar secretos de ejemplo ni hardcodear claves.
6. Probar tokens válidos, expirados, con firma incorrecta y sin permisos.

> Importante: el Gateway no debe inventar un contrato JWT incompatible con el
> servicio de Diego. Hasta que ese contrato exista, el reenvío transparente del
> header `Authorization` es el comportamiento correcto.

---

## 12. Cómo agregar Pedidos y Usuarios en el futuro

Cuando los microservicios estén disponibles y sus puertos/URLs sean confirmados
por sus responsables:

1. Agregar una nueva ruta en `application.properties` conservando el path:

```properties
# EJEMPLO: <PUERTO_PEDIDOS> debe ser el puerto real confirmado por Ignacio.
spring.cloud.gateway.server.webflux.routes[3].id=pedidos
spring.cloud.gateway.server.webflux.routes[3].uri=http://localhost:<PUERTO_PEDIDOS>
spring.cloud.gateway.server.webflux.routes[3].predicates[0]=Path=/api/pedidos/**
```

```properties
# EJEMPLO: <PUERTO_USUARIOS> debe ser el puerto real confirmado por Diego.
spring.cloud.gateway.server.webflux.routes[4].id=usuarios
spring.cloud.gateway.server.webflux.routes[4].uri=http://localhost:<PUERTO_USUARIOS>
spring.cloud.gateway.server.webflux.routes[4].predicates[0]=Path=/api/usuarios/**
```

2. No agregar `StripPrefix` si el microservicio espera el path completo.
3. Actualizar `RouteConfigurationTest` con las nuevas rutas.
4. Actualizar este README con las URLs definitivas.
5. No implementar Eureka ni discovery hasta que el equipo lo decida.

Los puertos anteriores son **marcadores de posición**: no se inventaron valores
reales porque esos servicios todavía no existen.

---

## 13. Pruebas ejecutadas

### 13.1 Compilación y tests Maven

Comando:

```powershell
.\mvnw.cmd clean package
```

Resultado real:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
JAR: target/gateway-tienda-sonido-0.0.1-SNAPSHOT.jar
```

Detalle por clase:

| Clase | Tests | Resultado |
|---|---|---|
| `GatewayApplicationTests` | 2 | OK |
| `RouteConfigurationTest` | 2 | OK |
| `GatewayRoutingIntegrationTest` | 7 | OK |

Cobertura de las pruebas:

- El contexto de Spring arranca correctamente.
- No existen beans `dataSource` ni `entityManagerFactory` (sin base de datos).
- Las tres rutas existen con IDs y predicados `Path` correctos.
- Ninguna ruta usa `StripPrefix`, `RewritePath` o `SetPath`.
- Con un servidor HTTP temporal como destino:
  - `/api/productos/MI001?incluir=ofertas` conserva path y query.
  - `/api/categorias` conserva path.
  - `/api/inventario/disponibilidad/MI001` conserva path.
  - `POST` conserva body y `Authorization`.
  - El preflight CORS se resuelve en el Gateway y no llega al destino.
  - Una respuesta real no duplica `Access-Control-Allow-Origin`.
  - `/actuator/health` responde `UP`.

### 13.2 Verificación de ejecución real

Se arrancó el JAR empaquetado en el puerto 8080 y un stub HTTP temporal en el
puerto 8083 (el microservicio real de Productos no estaba levantado durante la
verificación). El stub registró las solicitudes recibidas.

Evidencia:

```text
Netty started on port 8080 (http)
Started GatewayApplication in 3.841 seconds
Exposing 1 endpoint beneath base path '/actuator'
```

| Verificación | Resultado |
|---|---|
| `GET /actuator/health` | `200`, `{"groups":["liveness","readiness"],"status":"UP"}` |
| `GET /api/productos/MI001?incluir=ofertas` | `200`, destino recibió el path y query intactos |
| `GET /api/categorias` | `200`, destino recibió `/api/categorias` |
| `GET /api/inventario/disponibilidad/MI001` | `200`, destino recibió el path completo |
| `POST /api/productos` con body | `200`, destino recibió método, body y `Authorization` |
| `Authorization: Bearer token-prueba-456` | reenviado sin cambios |
| Preflight `OPTIONS` desde `http://localhost:5173` | `200` con `Access-Control-Allow-Origin`, métodos y headers correctos |
| Origen no permitido (`http://evil.example`) | `403` |
| Respuesta real con `Origin` permitido | una sola cabecera `Access-Control-Allow-Origin` |

### 13.3 Pruebas no ejecutadas / limitaciones

- No se probó contra el microservicio real de Productos porque no estaba
  levantado en `localhost:8083` durante la verificación. El enrutamiento se validó
  con un stub en el mismo puerto y con el mismo comportamiento de path/headers.
- No se probó con el frontend React real; el CORS se validó con preflight y
  respuestas reales vía `curl`.
- No se probó JWT real porque el servicio de autenticación de Diego aún no existe.
- No se probó Pedidos ni Usuarios porque esos microservicios no existen todavía.
- Docker no es necesario para este proyecto.

---

## 14. Comandos útiles

```powershell
# Ejecutar en desarrollo
.\mvnw.cmd spring-boot:run

# Compilar y ejecutar tests
.\mvnw.cmd clean package

# Ejecutar el JAR
java -jar target/gateway-tienda-sonido-0.0.1-SNAPSHOT.jar

# Ver el árbol de dependencias
.\mvnw.cmd dependency:tree
```

---

## 15. Problemas pendientes

1. Integrar el contrato JWT real cuando Diego entregue el servicio de autenticación.
2. Agregar las rutas de Pedidos y Usuarios cuando se confirmen sus puertos y paths.
3. Verificar el enrutamiento contra el microservicio real de Productos levantado en
   `localhost:8083`.
4. Definir con el equipo si el CORS definitivo queda solo en el Gateway o se
   mantiene también en cada microservicio.
5. Evaluar en el futuro service discovery (Eureka) solo si el equipo lo decide;
   hoy las rutas son estáticas por diseño.
