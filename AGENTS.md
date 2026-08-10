# Smart Banking Queue System - Agent Guide

## 1. Project Overview

- **Project:** Smart Banking Queue System (SBQS).
- **Purpose:** an Angular and Spring Boot application for bank-branch administration, customer check-in, paperless transaction preparation, queue-ticket issuance, counter operation, and history/reporting.
- **Current users:** `SUPER_ADMIN`, `BRANCH_ADMIN`, `STAFF`, and `CUSTOMER`, as confirmed by Spring Security, Keycloak realm configuration, routes, and service-level checks.
- **Implementation status:** the main full-stack queue journey, GPS-and-queue-based smart branch routing, administration screens, authentication, reporting/import, Camunda workflow, Kafka event publication, and Redis caching exist. The project is still a training/graduation project: some APIs expose entities, several frontend contracts use `any`, schema evolution is performed by startup Java rather than enabled migrations, and some requested training topics are absent or only partially demonstrated.

Do not describe a dependency, Docker container, configuration entry, or README claim as working functionality unless application code actively uses it.

## 2. Source of Truth

- Current source code defines the actual implementation.
- Training and project requirements define target scope and completion criteria.
- Documentation is supporting context and must not override working source code.
- When documentation and source disagree, inspect both sides of the implementation, record the discrepancy, and follow the code unless the requested task explicitly changes it.
- `docs/database-design.md` and `docker/postgres/init.sql` describe an older initial model in places; entities, repositories, `DatabaseSchemaInitializer`, and live database constraints are more authoritative for current behavior. The global service catalog is database-managed by Super Admin and is not seeded from a hard-coded Java list.

## 3. Technology Stack

### Frontend

- Angular 21.2, TypeScript 5.9, RxJS, HTML, and SCSS.
- Standalone components with lazy `loadComponent` routes; there are no feature NgModules.
- Bootstrap 5 grid/utilities plus project SCSS; Lucide Angular supplies icons.
- Reactive forms and template-driven forms are both used.
- Functional route guards and an HTTP interceptor are registered in `app.config.ts`.
- Vitest through the Angular unit-test builder.

### Backend

- Java 21, Spring Boot 3.5, Maven Wrapper, Spring Web, Validation, Data JPA/Hibernate, Mail, Cache, Security OAuth2 Resource Server, and springdoc OpenAPI.
- Constructor injection is the dominant style. Services contain most business rules and transactions.
- JPA handles operational persistence; MyBatis handles report queries.
- A bounded async executor processes mail/notification work after transaction commit.
- No Spring AOP aspect implementation or `RequestPart` usage was found.

### Database

- PostgreSQL 15 in Docker Compose.
- Identity-generated `BIGSERIAL`/JPA `IDENTITY` primary keys, relational foreign keys, composite uniqueness, and queue/history lookup indexes.
- Flyway dependencies and a location are declared, but Flyway is disabled. Schema creation/evolution currently combines Docker bootstrap SQL with idempotent JDBC startup initializers.
- No application stored procedures, named database functions, or scheduled database jobs were found.

### Authentication and authorization

- Keycloak 24 realm/client bootstrap, direct-grant custom login, Spring JWT resource-server validation, BCrypt local hashes, and short-lived fallback JWTs.
- Keycloak refresh tokens and logout are supported; fallback tokens have no refresh token.
- Backend endpoint rules and service branch checks are authoritative; frontend guards are navigation assistance only.

### Workflow

- Embedded Camunda Platform 7.23 with an executable ticket process in `src/main/resources/processes/ticket-approval.bpmn`.

### Reporting and file processing

- JasperReports templates generate PDF and XLSX documents.
- Apache POI reads `.xlsx` staff/service imports and generates import templates.
- Spring `MultipartFile` uploads are used for import endpoints.

### Messaging and caching

- Kafka publishes best-effort domain events for branch, service, counter, queue-machine, mapping, and ticket changes. No application Kafka consumer was found.
- Redis backs Spring caches for branches, services, and queue-monitor results with explicit eviction and a fail-open cache error handler.
- Spring application events plus an async listener send called-ticket email and Web Push notifications after commit. Web Push notifies subscribed customer devices when three tickets remain ahead and when the ticket is called.

### Containerization and tools

- Docker Compose provides PostgreSQL, Redis, Kafka, Keycloak, and MailHog.
- Maven Wrapper, npm lockfile, Angular CLI, Git, Prettier configuration through `package.json`, and Docker Compose support local development.

## 4. Repository Architecture

- `sbqs-backend/src/main/java/com/sbqs/`: layered Spring API (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `event`, `workflow`, `exception`).
- `sbqs-backend/src/main/resources/`: application configuration, MyBatis XML, Jasper `.jrxml` templates, and Camunda BPMN.
- `sbqs-backend/src/test/`: service, workflow, reporting, import, authentication, and context tests.
- `sbqs-frontend/src/app/pages/`: lazy route-level screens for auth, account, customer, staff, branch admin, super admin, and queue monitoring.
- `sbqs-frontend/src/app/core/`: API services, models, guards, interceptor, configuration, and cross-cutting client state.
- `sbqs-frontend/src/app/shared/`: reusable layouts, UI components, directive, and validation utilities.
- `sbqs-frontend/src/styles.scss`: global theme and shared SCSS foundation.
- `docker/postgres/` and `docker/keycloak/`: database and identity bootstrap assets.
- `docs/`: architecture and domain notes; validate them against code before use.
- `scripts/`: explicitly destructive development-data reset utility and instructions; never run it without approval.

## 5. Business Roles

### `SUPER_ADMIN`

- Manages branches, branch-admin accounts, and the global service catalog through manual creation, editing, or Excel import; can view users system-wide and export system-scoped administrative reports.
- Has no branch scope. The user service blocks editing/deleting another super admin through the normal management API.
- The frontend provides the super-admin dashboard, branch management, and global service-catalog routes, not every operational branch-admin page.

### `BRANCH_ADMIN`

- Operates within the account's assigned branch: receives all active global-catalog services automatically, configures their local availability/forms, and manages staff, queue machines, counters, mappings, appointments, monitor data, imports, and branch-scoped reports. Branch admins cannot rename or delete global-catalog services.
- May manage only `STAFF` users in the same branch. Identity mutations/imports require a non-fallback Keycloak session.
- Cross-branch protection is implemented in services and must remain in addition to URL authorization.

### `STAFF`

- Lists/claims a counter in the assigned branch, calls the next ticket for that counter's queue machine, views the prepared transaction, completes service, views pending workflow tasks, history, and queue monitoring.
- Ticket operations require ownership of the active counter session; a counter cannot call another ticket while serving one.
- Staff do not manage users, branches, categories, queue machines, or mappings.

### `CUSTOMER`

- Registers/verifies an account, maintains a profile, selects a bank branch and a mapped service, prepares transaction form data, obtains one active ticket, tracks/cancels that ticket, views own history, and exports own history.
- Customer branch selection is journey state, not a permanent branch assignment.
- Customers may cancel only their own `WAITING` ticket; they cannot cancel a serving ticket.

## 6. Queue Management Business Rules

- **Branch selection:** authenticated customers can list/filter branches and request Smart Routing recommendations after sharing GPS coordinates. `SmartRoutingService` ranks active branches using normalized distance (default 40%) and estimated waiting time (default 60%), where waiting time uses queued service workload divided by active counters; branches without an active counter receive a configurable penalty and are not marked recommended. Closed branches remain visible with their outside-hours status, but selection and the service-selection route are blocked until they reopen. Operational users remain constrained to their assigned branch.
- **Operating hours:** each branch has a seven-day schedule with separate morning and afternoon intervals. Smart Routing excludes branches outside service hours, and ticket issuance enforces the same rule server-side. Missing schedules use Mon-Fri 08:00-12:00 and 13:00-17:00, with weekends closed.
- **Digital delegation:** customers can issue a time-limited, single-use delegation scoped to one branch and service. Delegate identity numbers are BCrypt-protected and only the last four digits are returned; staff must verify the reference code and physical identity before marking it used.
- **Service selection:** a service belongs to one branch. Customer selection uses active services mapped to at least one queue machine for that branch; required customer-profile fields and service-specific form schema are validated before ticket issuance.
- **Ticket creation:** only a customer may create a normal/prepared ticket. The backend rejects a second active `WAITING` or `SERVING` ticket for the same email.
- **Ticket numbering:** numbering is an integer sequence per queue machine. The service pessimistically locks the machine, increments `last_ticket_number`, and persists within a transaction. No daily reset is implemented in the normal issuance path.
- **Waiting queue:** tickets start as `WAITING`; ordering for `call-next` is ascending ticket number within the counter's assigned queue machine.
- **Counter assignment:** staff claim an available counter through an active `counter_sessions` record. The counter must be active, assigned to that staff member, and linked to a queue machine.
- **Calling next:** calling completes the Camunda approval task, moves the ticket directly from `WAITING` to `SERVING`, records `serving_started_at`, attaches it as the counter's current ticket, publishes an event, and triggers called-ticket email work.
- **Serving/completion:** only the staff member holding the counter may inspect prepared fields or complete its `SERVING` ticket. Completion records a history snapshot, clears the counter, completes the Camunda service task, and sets `COMPLETED`.
- **Missed tickets:** the staff member holding the serving counter may mark its current `SERVING` ticket as `MISSED` when the called customer does not arrive. The operation closes the Camunda instance, releases the counter, records an immutable staff/history snapshot, and lets the customer obtain a new ticket. No recall or `SKIPPED`/`EXPIRED` flow is implemented.
- **Cancellation:** only the owning customer may change a `WAITING` ticket to `CANCELLED`; the active Camunda instance is deleted and a history snapshot/event is recorded.
- **Tracking:** customer tracking verifies ownership and reports status, people ahead, counter, branch, service, and queue-machine details. Queue monitor aggregates waiting counts and counter states and is cached briefly.
- Current ticket statuses are `WAITING`, `SERVING`, `COMPLETED`, `CANCELLED`, and `MISSED`; counter monitor states are `INACTIVE`, `IDLE`, and `SERVING`. The monitor includes the active counter-session staff name.

## 7. Authentication and Security

- The Angular custom login page posts credentials to `/api/auth/login`; do not replace it with the default Keycloak page unless explicitly requested.
- The backend first requests Keycloak tokens through direct access grants, resolves realm roles, and maps `ADMIN_BRANCH` to `BRANCH_ADMIN` for compatibility.
- Spring Security verifies issuer, signature, and intended Keycloak client; it also verifies internal HS256 fallback JWTs and maps realm roles to `ROLE_*` authorities.
- Fallback login uses a local BCrypt hash and issues a short-lived access token with `token_source=fallback`. It cannot refresh, and security blocks identity, branch, and import mutations from fallback sessions.
- Angular stores access token, refresh token, role, identity display data, authentication source, selected branch, and current journey data in `sessionStorage`, not `localStorage`.
- The interceptor adds Bearer tokens, refreshes once after a 401 when a refresh token exists, and clears/navigates to login on refresh failure.
- Logout clears browser session data immediately and revokes a Keycloak refresh token when present.
- Customer registration creates disabled/PENDING Keycloak and application users, stores a BCrypt hash, and requires email verification before activation.
- Forgot/reset password uses non-enumerating responses, cooldown, hashed one-use database tokens, expiry, SMTP email, Keycloak password update, and BCrypt synchronization.
- Passwords are validated by a shared policy and never serialized; `passwordHash` and `keycloakUserId` have `@JsonIgnore`.
- Login rate limiting and authentication audit records exist. CSRF is disabled for the stateless API; CORS permits configured local frontend origins.
- Treat route guards as UX only. Preserve backend URL rules, branch checks, ownership checks, and non-fallback restrictions.

## 8. Backend Coding Rules

- Keep business logic in service classes; controllers validate/map HTTP input and delegate.
- Use constructor injection and follow the existing package architecture.
- Reuse current repositories, mappers, services, event publisher, and workflow adapters.
- Prefer request/response DTOs. Do not expand existing entity-based endpoints without checking serialization and clients.
- Validate DTOs with Jakarta Validation and enforce ownership, status transitions, uniqueness, and branch scope in services.
- Never expose password hashes, tokens, Keycloak IDs, reset hashes, audit internals, or prepared transaction data to unauthorized users.
- Use transactions for multi-record state changes; preserve pessimistic locking around queue-machine numbering/counter selection.
- Check foreign-key, history, active-ticket, active-session, and frontend impact before deletion.
- Avoid new dependencies unless the repository has no suitable equivalent.
- Do not change API contracts without checking all callers under `sbqs-frontend/src/app/core/services/` and affected pages.
- Use domain events as best-effort notifications only; database consistency must not depend on Kafka delivery.
- Do not claim tests passed unless the exact command completed successfully.

## 9. Frontend Coding Rules

- Follow the discovered standalone Angular architecture; lazy-load route components and do not introduce an NgModule without a concrete need.
- Put backend calls in `core/services`; components should not build ad hoc API calls when a service exists.
- Reuse shared layouts, headers, buttons, cards, icons, import panel, report controls, notices, and validation utilities.
- Prefer typed interfaces/models over current legacy `any` uses, especially when changing a related contract.
- Keep templates, component TypeScript, and SCSS focused and maintainable; preserve Bootstrap/grid and established visual tokens.
- Use reactive forms for validation-heavy/auth/profile/dynamic forms and template-driven forms where the existing operational CRUD pattern already uses them.
- Validate on both frontend and backend and show actionable, user-friendly errors.
- Preserve `roleGuard`, `homeRedirectGuard`, and `authInterceptor`; never rely on them for server authorization.
- Keep screens responsive and consistent; do not duplicate shared UI or direct `sessionStorage` conventions without checking `AuthService` and tracking services.
- A standalone `PreventAutofillDirective` uses host listeners. No custom pipes or `@HostBinding` implementation is currently present; add only for a real feature need.

## 10. Database Rules

- Use lowercase plural snake_case table names and snake_case columns, matching existing JPA mappings.
- Use generated bigint identity primary keys. Preserve explicit foreign keys, composite mapping keys, and per-branch code uniqueness.
- Important relationships include branch-to-users/services/machines/counters/tickets, machine-to-services/counters/tickets, ticket-to-transaction-draft/history, and token-to-user.
- Preserve queue/history/authentication indexes used by polling, ownership lookup, and reports.
- Do not rename tables/columns without checking entities, repositories, MyBatis XML, Jasper data sources, startup SQL, and frontend contracts.
- Never delete data to resolve schema errors or recreate Docker volumes without explicit approval.
- Inspect existing rows and constraints before schema changes. Preserve historical snapshots and prefer soft status changes where services already do so.
- Prefer a controlled migration for new work. Current reality is legacy Docker init plus idempotent `DatabaseSchemaInitializer`/`PreparedServiceCatalogInitializer`; Flyway is disabled and no `db/migration` scripts are present.
- Do not add more destructive startup migration behavior. Never run `scripts/reset-dev-data.ps1` without explicit approval.
- The repository does not verify required `TRUNCATE`, stored procedure, function, sequence-management, or database-job exercises beyond PostgreSQL identity sequences; treat these training items as missing/not verified.

## 11. REST API Rules

- APIs use `/api/<resource>` plural nouns, path IDs, query filters, JSON request bodies, and multipart form data for imports.
- Use request DTOs plus `@Valid` for new/changed write contracts and response DTOs where sensitive or unstable entities are involved.
- Return appropriate statuses: existing code commonly uses 200, 202 for forgot-password, and 204 for logout/deletes. Preserve compatibility unless coordinating a contract change.
- `GlobalExceptionHandler` returns JSON with timestamp, status, error, and message; login throttling returns 429 and security returns 403 where applicable.
- Do not expose raw persistence/security exceptions. Add friendly constraint/error mapping when a new integrity rule needs it.
- No API pagination contract is currently implemented. Add pagination only as an explicit, coordinated backend/frontend change.
- Every non-public endpoint must remain authenticated, role-authorized, branch-scoped, and ownership-checked as applicable.
- Preserve endpoint paths and response shapes unless all frontend consumers and tests are updated together.

## 12. Reporting and File Processing

- `ReportController` exports users, services, tickets, and history using `format=pdf|xlsx`.
- Jasper `.jrxml` templates are compiled and cached in-process; bean rows come from scoped JPA/MyBatis-backed services.
- PDF uses Jasper export; Excel export uses Jasper's XLSX exporter. Both are implemented.
- Apache POI implements `.xlsx` template generation and parsing for staff and service imports.
- Multipart imports are branch-admin-only, non-fallback operations, limited to `.xlsx`, 5 MB, and 500 data rows; row errors are collected while valid rows may succeed.
- `MultipartFile` is implemented. `RequestPart`, non-Excel uploads, and a generic file-storage integration are not implemented.
- Keep templates under `src/main/resources/reports/`; test template compilation and both output formats after changes.

## 13. Camunda Workflow

- BPMN file: `sbqs-backend/src/main/resources/processes/ticket-approval.bpmn`; process key: `ticketApprovalProcess`.
- Flow: customer creates ticket -> `STAFF` candidate approval task -> called-ticket email service task -> serving user task assigned to the staff email -> completion.
- `TicketWorkflowService` starts instances by ticket business key, claims/completes tasks, scopes pending tasks to the staff branch, completes serving, and cancels active instances.
- The frontend staff dashboard lists pending approval tasks, but approval occurs as part of calling the next ticket.
- This is a maker/checker-like customer-to-staff approval workflow. There are no distinct roles literally named `MAKER` and `CHECKER`, no separate checker decision/rejection branch, and no Camunda-specific customer task UI.
- Camunda uses the application database with automatic schema update. Preserve alignment between BPMN task IDs, service constants, delegates, listeners, tests, and ticket transitions.

## 14. Messaging, Kafka, and Cache

- `KafkaDomainEventPublisher` implements a publisher/adapter pattern and sends JSON domain events to `sbqs.domain-events` when enabled.
- Publication failures are logged and do not roll back the completed business operation; there is no outbox, retry store, or application consumer.
- Redis is actively used through `@Cacheable`/eviction for branches, services, and queue monitor; the Docker container alone is not the evidence.
- Cache errors fail open and are rate-limited in logs. Correctness must remain in PostgreSQL and service rules.
- Called-ticket mail and Web Push use transactional application events, an async listener, and Camunda delegate integration. Queue advancement also emits a threshold event for the ticket that reaches exactly three people ahead. MailHog is local SMTP capture, not a durable message queue.
- Web Push subscriptions and per-ticket delivery deduplication are stored in PostgreSQL. Browser delivery uses environment-provided VAPID keys; never commit the private key. Kafka still carries best-effort domain events and is not required for push delivery.
- Existing patterns include layered architecture, DTO mapper, repository, dependency injection, domain-event publisher/adapter, strategy-like report format handling, and shared frontend services/components.

## 15. Build, Run, and Verification

From repository root on Windows PowerShell:

```powershell
docker compose up -d
cd sbqs-backend
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd sbqs-frontend
npm ci
npm start
```

Build and test commands declared by the repository:

```powershell
cd sbqs-backend
.\mvnw.cmd clean compile
.\mvnw.cmd test

cd ..\sbqs-frontend
npm run test:ci
npm run build
```

- Default ports: frontend `4200`, backend `8081`, Keycloak `8080`, PostgreSQL `5432`, Redis `6379`, Kafka `9092`, SMTP `1025`, MailHog UI `8025`.
- Check listeners with `Get-NetTCPConnection -State Listen` or `docker compose ps` before diagnosing port conflicts.
- Infrastructure-dependent application startup requires valid local secrets/configuration, especially a fallback JWT secret of at least 32 bytes.
- Report the actual result of every command run. A command listed here is not proof it passed in the current working tree.

## 16. Git Workflow

- Use `main` and `develop`; normal development changes target `develop`.
- Use clear conventional commits such as `feat:`, `fix:`, `test:`, `docs:`, or `refactor:`.
- Do not commit secrets, ignored `application-local.properties`, `.env`, generated reports, logs, `target/`, `dist/`, coverage, or `node_modules/`.
- Review `git status` and `git diff` before committing. Preserve unrelated user changes.
- Keep each commit focused on one feature or fix and include relevant tests/docs.

## 17. Requirement Coverage

| Capability | Status | Repository evidence / limitation |
|---|---|---|
| Frontend CRUD | Implemented | Branches, services/categories, users, queue machines, counters, and mappings have route-level UIs. |
| User management | Implemented | Super admin manages branch admins and branch admin manages same-branch staff. Deletion permanently removes eligible staff/branch-admin accounts from both PostgreSQL and Keycloak; an actively served ticket must be completed first. |
| Category/service management | Implemented | Super Admin manages the database-backed global catalog by create/edit/safe delete-or-archive/restore/Excel import; active entries are inherited by every existing/new branch, while branch admins only configure local availability/forms/mappings. |
| Login / logout | Implemented | Custom Angular forms, Keycloak direct grant, refresh/revoke, and local session clearing. |
| Registration | Implemented | Customer registration plus email verification and PENDING activation. |
| Password hashing | Implemented | BCrypt local hashes; Keycloak manages primary credentials. |
| Forgot password | Implemented | One-use hashed token, SMTP link, cooldown, Keycloak/local password synchronization. |
| Keycloak | Implemented | Realm bootstrap, admin/user clients, token exchange, role sync, refresh, logout, and JWT verification. |
| JWT | Implemented | Keycloak access tokens plus restricted short-lived fallback JWTs. |
| Session/local storage | Implemented | `sessionStorage` is used; `localStorage` is not the current pattern. |
| Form validation | Implemented | Angular validators, Jakarta Validation, password policy, service-form validation, and import validation. |
| Interceptor | Implemented | Functional auth header/refresh interceptor. |
| Shared components/modules | Partial | Shared standalone components/layouts/utilities exist; no shared NgModule because the app is standalone. |
| Pipes / host binding | Missing | No custom Angular pipe or `@HostBinding` found; host metadata and a host-listener directive exist. |
| JasperReports / PDF / Excel export | Implemented | Scoped report service and four `.jrxml` templates export PDF/XLSX. |
| Apache POI import | Implemented | Staff, branch-service, and global service-catalog `.xlsx` templates and parsing. |
| File upload | Partial | Multipart Excel import exists; no general file storage or `RequestPart`. |
| Camunda | Implemented | Embedded engine, executable BPMN, service/delegate/listener integration, and tests. |
| Maker-checker workflow | Partial | Customer creation followed by staff approval/serve and email; no explicit maker/checker roles or reject path. |
| PostgreSQL | Implemented | Docker database, JPA entities, relational constraints, indexes, and operational queries. |
| JPA/Hibernate | Implemented | Operational repositories/entities use Spring Data JPA. |
| MyBatis | Implemented | Report mapper interface/XML executes scoped join queries. |
| Database advanced exercises | Partial | Joins, filtering, indexes, constraints, and identity sequences exist; stored procedures/functions/jobs and several prescribed SQL exercises are not verified. |
| Unit testing | Partial | Backend and frontend tests exist, but coverage is uneven and pass status must be established per working tree. |
| Kafka/message queue | Partial | Kafka producer and domain events exist; no consumer, outbox, or delivery guarantee. |
| Web Push queue notification | Implemented | Customer device subscription, service worker delivery, VAPID configuration, three-ahead/called events, and delivery deduplication exist; browser/OS permission remains required. |
| Redis/caching | Implemented | Application cache annotations/configuration actively use Redis with TTL and eviction. |
| Git branches | Implemented | Local and origin `main`/`develop` refs are present; process compliance is not provable from code alone. |
| Design patterns | Implemented | Layering, repository, mapper, publisher/adapter, DI, shared service/component, and report-format patterns are visible. |
| AOP | Missing | No application aspect was found. |
| Multithreading/concurrency | Partial | Bounded async notifications and pessimistic database locks exist; broader training exercises are not verified. |

## 18. AI Agent Rules

- Read all directly related files before editing.
- Follow existing architecture and naming conventions.
- Do not rewrite unrelated code.
- Do not rename files, classes, methods, endpoints, tables, or columns unless required.
- Do not change frontend-backend contracts without checking both sides.
- Do not modify database schemas casually.
- Do not remove existing data to solve development errors.
- Do not add dependencies without checking whether the project already provides an equivalent.
- Do not replace working infrastructure unnecessarily.
- Do not silently change business rules.
- Do not implement speculative banking functionality.
- Do not claim a requirement is complete without repository evidence.
- Do not claim a build or test passed unless it was actually executed.
- Explain every changed file.
- Report verification commands and their actual results.
- Mention unresolved risks and limitations.
- Keep solutions appropriate for a graduation or training project; avoid unnecessary enterprise-scale complexity.
- Preserve unrelated working-tree changes and ask before destructive Git, data, volume, or reset operations.

## 19. Change Checklist

1. Understand the requested feature.
2. Inspect related frontend, backend, database, and security code.
3. Identify business rules and affected roles.
4. Check existing API contracts.
5. Check database relationships and existing data.
6. Implement the smallest coherent change.
7. Run relevant builds or tests.
8. Review the diff.
9. Summarize changes, verification, and remaining risks.
10. Remind the developer to commit and push to `develop` after a completed checkpoint.

## Maintenance of This File

- Prefer stable architectural directories over exhaustive file, endpoint, controller, entity, table, or component inventories.
- Use specific paths only for important entry points, configuration, templates, and workflows.
- Keep current implementation separate from target requirements and gaps.
- Avoid exact counts unless essential; update this guide when architecture, roles, authentication, workflow, schema strategy, reporting, messaging, or caching materially changes.
- Do not rewrite accurate project-specific guidance for small feature additions.
