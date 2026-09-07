# Deadlines

Deadlines is a multi-tenant SaaS platform that brings CRM and ERP capabilities together in one place. The product is being built incrementally, with each phase validated before the next one begins.

## Project structure

```text
deadlines/
├── web/         Next.js web application
├── mobile/      reserved for the future mobile application
├── backend/     Kotlin and Ktor API
├── database/    database migrations and seed data
├── openapi/     API contract
└── tests/       integration and end-to-end tests
```

Each directory includes its own README with more specific setup instructions and a description of its responsibilities.

## Current status

- `web/`: Next.js application with authentication and the initial platform experience.
- `mobile/`: reserved structure; implementation has not started yet.
- `backend/`: Phase 10, covering identity, organizations, access control, audit logs, plans, and Free subscriptions.
- `database/`: Flyway migrations for identity, organization isolation, auditing, plans, and subscriptions.
- `openapi/`: OpenAPI 3.1 contract for the implemented endpoints.
- `tests/`: unit, HTTP, and PostgreSQL integration tests.

## Local development

During local development, the backend and database are exposed only on `127.0.0.1`.

Create the local environment file, then start PostgreSQL and the backend:

```bash
cp .env.example .env
docker compose up -d --build backend
```

Verify that the backend is running:

```bash
curl http://localhost:8080/health
```

Install the web dependencies and start the development server:

```bash
cd web
npm ci
npm run dev
```

The web application is available at [http://localhost:3000](http://localhost:3000).

See the README inside each directory for component-specific instructions. The complete API contract is available in [`openapi/openapi.yaml`](openapi/openapi.yaml).
