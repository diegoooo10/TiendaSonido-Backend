# Tienda Sonido Vivo - Backend Docker integrado

Este paquete reúne los cuatro microservicios y un Compose de desarrollo local:
- Gateway: http://localhost:8080
- usuario-service: http://localhost:8081 (solo enlazado a localhost)
- pedido-service: http://localhost:8082 (solo enlazado a localhost)
- producto-service: http://localhost:8083 (solo enlazado a localhost)
- MySQL independientes: productos, pedidos y usuarios.

## Preparación

1. Copiar `.env.docker.example` como `.env.docker`.
2. Completar todas las contraseñas y `JWT_SECRET`.
3. `JWT_SECRET` debe coincidir con el secreto que firma los tokens en usuario-service.
   En el código fuente recibido, usuario-service tiene el secreto escrito directamente
   en `SecurityConfig.java` y no lo lee desde una variable de entorno. Esto debe
   corregirse antes de producción/AWS; no desplegar el secreto dentro del código.
4. Ejecutar desde esta carpeta:

   `docker compose --env-file .env.docker config --quiet`

   `docker compose --env-file .env.docker up --build -d`

5. Ver estado y logs:

   `docker compose --env-file .env.docker ps`

   `docker compose --env-file .env.docker logs -f gateway producto-service pedido-service usuario-service`

## Pruebas básicas

- Gateway health: `http://localhost:8080/actuator/health`
- Productos vía Gateway: `http://localhost:8080/api/productos`
- Categorías vía Gateway: `http://localhost:8080/api/categorias`
- Usuario y pedidos: usar los endpoints del API publicados por sus controladores.

## Datos persistentes

Cada base MySQL tiene un volumen independiente. `docker compose down` conserva los datos.
No ejecutar `docker compose down -v` salvo que se quiera borrar toda la información persistida.

## Importante antes de AWS

Este Compose es una base integrada para pruebas locales, no una configuración de producción lista para AWS.
Antes de producción:
- mover el secreto JWT fuera del código de usuario-service y usar AWS Secrets Manager/SSM;
- rotar el secreto actual porque está embebido en el código fuente compartido;
- no publicar puertos de MySQL ni de los microservicios en Internet;
- usar RDS (o almacenamiento gestionado) con backups y credenciales independientes;
- configurar HTTPS, dominio, CORS de producción, logs/monitorización y health checks;
- definir despliegue y escalado (ECS/Fargate o EC2) y almacenamiento de imágenes/archivos si aplica.
