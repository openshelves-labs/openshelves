# OpenShelves Architecture

OpenShelves follows a **modular monolith** architecture. Business domains are isolated into self-contained feature modules under `module/`, while cross-cutting infrastructure and shared contracts live at the root level.

---

## Project Structure

```text
src/main/java/com/example/openshelves/
├── OpenShelvesApplication.java   # Main @SpringBootApplication entry point
├── config/                       # Global Spring @Configuration classes (Security, JPA, Async)
├── exception/                    # GlobalExceptionHandler, ApiErrorResponse, custom exceptions
├── filter/                       # Global HTTP servlet filters (RateLimitFilter, JwtAuthFilter)
├── shared/                       # Cross-module base classes & API contracts ONLY
│   ├── dto/                      # ApiResponse, PageResponse
│   ├── event/                    # Shared application events
│   └── model/                    # BaseEntity, AuditableEntity
├── util/                         # Pure, stateless static helpers and constants
└── module/                       # Self-contained business feature modules
    ├── auth/                     # Authentication, JWT tokens, and user management
    ├── book/                     # Book catalog and metadata
    └── notification/             # Multi-channel notification engine (Email, Slack, Teams)
```

---

## Package Responsibilities

### Root Infrastructure Packages

* **`config/`** — Holds `@Configuration` classes and third-party bean definitions (e.g., `SecurityConfig`, `JpaConfig`, `AsyncConfig`).
* **`exception/`** — Centralizes error handling with `@RestControllerAdvice`, custom runtime exceptions, and the standard `ApiErrorResponse` payload.
* **`filter/`** — Contains global servlet filters extending `OncePerRequestFilter` (e.g., rate limiting, request logging, token extraction) that execute before requests reach controllers.
* **`shared/`** — Contains foundational abstractions used across multiple modules, such as `@MappedSuperclass` entities (`BaseEntity`) and standardized API wrappers (`ApiResponse`).
* **`util/`** — Strictly reserved for pure, stateless utility classes (`private` constructor, `public static` methods) with zero Spring dependencies.

### Feature Modules (`module/`)

Each folder inside `module/` represents a single business domain and organizes its own internal layers as needed:

```text
module/<feature>/
├── controller/   # REST endpoints (@RestController)
├── dto/          # Request and response payloads
├── model/        # JPA entities (@Entity) extending BaseEntity
├── repository/   # Spring Data repositories (@Repository)
└── service/      # Business logic and transaction boundaries (@Service)
```

*(Note: Modules only create the inner layer folders they actually need.)*

---

## Architectural Rules

1. **Singular Package Names.** All Java packages use singular nouns (`module`, `filter`, `controller`, `service`, `model`).
2. **No Cross-Module Repository Access.** A module must never inject or query another module's `Repository`. Always communicate across modules through public `Service` interfaces or Spring application events.
3. **The "Rule of Two" for `shared/`.** Only move a class into `shared/` if it is actively required by **two or more modules** (or global infrastructure). Single-module classes stay inside their own module.
4. **No Business Logic in `shared/` or `util/`.** Never annotate classes inside `shared/` or `util/` with `@Service`, `@Component`, or `@Repository`.
5. **Pluggable Notifications.** External messaging integrations (Email, Slack, Teams) live inside `module/notification/channel/` behind the `NotificationSender` interface so new channels can be added without modifying business modules.
