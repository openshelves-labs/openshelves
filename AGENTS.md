# AI Agent Instructions for OpenShelves

This document provides system instructions and context for any AI coding assistants, autonomous agents, and language models (e.g., Cursor, GitHub Copilot, Windsurf, Claude) interacting with the OpenShelves repository.

## 1. Project Overview
OpenShelves is a platform modernizing the university library experience, turning complex academic catalogs into intuitive, open discovery platforms.

The repository is structured as a monorepo:
- `/backend`: The core API services.
- `/frontend`: The user interface (currently pending initialization).
- `/infra`: Infrastructure as Code and deployment configurations (currently pending initialization).

## 2. Technology Stack
### Backend (`/backend`)
- **Language**: Java 25 (Azul JVM).
- **Build Tool**: Gradle.
- **Framework**: Spring Boot 4.0.6 (Spring WebMVC, Spring Data JPA).
- **Database**: PostgreSQL.
- **Key Libraries**: Lombok (for boilerplate reduction), Resilience4j (for fault tolerance/rate limiting), OkHttp3 (for HTTP clients).
- **Testing**: JUnit 5 Platform.

### Frontend (`/frontend`)
- *Stack to be determined.*

### Infrastructure (`/infra`)
- *Stack to be determined.*

## 3. Coding Guidelines
When generating code for this repository, AI agents MUST adhere to the following rules:

### General
- **No Hallucinated Libraries**: Do not introduce new third-party dependencies without explicit user permission. Stick to the BOMs and versions defined in `build.gradle`.
- **Formatting**: Maintain the existing code style. Ensure code is readable, modular, and well-documented.

### Backend (Java)
- **Lombok First**: Use Lombok annotations (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) to minimize boilerplate code.
- **Dependency Injection**: Use constructor injection (via Lombok's `@RequiredArgsConstructor`) rather than field injection (`@Autowired`).
- **Resilience**: Wrap external service calls with Resilience4j patterns where applicable.
- **Testing**: Write JUnit 5 tests for all new business logic. Mock external dependencies appropriately.

## 4. Documentation & Workflows
- **Pull Requests**: When asked to draft a PR, strictly follow the format outlined in `.github/PULL_REQUEST_TEMPLATE.md`. Keep descriptions concise and professional.
- **AI Usage Policy**: Any code generated must comply with `AI_USAGE_POLICY.md` (e.g., no insertion of copyrighted/GPL code).
- **Commit Messages**: Write clear, imperative commit messages.
