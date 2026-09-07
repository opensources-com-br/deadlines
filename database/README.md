# Deadlines Database

This directory contains the shared PostgreSQL schema resources for Deadlines. The schema is managed exclusively through versioned Flyway migrations; the application does not create or update tables automatically through an ORM.

## Structure

```text
database/
├── migrations/   versioned schema and data migrations
└── seed/          explicit development or bootstrap data
```

## Requirements

- PostgreSQL 16
- Docker for the default local setup

## Local development

From the repository root, create the local environment file and start PostgreSQL:

```bash
cp .env.example .env
docker compose up -d postgres
```

Check the container status:

```bash
docker compose ps postgres
```

Connect to the local database with `psql` inside the container:

```bash
docker compose exec postgres psql -U deadlines -d deadlines
```

The default local connection is available only on `127.0.0.1:5432`.

## Applying migrations

Flyway applies pending migrations when the backend starts. Start the backend and PostgreSQL together with:

```bash
docker compose up -d --build backend
```

For local Gradle development, start PostgreSQL and run the backend from its directory:

```bash
cd backend
set -a
source ../.env
set +a
./gradlew run
```

The default migration location is `filesystem:../database/migrations` and can be changed with `MIGRATIONS_LOCATION`.

## Migration history

| Version | Migration | Purpose |
| --- | --- | --- |
| `V001` | `baseline` | Establishes the Flyway baseline |
| `V002` | `create_users` | Creates users and user profiles |
| `V003` | `add_auth_credentials_and_sessions` | Adds password credentials and refresh sessions |
| `V004` | `add_email_verification_and_password_reset` | Adds verification and password-reset tokens |
| `V005` | `create_organizations_and_memberships` | Creates organizations and user memberships |
| `V006` | `create_roles_and_permissions` | Adds organization roles and permissions |
| `V007` | `provision_default_organization_roles` | Creates protected Owner and Member roles automatically |
| `V008` | `grant_custom_permissions_to_owner` | Grants new custom permissions to the Owner role |
| `V009` | `create_members_and_invitations` | Adds role-backed memberships and invitations |
| `V010` | `enforce_organization_role_scope` | Enforces organization boundaries for assigned roles |
| `V011` | `create_organization_audit_logs` | Adds immutable organization audit history and triggers |
| `V012` | `create_plan_catalog` | Creates plans and resource limits |
| `V013` | `activate_free_plan_only` | Keeps only the Free plan active |
| `V014` | `create_organization_subscriptions` | Adds organization subscriptions and Free-plan provisioning |
| `V015` | `add_device_identity_to_sessions` | Reuses one refresh session per user device |
| `V016` | `create_user_preferences` | Stores locale, timezone, and theme preferences |
| `V017` | `define_platform_permissions` | Adds granular permissions for platform operations |

## Schema areas

### Identity and sessions

The identity schema stores users, profiles, password hashes, email-verification tokens, password-reset tokens, and refresh sessions. Sensitive tokens are persisted only as SHA-256 hashes. Revoked and expired sessions remain available for lifecycle checks but are excluded from active-session queries.

### Organizations and access control

Organizations contain memberships, roles, permissions, role-permission assignments, and invitations. Database constraints prevent a role from another organization from being assigned to a membership or invitation.

Each new organization receives protected `Owner` and `Member` roles through a database trigger. New custom permissions are automatically granted to the organization owner. Platform permissions are granular, and legacy management grants are expanded during migration so existing custom roles retain their capabilities.

### Audit history

Organization changes are recorded by database triggers in `organization_audit_logs` within the same transaction as the source operation. Audit records retain resource identifiers after deletion and intentionally avoid copying names, descriptions, email addresses, passwords, or tokens into metadata.

Normal `UPDATE`, `DELETE`, and `TRUNCATE` operations against the audit table are blocked to preserve history.

### Plans and subscriptions

The plan catalog stores pricing metadata and resource limits. A limit value of `-1` represents unlimited usage. Free, Pro, and Business plans exist in the catalog, but only Free is currently active.

Every organization receives an active Free subscription. Existing organizations were backfilled when the subscription schema was introduced, and new organizations are provisioned through a trigger.

## Creating a migration

Add migrations to `database/migrations` using Flyway's versioned naming format:

```text
V015__describe_the_change.sql
```

Follow these rules:

- Never edit or reorder a migration that has already been applied.
- Use the next available version number.
- Keep schema constraints and indexes close to the objects they protect.
- Use explicit data migrations when existing rows need to be updated.
- Make destructive changes deliberate and document their migration path.
- Keep secrets and personal data out of migrations and seed files.

## Validation

The backend test suite validates migrations against a disposable PostgreSQL database through Testcontainers:

```bash
cd backend
./gradlew build
```

Docker must be available for the integration tests.

## Seed data

The [`seed`](seed) directory is reserved for explicit development or bootstrap data. Seed files must be deterministic, safe to rerun when documented as such, and free of secrets or personal information.

For backend setup and configuration, see the [backend README](../backend/README.md). For the implemented API contract, see [`openapi/openapi.yaml`](../openapi/openapi.yaml).
