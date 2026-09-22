# Архитектура

```text
controller → service → repository → PostgreSQL
                 ↓
          Agent и Action clients
```

- Controller переводит generated HTTP model в простой service command.
- Service управляет одним сообщением и временем жизни диалога. Транзакции ограничены отдельными
  операциями хранения и lease.
- Repository использует generated jOOQ и только свою PostgreSQL-базу.
- Agent client получает предложение, Action client создаёт действие.

## Хранение

- `conversations` хранит владельца, статус и срок жизни диалога.
- `conversation_messages` хранит входное сообщение и ключ повтора.
- уникальный ключ `(tenant_id, subject, request_key)` защищает от дубля даже при параллельных запросах;
- при закрытии или истечении 24 часов `message_text` становится `NULL`;
- временный ответ очищается одновременно с `message_text`;
- технические поля остаются для идемпотентности. Их общий срок хранения будет принят отдельным ADR.

## Надёжная обработка

```text
NEW -> PROCESSING -> READY
          |
          v
        FAILED -> PROCESSING

NEW / PROCESSING / READY / FAILED -> ERASED
```

Worker сначала атомарно переводит сообщение в `PROCESSING`, а после внешнего вызова отдельной короткой
транзакцией сохраняет `READY` или `FAILED`. Сетевая операция никогда не держит транзакцию базы.

Если процесс остановился, lease истекает через две минуты и другой worker может продолжить работу.
Одновременный SQL `UPDATE` позволяет начать только одному worker. Каждый lease имеет случайный token:
старый worker не может перезаписать результат после повторного захвата. Состояние `ERASED` конечное:
оно не запускает внешние сервисы повторно.

## Сценарий сообщения

```text
store message -> claim lease -> Agent
                              |-> question -> save TEXT
                              `-> proposal -> Action -> card strategy -> save CONFIRMATION
```

`MessageFlowService` не открывает транзакцию. `MessageService.store`, `MessageWorkService.start`,
`complete` и `fail` выполняются отдельными короткими транзакциями. Agent и Action представлены портами,
поэтому generated HTTP types не протекают в application-логику.

Карточки выбираются по `kind` через map стратегий. Сейчас разрешён только
`calendar.create_event`. Карточка использует payload, возвращённый после сохранения Action Service, и
передаёт `actionId` с `payloadHash` для последующего подтверждения.

## HTTP-граница

`MessageController` реализует generated `MessagesApi` и вызывает один метод `MessageFlowService`.
`tenant_id`, `sub` и исходный bearer token берутся из JWT, поэтому канал не может подменить владельца
данными request body. Проверяются подпись, issuer и audience `conversation-service`.

`RestAgentClient` и `RestActionClient` используют generated transport-модели contracts `2.3.0`, но
переводят их во внутренние `Proposal` и `SavedAction`. URL и timeout задаются environment. Ошибка сети
или некорректный ответ превращаются в безопасный код состояния без сохранения текста exception.

OpenAPI Generator 7.24 некорректно генерирует Java для boolean `const`. Поэтому только временная
codegen-копия Agent API в `build/` теряет это ограничение, а адаптер обязательно проверяет
`requiresApproval == true`. Исходный snapshot остаётся неизменным.
