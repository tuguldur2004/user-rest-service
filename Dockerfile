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

# Runtime port (all DB/service settings should be provided by deployment env vars)
ENV PORT=8080

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
