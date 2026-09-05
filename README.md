# Deadlines

O Deadlines será um SaaS multi-tenant para reunir CRM e ERP em uma única plataforma. O produto é construído incrementalmente, com cada fase validada antes da próxima.

## Estrutura

```text
deadlines/
├── web/         aplicação web existente
├── mobile/      aplicação mobile futura
├── backend/     aplicação Kotlin + Ktor
├── database/    migrations e seeds
├── openapi/     contrato da API
└── tests/       testes de integração e ponta a ponta
```

Cada diretório contém seu próprio README com o limite de responsabilidade correspondente.

## Estado atual

- `web/`: aplicação Next.js existente.
- `mobile/`: estrutura reservada, sem implementação.
- `backend/`: Fase 1 com CRUD local de usuários, ainda sem autenticação.
- `database/`: migrations do Flyway para a fundação e usuários.
- `openapi/`: contrato OpenAPI 3.1 dos endpoints implementados.
- `tests/`: testes unitários, HTTP e de integração com PostgreSQL.

## Desenvolvimento local

O backend e o banco de dados são publicados somente em `127.0.0.1` durante o desenvolvimento da Identity.

Crie o arquivo local de ambiente e inicie PostgreSQL e backend:

```bash
cp .env.example .env
docker compose up -d --build backend
```

Verifique o backend:

```bash
curl http://localhost:8080/health
```

Para executar a aplicação web:

```bash
cd web
npm ci
npm run dev
```

Consulte os READMEs de cada aplicação para instruções específicas.
