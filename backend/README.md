# Backend

Backend Kotlin + Ktor do Deadlines. A Fase 7 adiciona membros, roles por membership e convites de organização por e-mail.

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
├── organizations/ organizações e associações de usuários
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
PATCH  /api/v1/auth/password
POST   /api/v1/auth/email/verify
POST   /api/v1/auth/email/resend
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/sessions
DELETE /api/v1/sessions/{sessionId}
POST   /api/v1/sessions/revoke-all
POST   /api/v1/organizations
GET    /api/v1/organizations/current
PATCH  /api/v1/organizations/current
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
GET    /api/v1/members
GET    /api/v1/members/{memberId}
PATCH  /api/v1/members/{memberId}
DELETE /api/v1/members/{memberId}
GET    /api/v1/invitations
POST   /api/v1/invitations
GET    /api/v1/invitations/preview?token={token}
POST   /api/v1/invitations/accept
GET    /api/v1/invitations/{invitationId}
POST   /api/v1/invitations/{invitationId}/resend
DELETE /api/v1/invitations/{invitationId}
GET    /api/v1/users/me
PATCH  /api/v1/users/me
POST   /api/v1/users
GET    /api/v1/users?page=1&limit=20
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

`POST /api/v1/auth/register` cria uma conta com status `pending`, envia a confirmação e não retorna tokens. A confirmação ativa a conta; só então login e JWT ficam disponíveis. `POST /api/v1/auth/email/resend` é público e sempre retorna sucesso, evitando revelar se o e-mail está cadastrado. Tokens de confirmação e redefinição são armazenados somente como hash, expiram e só podem ser usados uma vez; uma redefinição também revoga todas as sessões do usuário.

As rotas de sessões exigem JWT. Cada novo access token contém o identificador da sessão (`sid`), permitindo marcar a sessão atual. Revogar uma sessão invalida imediatamente seu refresh token; um access token já emitido continua válido até expirar.

Depois de confirmar o e-mail e entrar, um usuário sem associação cria sua organização em `POST /api/v1/organizations`. A criação da organização e da associação como `owner` ocorre na mesma transação. O banco impede que um usuário tenha mais de uma associação ativa. Somente o proprietário pode atualizar a organização atual.

Cada organização recebe automaticamente as roles protegidas `Owner` e `Member`. O proprietário pode criar, editar e excluir roles e permissões customizadas, além de substituir as permissões de qualquer role exceto `Owner`. Permissões globais e roles do sistema podem ser consultadas, mas não alteradas ou excluídas. Todas as consultas são limitadas à organização atual.

O proprietário pode convidar um e-mail escolhendo qualquer role da organização, exceto `Owner`. O convite expira em sete dias por padrão, pode ser reenviado ou revogado e armazena somente o hash do token. A conta autenticada precisa possuir o mesmo e-mail do convite. Um usuário pode receber convites de várias organizações, mas o banco permite somente uma membership ativa; para entrar em outra organização, ele precisa primeiro ser removido da atual. O proprietário nunca pode ser removido ou ter sua role substituída.

Por padrão, o ambiente local registra o envio sem incluir o token no log. Ao definir `RESEND_API_KEY`, o backend seleciona o Resend automaticamente. Para o teste inicial, use `MAIL_FROM="Deadlines <onboarding@resend.dev>"`; o Resend só entregará para o e-mail da própria conta. Em produção, use `EMAIL_PROVIDER=resend`, um domínio verificado e `EMAIL_FROM` nesse domínio. As rotas de usuários continuam sem autenticação durante o desenvolvimento local da Identity. O Compose publica o backend somente em `127.0.0.1`; não faça deploy desta fase.

Exemplo de criação:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"tarik@example.com","firstName":"Tarik","lastName":"Villalobos"}'
```
