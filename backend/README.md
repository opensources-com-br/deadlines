# Backend

Backend Kotlin + Ktor do Deadlines. A Fase 3 adiciona confirmação de e-mail e recuperação de senha ao fluxo de autenticação.

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
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
POST   /api/v1/auth/email/verify
POST   /api/v1/auth/email/resend
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/users/me
PATCH  /api/v1/users/me
POST   /api/v1/users
GET    /api/v1/users?page=1&limit=20
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

`POST /api/v1/auth/register` cria uma conta com status `pending`, envia a confirmação e não retorna tokens. A confirmação ativa a conta; só então login e JWT ficam disponíveis. `POST /api/v1/auth/email/resend` é público e sempre retorna sucesso, evitando revelar se o e-mail está cadastrado. Tokens de confirmação e redefinição são armazenados somente como hash, expiram e só podem ser usados uma vez; uma redefinição também revoga todas as sessões do usuário.

Por padrão, o ambiente local registra o envio sem incluir o token no log. Ao definir `RESEND_API_KEY`, o backend seleciona o Resend automaticamente. Para o teste inicial, use `MAIL_FROM="Deadlines <onboarding@resend.dev>"`; o Resend só entregará para o e-mail da própria conta. Em produção, use `EMAIL_PROVIDER=resend`, um domínio verificado e `EMAIL_FROM` nesse domínio. As rotas de usuários continuam sem autenticação durante o desenvolvimento local da Identity. O Compose publica o backend somente em `127.0.0.1`; não faça deploy desta fase.

Exemplo de criação:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"tarik@example.com","firstName":"Tarik","lastName":"Villalobos"}'
```
