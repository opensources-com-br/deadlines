# Migrations

Migrations versionadas do PostgreSQL, executadas pelo Flyway durante a inicialização do backend.

Regras:

- migrations aplicadas nunca devem ser alteradas;
- mudanças posteriores recebem um novo número sequencial;
- tabelas não são criadas automaticamente pela aplicação;
- cada migration pertence à fase ou feature que introduz o respectivo schema.
