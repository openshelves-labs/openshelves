# OpenShelves: The Future of Academic Discovery

[![License: BUSL 1.1](https://img.shields.io/badge/License-BUSL_1.1-0366D6?style=flat)](LICENSE)
[![Spring Boot 4.1.2](https://img.shields.io/badge/Spring_Boot-4.1.2-6DB33F?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?style=flat&logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Next.js 16.4.3](https://img.shields.io/badge/Next.js-16.4.3-6E7681?style=flat&logo=nextdotjs)](https://nextjs.org/)
[![Node.js 26](https://img.shields.io/badge/Node.js-26-5FA04E?style=flat&logo=nodedotjs)](https://nodejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Contributions Welcome](https://img.shields.io/badge/Contributions-Welcome-2EA44F?style=flat)](CONTRIBUTING.md)
[![Code of Conduct](https://img.shields.io/badge/Code_of_Conduct-Adopted-0366D6?style=flat)](CODE_OF_CONDUCT.md)

OpenShelves turns complex, legacy university library catalogs into fast, intuitive and open discovery platforms for students and researchers.

> [!IMPORTANT]
> **Project status: early development.**
> Not ready for production. We're building the core architecture and the first features (sign-in, catalog, circulation).

---

## 🌟 The vision

Libraries should be as easy to search as the web, and as reliable as a scholarly archive.

- 🔎 **Intuitive discovery:** a fast, modern UI that makes finding resources effortless.
- 🧱 **Robust architecture:** a resilient backend built for reliability and scale.
- 🌍 **Open standards:** built on open principles to serve the global academic community.

## ✨ What it does (planned)

| | Area | For |
|---|---|---|
| 🔎 | Catalog search, facets, new arrivals, recommendations | Patrons |
| 🔄 | Loans, renewals and holds | Patrons |
| 🗃️ | Reading lists (course syllabi, research folios) | Patrons, faculty |
| 💳 | Fines and online payments | Patrons |
| 💾 | Digital resources and journals | Patrons |
| 🛠️ | Cataloging from external metadata providers (Open Library, Google Books, …) | Librarians |
| 🔐 | Password and campus SSO (SAML / OIDC) sign-in | Everyone |

The full list of API groups is in [docs/api-design](docs/api-design/README.md).

---

## 🏗️ Architecture

<p align="center">
  <img src="docs/assets/architecture.svg" alt="OpenShelves architecture: patrons and librarians use the Next.js frontend, which calls the Spring Boot backend API with a bearer token. The backend's layers are security, REST controllers, services and metadata clients. It stores data in PostgreSQL, looks up book metadata from Open Library, Google Books, Hardcover, Comic Vine and RanobeDB, and will later integrate with a campus IdP, payments, email and a library proxy." width="100%">
</p>

- 🔐 **Stateless auth:** the frontend sends an opaque bearer token on every call; the backend resolves it to a session in Postgres.
- 🧠 **One backend, feature packages:** auth, catalog, circulation, fines and cataloging live in one Spring Boot app, split by package.
- 🛡️ **External calls are guarded:** each metadata provider has its own HTTP client with timeouts, rate limiting and retries, so one slow provider can't stall the rest.

## 📁 Repository structure

| Path | What's in it |
|---|---|
| [`backend/`](backend) | REST API: Java 25, Spring Boot 4, PostgreSQL |
| [`frontend/`](frontend) | Web app: Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| [`infra/`](infra) | Infrastructure as Code *(not started)* |
| [`docs/`](docs) | API design, design system, user journeys, proofs of concept |

## 🛠️ Technology stack

| Layer | Stack |
|---|---|
| ⚙️ Backend | Java 25 (Azul) · Spring Boot 4.1 (WebMVC, Data JPA, Security, Validation, Actuator) · Gradle |
| 🗄️ Database | PostgreSQL |
| 🛡️ Resilience & HTTP | Resilience4j · OkHttp 5 |
| 🖥️ Frontend | Next.js 16 (App Router) · React 19 · TypeScript (strict) · Tailwind CSS 4 |
| 🧪 Testing | JUnit 6 (Jupiter) |

---

## 🚀 Getting started

### Prerequisites

- **Java 25** (Azul Zulu recommended)
- **Node.js 26** (pinned in [`frontend/.nvmrc`](frontend/.nvmrc))
- **PostgreSQL** (local install or Docker)

### 1. Database

```bash
createdb openshelves
psql -d openshelves -f backend/src/main/resources/schema.sql
psql -d openshelves -f backend/src/main/resources/seed.sql   # optional sample data
```

Connection settings (URL, user, password) are in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml).

### 2. Backend → http://localhost:8080

```bash
cd backend
./gradlew bootRun
```

Health check: `GET http://localhost:8080/actuator/health`

### 3. Frontend → http://localhost:3000

```bash
cd frontend
npm install
npm run dev
```

More detail (configuration, project layout, conventions) is in [`backend/README.md`](backend/README.md) and [`frontend/README.md`](frontend/README.md).

---

## 📚 Documentation

| | Doc | What's in it |
|---|---|---|
| 🔌 | [API design](docs/api-design/README.md) | Every API group, endpoint and convention |
| 🎨 | [Design system](docs/Design%20System.md) | Colours, typography, components |
| 🖥️ | [Frontend architecture](docs/Frontend%20Architecture.md) | Folder structure, data flow, auth, API client |
| 🧭 | [User journey maps](docs/User%20Journey%20Maps.pdf) | How patrons and librarians move through the product |
| 🧪 | [Demo site](docs/poc/demo-site/README.md) | Frontend-only prototype the UI is based on |
| 🤖 | [AGENTS.md](AGENTS.md) | Instructions for AI coding assistants |

## 🤝 Contributing

We're looking for early contributors, library enthusiasts and anyone who cares about open academic tools.

- Start with [CONTRIBUTING.md](CONTRIBUTING.md).
- See [GOVERNANCE.md](GOVERNANCE.md) for how the project is run.
- Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## 📄 Policies

- [AI Usage Policy](AI_USAGE_POLICY.md)
- [Security Policy](SECURITY.md)
- [Anti-Impersonation Policy](ANTI-IMPERSONATION.md)
- [Contributor License Agreement](CLA.md)
- [License: Business Source License 1.1](LICENSE)

---

© 2026 OpenShelves Labs. Built with ❤️ for the academic community.
