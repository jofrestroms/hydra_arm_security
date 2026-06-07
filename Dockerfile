# Multi-stage build OPTIMIZADO
# Usamos eclipse-temurin (versión oficial mantenida de Java)
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalamos curl para el HEALTHCHECK (ajustado para alpine)
RUN apk add --no-cache curl

# Copiar JAR desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto 8081
ENV PORT=8081
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-Dserver.port=8081", "-jar", "app.jar"]