# Postman

Importe `deadlines.postman_collection.json` no Postman e inicie a API localmente:

```bash
docker compose up -d --build backend
```

A collection usa `http://localhost:8080` por padrão e não contém credenciais reais. Antes de executar `Register`, defina `userEmail` na aba **Variables** da collection; esse valor não será sobrescrito. A senha inicial é `postman-password-123` e pode ser alterada em `userPassword`.

Execute a collection completa ou as pastas na ordem apresentada. Os scripts armazenam automaticamente access token, refresh token e IDs entre as requisições.

A pasta `Email (local)` valida o reenvio autenticado e a solicitação de recuperação. Por segurança, o backend nunca expõe nem registra os tokens de confirmação/redefinição. Com o Resend configurado, copie o token recebido no e-mail para `emailToken` e ative a requisição manual desejada.

As rotas em `Users (local only)` permanecem abertas somente durante o desenvolvimento local da Identity.
