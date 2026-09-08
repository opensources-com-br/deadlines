# opensources Web

The opensources web application provides the public site, authentication flows, organization onboarding, and the initial authenticated platform experience.

## Technology

- Next.js 16 with the App Router
- React 19 and TypeScript
- Tailwind CSS 4
- Base UI and shadcn components
- Lucide icons
- next-intl for Portuguese and English translations

## Requirements

- Node.js 22 or newer
- npm
- The opensources backend running locally on port `8080`

## Local development

Start PostgreSQL and the backend from the repository root:

```bash
cp .env.example .env
docker compose up -d --build backend
```

Install the frontend dependencies and start Next.js:

```bash
cd web
npm ci
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Environment variables

The server-side API handlers use `BACKEND_API_URL` to reach the Kotlin API. It defaults to `http://localhost:8080` during local development.

```bash
BACKEND_API_URL=http://localhost:8080
```

The browser sends requests only to same-origin `/api/*` handlers. Those handlers forward authenticated requests to the backend, keeping access and refresh tokens in HTTP-only cookies and avoiding direct browser-to-backend communication.

## Available commands

```bash
npm run dev     # Start the development server
npm run build   # Create a production build
npm run start   # Run the production build
npm run lint    # Run ESLint
npm run i18n:check # Verify that translation catalogs have matching keys
```

## Internationalization

The interface supports Brazilian Portuguese (`pt-BR`) and English (`en`). Public pages use the locale cookie, while authenticated users also persist their language, timezone, and theme preferences in their account. Signing in on another device restores those saved preferences.

Translation messages live in `messages/pt-BR.json` and `messages/en.json`. Run `npm run i18n:check` after changing either catalog to prevent missing or orphaned translations.

## Application areas

- `/`: public landing page
- `/login` and `/register`: authentication
- `/forgot-password` and `/reset-password`: password recovery
- `/verify-email`: email verification
- `/onboarding/organization`: organization setup
- `/app`: authenticated platform home
- `/app/settings`: organization and account settings

Authenticated platform routes validate the access token on the server. Logging out revokes the refresh session, removes the authentication cookies, and prevents protected pages from being restored from browser history.

The authenticated settings shell loads the user's effective organization permissions on the server. UI sections and actions use permission guards for visibility, while the backend independently authorizes every protected operation.

## Project structure

```text
web/
├── app/          routes, layouts, and server-side API handlers
├── components/   shared UI components
├── features/     feature-specific domain, infrastructure, and presentation code
├── hooks/        shared React hooks
├── lib/          shared utilities
└── public/       static assets
```

Feature code is grouped by responsibility:

```text
features/<feature>/
├── domain/          types and business models
├── infrastructure/  API clients and request helpers
└── presentation/    screens and UI components
```

## Production

The application uses Next.js standalone output. Build it with:

```bash
npm run build
npm run start
```

Set `BACKEND_API_URL` to the reachable production API before starting the server.

For repository-wide setup, backend documentation, and the OpenAPI contract, see the [main project README](../README.md).
