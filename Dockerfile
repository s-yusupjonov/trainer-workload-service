## --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

## --- Runtime stage (non-root) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/trainer-workload-service-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- "http://localhost:${SERVER_PORT:-8082}/actuator/health" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
