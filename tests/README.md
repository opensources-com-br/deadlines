# Tests

Testes que atravessam os limites de uma aplicação ficam neste diretório.

- `integration/`: integração entre backend, banco e demais infraestruturas.
- `e2e/`: fluxos completos do produto.
- `postman/`: collection executável para validação manual da API local.

Testes unitários permanecem próximos ao código de cada aplicação.

## Fase 8 — Auditoria

Validação automatizada: `cd backend && ./gradlew build` (95 testes, incluindo
PostgreSQL descartável via Testcontainers), `cd web && npm run lint && npm run build`.
Os testes de auditoria cobrem Owner/Member/sem organização, autenticação, filtros,
isolamento por organização, paginação, autor, metadados permitidos, rollback,
proteção contra UPDATE/DELETE/TRUNCATE e alterações reais de role-permission.

Roteiro manual local:
1. Entre como Owner e abra **Organization history → View history** em `/app`.
2. Altere o nome da organização, volte ao histórico e clique em **Refresh**.
3. Confira ação, data, autor e ID do recurso em **Event details**.
4. Filtre por ação, autor/recurso e período; verifique **Clear** e a paginação.
5. Crie/edite/exclua uma role de teste e altere suas permissões; confirme os eventos.
6. Entre como Member: o card deve ficar oculto e a API de auditoria deve retornar 403.

O histórico começa na aplicação da V011; dados antigos não são reconstruídos.
Testes de convite que enviam e-mail dependem do destinatário autorizado no Resend.

## Fase 10 — Assinaturas

A V014 testa a criação da estrutura de assinatura e o backend cobre a consulta
autenticada da assinatura atual. No roteiro manual, crie ou use uma organização,
execute `Subscriptions → Get current subscription` no Postman e confirme o
status `active` e o plano `free`.
