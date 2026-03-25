# ---- Build Stage ----
FROM cgr.dev/chainguard/maven:latest AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Run Stage ----
FROM gcr.io/distroless/java17-debian12:nonroot AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables for DB config (override as needed)
ENV PROFILE_DB_URL=jdbc:postgresql://localhost:5432/profile_db \
    PROFILE_DB_USER=postgres \
    PROFILE_DB_PASSWORD= \
    SOAP_SERVICE_URL=http://localhost:3001/ws \
    PORT=8080

EXPOSE 8080
ENTRYPOINT ["-jar", "/app/app.jar"]
