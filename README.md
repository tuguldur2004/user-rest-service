# user-rest-service-spring

REST profile service with SOAP token validation middleware

## Endpoints

- `POST /users`
- `GET /users/{id}`
- `GET /users/name/{username}`
- `PUT /users/{id}`
- `DELETE /users/{id}`

## Run

```bash
mvn spring-boot:run
```

Runs on `http://localhost:8080` (or `$PORT` if set).

## Environment

- `PROFILE_DB_URL`
- `PROFILE_DB_USER`
- `PROFILE_DB_PASSWORD`
- `SOAP_SERVICE_URL`
- `CORS_ALLOWED_ORIGINS` (comma-separated, required in production)
- `PORT` (set by DigitalOcean App Platform automatically)

## Deploy to DigitalOcean (App Platform)

1. Push this project to GitHub.
2. In DigitalOcean, create a new **App Platform** app from that repository.
3. Choose **Dockerfile** as build method (this repo already includes one).
4. Set component HTTP port to `8080`.
5. Add environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `PROFILE_DB_URL=jdbc:postgresql://<host>:5432/<db>`
   - `PROFILE_DB_USER=<db_user>`
   - `PROFILE_DB_PASSWORD=<db_password>`
   - `SOAP_SERVICE_URL=https://<soap-service-host>/ws`
   - `CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>`
6. Configure health check path as `/health`.
7. Deploy.

You can also use the included DigitalOcean app spec at .do/app.yaml.
Before deploying, replace all `CHANGE_ME` values with real secrets.

### Notes

- The app expects PostgreSQL to be reachable from the internet/VPC.
- Keep `PROFILE_DB_PASSWORD` in DigitalOcean encrypted secrets.
- CORS is restricted by `CORS_ALLOWED_ORIGINS` in production profile.

## Deploy to DigitalOcean Droplets

For the 3-droplet topology, run the REST service on the same droplet as the SOAP service, but expose it on a different host port:

- REST service host port: `8081`
- SOAP service host port: `8082`

Set `SOAP_SERVICE_URL` to the SOAP service on the same droplet, for example:

- `SOAP_SERVICE_URL=http://soap-service:8080/ws` when both services run in Docker Compose on one droplet
- `SOAP_SERVICE_URL=http://<rest-soap-private-ip>:8082/ws` when calling the SOAP service through the droplet host port

The gateway should call the REST service through the private VPC address, not a public IP.
