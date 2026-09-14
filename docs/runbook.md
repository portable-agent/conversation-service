# Runbook

## Generated API не собирается

Проверьте версию в `src/main/openapi/README.md`, затем запустите `openApiGenerate`. Не исправляйте
generated Java вручную.

## Приложение не стартует

Проверьте `DB_URL`, `DB_USER`, `DB_PASSWORD` и `OIDC_ISSUER`. Секреты не имеют значений по умолчанию.

## Текст не удаляется после срока диалога

По умолчанию диалог открыт 24 часа, а очистка запускается каждые 5 минут пачками по 100 записей.
Проверьте `CONVERSATION_OPEN_TTL`, `CONVERSATION_CLEANUP_DELAY` и
`CONVERSATION_CLEANUP_BATCH_SIZE`. Повторная очистка безопасна.
