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

Runs on `http://localhost:3000`.

Copy `.env.example` to `.env` and set values.

## Environment

- `PROFILE_DB_URL`
- `PROFILE_DB_USER`
- `PROFILE_DB_PASSWORD`
- `SOAP_SERVICE_URL`
