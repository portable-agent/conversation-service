# Архитектура

```text
controller → service → repository → PostgreSQL
                 ↓
          Agent и Action clients
```

- Controller переводит generated HTTP model в простой service command.
- Service управляет одним сообщением, временем жизни диалога и транзакцией.
- Repository использует generated jOOQ и только свою PostgreSQL-базу.
- Agent client получает предложение, Action client создаёт действие.

## Хранение

- `conversations` хранит владельца, статус и срок жизни диалога.
- `conversation_messages` хранит входное сообщение и ключ повтора.
- уникальный ключ `(tenant_id, subject, request_key)` защищает от дубля даже при параллельных запросах;
- при закрытии или истечении 24 часов `message_text` становится `NULL`;
- технические поля остаются для идемпотентности. Их общий срок хранения будет принят отдельным ADR.

HTTP endpoint и clients появятся следующим пакетом после проверки этого слоя.
