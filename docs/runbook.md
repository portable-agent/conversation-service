# Runbook

## Generated API не собирается

Проверьте версию в `src/main/openapi/README.md`, затем запустите `openApiGenerate`. Не исправляйте
generated Java вручную.

## Приложение не стартует

Проверьте `DB_URL`, `DB_USER`, `DB_PASSWORD` и `OIDC_ISSUER`. Секреты не имеют значений по умолчанию.
