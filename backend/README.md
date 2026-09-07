# Deadlines Backend

The Deadlines backend is a Kotlin and Ktor API for identity, organizations, access control, audit history, plans, and subscriptions.

## Technology

- Kotlin with JVM 21
- Ktor and Netty
- PostgreSQL
- Exposed JDBC and HikariCP
- Flyway database migrations
- kotlinx.serialization
- JUnit 5 and Testcontainers

## Project structure

```text
backend/
├── src/main/kotlin/deadlines/
│   ├── application/     application bootstrap, plugins, and route registration
│   ├── config/          typed environment configuration
│   ├── identity/        authentication, users, email verification, and sessions
│   ├── organizations/   organizations, members, invitations, access, and audits
│   ├── plans/           global plan catalog
│   ├── subscriptions/   organization subscriptions
│   └── shared/          shared database and error infrastructure
├── src/test/            unit and HTTP tests
└── build.gradle.kts     Gradle build configuration
```

Each feature keeps its models, DTOs, services, repositories, routes, and errors close together.

## Requirements

- Java 21
- Docker for the local PostgreSQL database and Testcontainers

The Gradle wrapper is included, so a global Gradle installation is not required.

## Local development

From the repository root, create the environment file and start PostgreSQL:

```bash
cp .env.example .env
docker compose up -d postgres
```

Then start the backend:

```bash
cd backend
set -a
source ../.env
set +a
./gradlew run
```

The API is available at [http://localhost:8080](http://localhost:8080). Verify it with:

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"status":"ok"}
```

You can also build and run the backend container from the repository root:

```bash
docker compose up -d --build backend
```

## Configuration

The application reads its configuration from environment variables.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `DATABASE_URL` | Yes | — | PostgreSQL JDBC URL |
| `DATABASE_USER` | Yes | — | Database user |
| `DATABASE_PASSWORD` | Yes | — | Database password |
| `DATABASE_POOL_SIZE` | No | `10` | Maximum connection pool size |
| `MIGRATIONS_LOCATION` | No | `filesystem:../database/migrations` | Flyway migration location |
| `PORT` | No | `8080` | HTTP server port |
| `JWT_SECRET` | Yes | — | JWT signing secret with at least 32 characters |
| `JWT_ISSUER` | No | `deadlines` | JWT issuer |
| `JWT_AUDIENCE` | No | `deadlines-api` | JWT audience |
| `JWT_ACCESS_EXPIRATION_SECONDS` | No | `900` | Access-token lifetime |
| `JWT_REFRESH_EXPIRATION_SECONDS` | No | `2592000` | Refresh-token lifetime |
| `EMAIL_PROVIDER` | No | `logging` | Email provider: `logging` or `resend` |
| `EMAIL_FROM` | No | `no-reply@deadlines.local` | Sender address |
| `RESEND_API_KEY` | For Resend | — | Resend API key |
| `APP_BASE_URL` | No | `http://localhost:3000` | Base URL used in email links |
| `EMAIL_VERIFICATION_EXPIRATION_SECONDS` | No | `86400` | Email-verification token lifetime |
| `PASSWORD_RESET_EXPIRATION_SECONDS` | No | `3600` | Password-reset token lifetime |
| `INVITATION_EXPIRATION_SECONDS` | No | `604800` | Organization invitation lifetime |

See [`.env.example`](../.env.example) for a complete local configuration.

## Database migrations

Flyway applies the migrations from [`database/migrations`](../database/migrations) when the application starts. The schema covers users, credentials, sessions, organizations, memberships, roles, permissions, invitations, audit logs, plans, and subscriptions.

Migration files are immutable after they have been applied. Add a new numbered migration for every schema change.

## Testing

Run the complete backend test suite with:

```bash
./gradlew build
```

The build runs unit and Ktor HTTP tests. When Docker is available, Testcontainers also validates the migrations and PostgreSQL integration behavior against a disposable database.

## API overview

All endpoints are versioned under `/api/v1`, except the health check.

### Health

```text
GET /health
```

### Authentication and identity

```text
POST  /api/v1/auth/register
POST  /api/v1/auth/login
POST  /api/v1/auth/refresh
POST  /api/v1/auth/logout
GET   /api/v1/auth/me
PATCH /api/v1/auth/password
POST  /api/v1/auth/email/verify
POST  /api/v1/auth/email/resend
POST  /api/v1/auth/forgot-password
POST  /api/v1/auth/reset-password
GET   /api/v1/users/me
PATCH /api/v1/users/me
GET   /api/v1/users/me/preferences
PATCH /api/v1/users/me/preferences
```

Registration creates a pending account and sends an email-verification link. Login becomes available after verification. Verification, reset, refresh, and invitation tokens are stored only as hashes and are single-use where applicable.

Access tokens are JWTs that include the session identifier in the `sid` claim. Clients send a stable `X-Device-Id` UUID when creating or renewing authenticated sessions. Each user has at most one session per device; signing in again or refreshing rotates that device's session in place. Logout revokes the refresh session but does not require the client to discard its device identifier. An already-issued access token remains valid until its short expiration time.

User preferences persist locale, IANA timezone, and theme on the account. The web client mirrors them locally for immediate rendering and restores them from the account after login, so the same preferences follow the user across devices.

### Sessions

```text
GET    /api/v1/sessions
DELETE /api/v1/sessions/{sessionId}
POST   /api/v1/sessions/revoke-all
```

Session routes require authentication. Password resets revoke all active sessions for the user.

### Organizations and team

```text
POST  /api/v1/organizations
GET   /api/v1/organizations/current
PATCH /api/v1/organizations/current
GET   /api/v1/members
GET   /api/v1/members/{memberId}
PATCH /api/v1/members/{memberId}
DELETE /api/v1/members/{memberId}
GET   /api/v1/invitations
POST  /api/v1/invitations
GET   /api/v1/invitations/preview?token={token}
POST  /api/v1/invitations/accept
GET   /api/v1/invitations/{invitationId}
POST  /api/v1/invitations/{invitationId}/resend
DELETE /api/v1/invitations/{invitationId}
```

Creating an organization also creates the owner's membership and Free subscription in the same transaction. Each user can have only one active organization membership. Access to organization operations is determined by the permissions assigned to the membership role.

Invitations expire after seven days by default. Invitation tokens are stored as hashes, and the authenticated account must use the invited email address.

### Access control

```text
GET    /api/v1/permissions
POST   /api/v1/permissions
GET    /api/v1/permissions/{permissionId}
PATCH  /api/v1/permissions/{permissionId}
DELETE /api/v1/permissions/{permissionId}
GET    /api/v1/roles
POST   /api/v1/roles
GET    /api/v1/roles/{roleId}
PATCH  /api/v1/roles/{roleId}
DELETE /api/v1/roles/{roleId}
GET    /api/v1/roles/{roleId}/permissions
PUT    /api/v1/roles/{roleId}/permissions
```

Every organization receives protected `Owner` and `Member` roles. The Owner receives every platform permission, while custom roles can receive independent read, create, update, and delete capabilities. System roles and global permissions remain read-only.

The authenticated authorization context is available at:

```text
GET /api/v1/users/me/authorization
```

It resolves the current `organizationId`, `membershipId`, `roleId`, and effective permission keys directly from the database. Services use the same centralized authorization component, so role changes take effect without issuing a new access token.

### Audit history

```text
GET /api/v1/audits
```

Audit history requires `audit.read` and supports `offset`, `limit`, `action`, `resource`, `actorId`, `resourceId`, `from`, and `to` filters. Events are written by database triggers in the same transaction as the underlying change.

Audit metadata intentionally excludes names, descriptions, email addresses, passwords, and tokens. Historical events cannot be updated, deleted, or truncated through normal database operations.

### Plans and subscriptions

```text
GET /api/v1/plans
GET /api/v1/subscriptions/current
```

The public plan catalog returns active plans and their resource limits. A limit value of `-1` means unlimited. The Free plan is currently the only active option.

Every organization has one active subscription. Checkout, billing, upgrades, downgrades, cancellations, trials, and usage-limit enforcement are reserved for future phases.

## Email delivery

Local development uses the logging email provider by default. It records delivery activity without writing verification or reset tokens to the logs.

Set `EMAIL_PROVIDER=resend`, `RESEND_API_KEY`, and a verified `EMAIL_FROM` address to use Resend. The provider is also selected automatically when a Resend API key is present and `EMAIL_PROVIDER` is omitted.

## API contract

The complete request and response contract is maintained in [`openapi/openapi.yaml`](../openapi/openapi.yaml).

For repository-wide setup and frontend documentation, see the [main project README](../README.md) and the [web README](../web/README.md).
