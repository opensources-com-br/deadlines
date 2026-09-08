# opensources

opensources is a reusable SaaS foundation for identity, one-organization accounts, teams, access control, auditing, and preferences. Product-specific capabilities are independent optional modules.

## Project structure

```text
opensources/
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
- `backend/`: core identity and organization foundation plus optional-module composition.
- `database/`: core and optional-module Flyway migrations.
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

See [extension contracts](docs/EXTENDING.md) to configure a product, add a module, permission, migration, or application area.
