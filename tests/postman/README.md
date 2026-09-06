# Postman

Importe `deadlines.postman_collection.json` no Postman e inicie a API localmente:

```bash
docker compose up -d --build backend
```

A collection usa `http://localhost:8080` por padrão e não contém credenciais reais. Antes de executar `Register`, defina `userEmail` na aba **Variables** da collection; esse valor não será sobrescrito. A senha inicial é `postman-password-123` e pode ser alterada em `userPassword`.

O fluxo de cadastro é: defina `userEmail`, execute `Register`, copie o token do e-mail do Resend para `emailToken`, ative `Verify email (manual token)` e execute-a. Depois ative e execute `Login`; os tokens passam a ser armazenados automaticamente para `Me`, `Refresh`, `Logout` e a pasta `Sessions`.

A pasta `Email (local)` valida o reenvio público e a solicitação de recuperação. Por segurança, o backend nunca expõe nem registra os tokens de confirmação/redefinição.

A pasta `Sessions` lista as sessões ativas e guarda uma sessão não atual em `sessionId`, quando houver. `Revoke session` e `Revoke all sessions` ficam desativadas por padrão para evitar encerrar sessões sem intenção. Depois de revogada, uma sessão não renova mais seu access token; o token já emitido permanece válido até expirar.

As rotas em `Users (local only)` permanecem abertas somente durante o desenvolvimento local da Identity.
