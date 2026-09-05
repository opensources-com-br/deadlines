# Backend

Backend Kotlin + Ktor do Deadlines. A Fase 0 contém apenas a fundação técnica; Identity e módulos de negócio ainda não foram implementados.

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

## Endpoint atual

```text
GET /health
```
