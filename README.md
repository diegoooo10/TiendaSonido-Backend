# TiendaSonido — Microservicio de Usuarios

Microservicio REST desarrollado con **Spring Boot** para gestionar usuarios y autenticación en el backend de **TiendaSonido**. Forma parte de una arquitectura de microservicios junto con los servicios de productos y pedidos.

## Tecnologías

- Java 21
- Spring Boot 4.1.2 y Maven
- Spring Web, Spring Data JPA y Spring Security
- MySQL
- JSON Web Tokens (JWT)
- Bean Validation y Lombok
- SpringDoc OpenAPI / Swagger UI
- Docker

## Funcionalidades

- Registro de usuarios con rol `CLIENTE` por defecto.
- Inicio de sesión mediante correo electrónico y contraseña.
- Contraseñas almacenadas mediante `PasswordEncoder`.
- Generación de tokens JWT al iniciar sesión.
- Listado y actualización de usuarios.
- Validación de datos de entrada y comprobación de duplicados de correo y RUN.
- Manejo centralizado de excepciones y respuestas HTTP.
- Configuración de seguridad con Spring Security.

> **Estado:** El microservicio cuenta con registro, inicio de sesión y generación de JWT. La integración de seguridad entre microservicios y los ajustes finales de autorización deben verificarse en el proyecto integrado.

## Estructura del proyecto

El código está organizado por responsabilidades:

| Componente | Responsabilidad |
| --- | --- |
| `controller` | Expone los endpoints HTTP. |
| `service` | Implementa las reglas de negocio. |
| `repository` | Accede a MySQL mediante Spring Data JPA. |
| `model` / `entity` | Representa las entidades persistentes. |
| `dto` | Define los datos de entrada y salida. |
| `mapper` | Convierte entre entidades y DTO. |
| `config` / `security` | Configura la seguridad y el manejo de JWT. |
| `exception` | Gestiona errores específicos y respuestas de error. |

*Los nombres exactos de los paquetes pueden variar según la organización del repositorio.*

## Modelo de usuario

El usuario contiene los siguientes atributos:

| Campo | Descripción |
| --- | --- |
| `id` | Identificador autogenerado. |
| `username` | Nombre de usuario único, si se mantiene en el modelo. |
| `password` | Contraseña cifrada mediante hash. |
| `rol` | `ADMIN` o `CLIENTE`. |
| `nombre`, `apellido` | Datos personales. |
| `correo` | Correo electrónico único, utilizado para iniciar sesión. |
| `run` | RUN único. |
| `fechaNacimiento` | Fecha de nacimiento. |
| `direccion` | Dirección del usuario. |

Las respuestas públicas deben excluir la contraseña y otros datos sensibles innecesarios.

## API REST

Los siguientes son los **tipos de operaciones implementadas**. Consulta los controladores o Swagger UI para confirmar las rutas exactas antes de consumir la API.

| Operación | Método HTTP | Descripción |
| --- | --- | --- |
| Registrar usuario | `POST` | Crea un usuario con rol `CLIENTE`. |
| Iniciar sesión | `POST` | Valida credenciales y devuelve un JWT. |
| Listar usuarios | `GET` | Obtiene usuarios según las reglas de autorización. |
| Actualizar usuario | `PUT` / `PATCH` | Modifica datos de un usuario existente; comprobar el método real en el controlador. |

### Ejemplo de registro

```json
{
  "password": "ClaveSegura123!",
  "nombre": "Ana",
  "apellido": "Pérez",
  "correo": "ana@example.com",
  "run": "12345678-9",
  "fechaNacimiento": "2000-05-15",
  "direccion": "Dirección de ejemplo"
}
```

### Ejemplo de inicio de sesión

Solicitud:

```json
{
  "correo": "ana@example.com",
  "password": "ClaveSegura123!"
}
```

Respuesta exitosa (estructura ilustrativa):

```json
{
  "token": "<JWT_GENERADO>"
}
```

Para consumir rutas protegidas, envía el token cuando la configuración de seguridad del endpoint lo requiera:

```http
Authorization: Bearer <JWT_GENERADO>
```

## Requisitos previos

- JDK 21
- Maven (o el Maven Wrapper incluido en el proyecto)
- MySQL en ejecución (por ejemplo, mediante Laragon)
- Docker, opcional

## Configuración

1. Clona el repositorio y cambia a la rama del microservicio:

   ```bash
   git clone https://github.com/diegoooo10/TiendaSonido-Backend.git
   cd TiendaSonido-Backend
   git checkout usuario-service
   ```

2. Crea una base de datos MySQL para el servicio y configura la conexión en `application.properties` o `application.yml`, según el proyecto.

3. Configura las credenciales de la base de datos y el secreto JWT de forma segura. **No subas contraseñas ni secretos reales a GitHub.** Los nombres concretos de las propiedades dependen de la configuración de tu aplicación.

4. Inicia el servicio:

   ```bash
   ./mvnw spring-boot:run
   ```

   En Windows PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

## Docker

El proyecto incorpora un `Dockerfile` para construir una imagen del microservicio. Desde la carpeta que contiene ese archivo:

```bash
docker build -t tiendasonido-usuario-service .
```

La ejecución del contenedor requiere configurar correctamente la conexión a MySQL y las variables o propiedades utilizadas por el proyecto. Si MySQL se ejecuta fuera del contenedor, `localhost` dentro del contenedor no apunta al host.

> El puerto expuesto, las variables de entorno y el comando `docker run` deben ajustarse al `Dockerfile` y a la configuración reales del servicio.

## Documentación y pruebas

El proyecto incluye SpringDoc OpenAPI. Con el servicio iniciado, comprueba la ruta de Swagger UI configurada (habitualmente `/swagger-ui/index.html`) y el documento OpenAPI (habitualmente `/v3/api-docs`).

Pruebas manuales recomendadas:

- Registrar un usuario válido.
- Rechazar correos y RUN duplicados.
- Rechazar solicitudes con campos obligatorios inválidos.
- Iniciar sesión con credenciales correctas e incorrectas.
- Verificar que las respuestas no revelen contraseñas.
- Comprobar los permisos de acceso a operaciones protegidas.

## Integración con otros microservicios

Este servicio centraliza la gestión de usuarios y la emisión de JWT. Los microservicios de productos y pedidos forman parte del mismo backend, pero deben configurar y verificar su propia autorización o utilizar la arquitectura de seguridad que se defina para la integración.

## Repositorio

[diegoooo10/TiendaSonido-Backend](https://github.com/diegoooo10/TiendaSonido-Backend) — rama `usuario-service`.

## Notas

Este README documenta la implementación conocida del proyecto. Antes de publicarlo como documentación definitiva, conviene contrastar rutas, nombres de paquetes, puerto y configuración Docker con el código fuente actual.
