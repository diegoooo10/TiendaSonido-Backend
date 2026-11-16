# Etapa 1: Construcción de la aplicación
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar configuración Maven
COPY pom.xml .

# Descargar dependencias
RUN mvn -B dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar aplicación
RUN mvn -B clean package -DskipTests

# Etapa 2: Ejecución de la aplicación
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar JAR generado
COPY --from=build /app/target/pedido-service-0.0.1-SNAPSHOT.jar app.jar

# Puerto del microservicio
EXPOSE 8082

# Ejecutar aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]