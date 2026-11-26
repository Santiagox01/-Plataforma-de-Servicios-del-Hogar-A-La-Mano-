# Etapa 1: compilación del JAR ejecutable de Spring Boot (sin pruebas; se ejecutan en CI/local)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Etapa 2: imagen de ejecución liviana sin herramientas de compilación ni secretos embebidos
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache su-exec \
    && addgroup -S alamano && adduser -S alamano -G alamano \
    && mkdir -p /app/uploads /data/uploads \
    && chown -R alamano:alamano /app /data/uploads

COPY --from=builder --chown=alamano:alamano /workspace/target/a-la-mano-backend-*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
