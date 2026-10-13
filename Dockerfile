# ============================================================================
# FISCAL — Custom Tax Management System Dockerfile
# Multi-stage lightweight production container
# ============================================================================

# Stage 1: Build JAR and dependencies
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy compiled classes, dependencies, and web assets
COPY --from=builder /app/target/classes ./target/classes
COPY --from=builder /app/target/fiscal/WEB-INF/lib ./target/fiscal/WEB-INF/lib
COPY --from=builder /app/src/main/webapp ./src/main/webapp

# Port binding
ENV PORT=8080
EXPOSE 8080

# Launch server
CMD ["java", "-cp", "target/classes:target/fiscal/WEB-INF/lib/*", "com.fiscal.server.FiscalDevServer"]
