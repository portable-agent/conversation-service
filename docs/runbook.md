# Runbook

## Обязательные параметры

- `DB_URL`, `DB_USER`, `DB_PASSWORD` — PostgreSQL;
- `OIDC_ISSUER`, `OIDC_JWKS_URL`, `OIDC_AUDIENCE` — проверка JWT;
- `AGENT_URL`, `ACTION_URL` — адреса зависимостей;
- `REMOTE_CONNECT_TIMEOUT`, `REMOTE_READ_TIMEOUT` — сетевые timeout в ISO-8601 duration.

Для production `OIDC_AUDIENCE` должен быть `conversation-service`. Значение по умолчанию подходит для
локального запуска, но issuer и JWKS URL всегда задаются явно.

## Generated API не собирается

Проверьте версию в `src/main/openapi/README.md`, затем запустите `openApiGenerate`. Не исправляйте
generated Java вручную.

## Приложение не стартует

Проверьте `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER` и `OIDC_JWKS_URL`. Секреты не имеют
значений по умолчанию.

## Текст не удаляется после срока диалога

По умолчанию диалог открыт 24 часа, а очистка запускается каждые 5 минут пачками по 100 записей.
Проверьте `CONVERSATION_OPEN_TTL`, `CONVERSATION_CLEANUP_DELAY` и
`CONVERSATION_CLEANUP_BATCH_SIZE`. Повторная очистка безопасна.

## Сообщение долго находится в PROCESSING

Lease worker по умолчанию живёт две минуты. После этого следующий запрос может атомарно забрать работу.
Проверьте `CONVERSATION_WORK_TIMEOUT`. Не устанавливайте значение меньше максимального timeout внешнего
вызова, иначе два worker смогут начать одну долгую операцию.
