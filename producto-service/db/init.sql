-- Base de datos del microservicio de productos de Tienda Sonido Vivo.
-- Hibernate crea y actualiza las tablas (spring.jpa.hibernate.ddl-auto=update).
-- Este script solo asegura la base de datos y su juego de caracteres.

CREATE DATABASE IF NOT EXISTS sonidovivo_productos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
