# syntax=docker/dockerfile:1

# ---------- Etapa de compilacion ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

# Copiar primero el wrapper y el POM para aprovechar la cache de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw -B -DskipTests package

# ---------- Etapa de ejecucion ----------
FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="producto-service" \
      org.opencontainers.image.description="Productos, categorias, precios, inventario y reservas de Tienda Sonido Vivo" \
      org.opencontainers.image.vendor="Tienda Sonido Vivo"

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /build/target/producto-service-*.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8083

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
