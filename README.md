# Deadlines

O Deadlines será um SaaS multi-tenant para reunir CRM e ERP em uma única plataforma. O produto será construído incrementalmente; neste momento, somente a aplicação web já está implementada.

## Estrutura

```text
deadlines/
├── web/         aplicação web existente
├── mobile/      aplicação mobile futura
├── backend/     backend futuro
├── database/    migrations e seeds
├── openapi/     contrato da API
└── tests/       testes de integração e ponta a ponta
```

Cada diretório contém seu próprio README com o limite de responsabilidade correspondente.

## Estado atual

- `web/`: aplicação Next.js existente.
- `mobile/`: estrutura reservada, sem implementação.
- `backend/`: estrutura reservada, sem implementação.
- `database/`: estrutura preparada para migrations e seeds futuros.
- `openapi/`: estrutura preparada para o contrato OpenAPI 3.1.
- `tests/`: estrutura preparada para testes entre aplicações.

## Desenvolvimento local

Crie o arquivo local de ambiente e inicie o PostgreSQL:

```bash
cp .env.example .env
docker compose up -d postgres
```

Para executar a aplicação web:

```bash
cd web
npm ci
npm run dev
```

As instruções de backend e mobile serão adicionadas quando essas aplicações forem iniciadas.
