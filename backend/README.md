# Backend

Backend Kotlin + Ktor do Deadlines. A Fase 1 adiciona o CRUD local de usuários; autenticação e autorização entram nas próximas fases.

## Stack da Fase 0

- Kotlin e Ktor
- PostgreSQL
- Exposed JDBC
- HikariCP
- Flyway
- kotlinx.serialization
- JUnit 5 e Testcontainers

## Estrutura

```text
src/main/kotlin/deadlines/
├── application/  bootstrap, plugins e rotas
├── config/       configuração tipada por ambiente
├── identity/     identidade, começando por usuários
└── shared/       infraestrutura compartilhada
```

As próximas features serão organizadas por módulo, mantendo modelo, DTOs, serviço, repository, rotas e erros próximos entre si.

## Executar localmente

Inicie o PostgreSQL na raiz do repositório:

```bash
docker compose up -d postgres
```

Carregue o ambiente e execute o backend:

```bash
set -a
source ../.env
set +a
./gradlew run
```

O backend estará disponível em `http://localhost:8080`.

## Testar

```bash
./gradlew build
```

Com Docker disponível, o build também valida as migrations em um PostgreSQL descartável via Testcontainers.

## Endpoints atuais

```text
GET /health
POST   /api/v1/users
GET    /api/v1/users?page=1&limit=20
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

As rotas de usuários estão sem autenticação durante o desenvolvimento local da Identity. O Compose publica o backend somente em `127.0.0.1`; não faça deploy desta fase.

Exemplo de criação:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"tarik@example.com","firstName":"Tarik","lastName":"Villalobos"}'
```
