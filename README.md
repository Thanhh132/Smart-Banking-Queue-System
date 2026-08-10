# Smart Banking Queue System (SBQS)

SBQS is a banking queue management and appointment platform with role-based workflows for customers, staff, branch administrators, and super administrators.

## Technology stack

- Angular 21 and TypeScript
- Java 21 and Spring Boot
- PostgreSQL, Redis, and Kafka
- Keycloak for identity and access management
- Camunda for ticket approval workflows
- JasperReports and Apache POI for reporting and imports
- Docker Compose for local infrastructure

## Repository layout

```text
.
|-- sbqs-backend/    Spring Boot API and business workflows
|-- sbqs-frontend/   Angular web application
|-- docker/          PostgreSQL and Keycloak bootstrap assets
|-- docs/            Architecture and domain documentation
|-- scripts/         Local development utilities
`-- docker-compose.yml
```

See [Architecture](docs/ARCHITECTURE.md) for component boundaries and [Service Category Architecture](docs/SERVICE_CATEGORY.md) for the service-category domain mapping.

## Local development

Prerequisites: Java 21, Node.js with npm, and Docker Desktop.

Start PostgreSQL, Redis, Kafka, and Keycloak:

```bash
docker compose up -d
```

Run the backend:

```bash
cd sbqs-backend
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`.

Run the frontend in another terminal:

```bash
cd sbqs-frontend
npm ci
npm start
```

The frontend runs at `http://localhost:4200`, the API at `http://localhost:8081`, Keycloak at `http://localhost:8080`, and Camunda Cockpit at `http://localhost:8081/camunda/app/cockpit/default/`.

For local development, Cockpit uses `admin` / `admin`. Override `CAMUNDA_ADMIN_USERNAME` and `CAMUNDA_ADMIN_PASSWORD` outside local development. The Camunda login is separate from SBQS/Keycloak authentication.

## Verification

```bash
cd sbqs-backend
./mvnw test

cd ../sbqs-frontend
npm run test:ci
npm run build
```

Local credentials in the default configuration are development-only. Override database and Keycloak settings with the environment variables documented in `sbqs-backend/src/main/resources/application-local.example.properties` before using a shared environment.
