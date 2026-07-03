# Architecture

## System context

SBQS is split into an Angular client, a Spring Boot API, and local infrastructure managed by Docker Compose. Keycloak owns authentication and roles; PostgreSQL stores transactional data; Redis caches read-heavy queue data; Kafka carries domain events; Camunda coordinates approval workflows.

```text
Angular client
    |
    | HTTP / JWT
    v
Spring Boot API ---- Keycloak
    |  |  |
    |  |  +-------- Kafka
    |  +----------- Redis
    +-------------- PostgreSQL / Camunda
```

## Backend boundaries

The backend uses a layered package structure:

```text
controller -> service -> repository -> database
                 |
                 +---- event and workflow adapters
```

- `controller`: HTTP routing, request validation, and response status codes.
- `service`: business rules, authorization at branch scope, transactions, caching, and domain-event publication.
- `repository` and `mapper`: JPA persistence and report-oriented MyBatis queries.
- `dto`: external request/response and import/report contracts.
- `entity`: persistence models. Sensitive internal fields must never be serialized.
- `config`, `event`, and `workflow`: infrastructure adapters kept outside domain services.

New endpoints should use DTOs at the HTTP boundary. Existing entity-based responses can be migrated incrementally without changing database tables or public routes.

## Frontend boundaries

- `pages`: route-level features. Routes use `loadComponent` so feature code is loaded on demand.
- `shared`: reusable presentational components, layouts, and validation utilities.
- `core/services`: API clients and cross-cutting application services.
- `core/models`: transport and view models.
- `core/guards` and `core/interceptors`: authentication and authorization plumbing.

Page components may depend on `core` and `shared`; reusable `shared` components should not import feature pages.

## Configuration

Committed configuration contains local-development defaults only. Shared or deployed environments must inject database, Keycloak, SMTP, Redis, Kafka, and seed credentials through environment variables or an ignored `application-local.properties` file.

## Change discipline

- Keep business behavior changes separate from mechanical cleanup commits.
- Add or update tests with domain behavior changes.
- Run backend tests, frontend CI tests, and the production frontend build before merging.
- Do not commit generated output, runtime logs, local credentials, IDE state, or crash dumps.
