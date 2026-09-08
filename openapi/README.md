# OpenAPI

O contrato OpenAPI 3.1 da API será mantido neste diretório.

- `openapi.yaml` descreve somente endpoints implementados.
- `/health` é operacional e não recebe versionamento.
- Endpoints da aplicação usam `/api/v1`.
- O cadastro público está restrito a `/api/v1/auth/register`; não há CRUD técnico público de usuários.
