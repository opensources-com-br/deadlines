# Backend

Backend Kotlin + Ktor do Deadlines. A Fase 9 adiciona o catálogo global de planos e seus limites.

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
GET    /api/v1/plans
GET    /api/v1/subscriptions/current
GET    /api/v1/audits
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

## Auditoria (Fase 8)

`GET /api/v1/audits` usa a organização ativa do usuário autenticado e exige Owner.
Aceita `offset` (0–1.000.000), `limit` (1–100, padrão 20), `action`, `resource`,
`actorId`, `resourceId`, `from` e `to` (ISO-8601, anos 0001–9999, limites inclusivos). Retorna
`data`, `offset`, `limit` e `hasMore`, em ordem decrescente por data e ID.
Parâmetros desconhecidos, repetidos ou inválidos retornam 422.

A migration V011 grava eventos por triggers na mesma transação da alteração.
Os serviços propagam `AuditActor` pelo contexto de corrotina; `DatabaseQuery`
configura o autor apenas durante a transação. Escritas diretas de manutenção
ficam com `actorId` nulo no banco e omitido no JSON. Não existe backfill de ações anteriores à migration.

O histórico inclui alterações de organização, membros, convites, roles,
permissions e associações role-permission. Metadados guardam somente IDs e
indicadores de campos alterados; nomes, descrições, e-mails, senhas e tokens
não são copiados. Recursos removidos mantêm seu histórico. UPDATE, DELETE e
TRUNCATE do histórico são bloqueados no banco; não há endpoints de escrita.
Administradores capazes de desabilitar triggers/alterar o schema continuam
fora dessa garantia: separação de credenciais operacionais fica para produção.

Eventos de convite representam persistência: `created` e `resent` não confirmam
entrega de e-mail. Uma criação cuja entrega falha registra também a revogação
automática. Uma renovação com falha de entrega mantém o evento de renovação.
Na exclusão de uma role, `role.deleted` representa a remoção da role e de suas
associações. Salvar a mesma seleção de permissões não gera eventos adicionais.

Na web, Owner encontra **Organization history → View history** em `/app`.
Use **Refresh** para buscar os eventos mais recentes. A paginação por offset
pode se deslocar se novos eventos forem gravados durante a navegação.

## Catálogo de planos (Fase 9)

`GET /api/v1/plans` é público e retorna somente planos ativos, ordenados para
exibição, incluindo preço mensal em centavos, moeda e limites por recurso.
O valor `-1` em um limite significa ilimitado. A migration V012 inicia o
catálogo com `Free`, `Pro` e `Business` e limites para `members`, `projects`
e `deadlines`. Durante esta fase inicial, apenas o `Free` está ativo e é
retornado pela API; os outros permanecem armazenados para ativação futura.

O catálogo, por si só, não bloqueia uso pelos limites. A associação inicial
ao Free é descrita na Fase 10 abaixo; escolha de plano, cobrança e enforcement
continuam para fases futuras.

## Assinaturas (Fase 10)

Toda organização possui uma assinatura ativa. A migration V014 associa as
organizações existentes ao `Free` e um trigger cria essa mesma assinatura na
transação de toda nova organização. `GET /api/v1/subscriptions/current` exige
JWT e retorna a assinatura e seu plano para a organização ativa do usuário.

No momento, não existem checkout, cobrança, teste, upgrade, downgrade,
cancelamento ou bloqueio dos limites. O plano Free é a única opção ativa.
