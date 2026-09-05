# Postman

Importe `deadlines.postman_collection.json` no Postman e inicie a API localmente:

```bash
docker compose up -d --build backend
```

A collection usa `http://localhost:8080` por padrão e não contém credenciais reais. Execute a collection completa ou as pastas na ordem apresentada; os scripts geram e-mails únicos e armazenam automaticamente access token, refresh token e IDs entre as requisições.

As rotas em `Users (local only)` permanecem abertas somente durante o desenvolvimento local da Identity.
