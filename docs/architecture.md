# Архитектура

```text
controller → service → repository → PostgreSQL
                 ↓
      Agent, Action и Connection clients
```

- Controller переводит generated HTTP model в простой service command.
- Service управляет одним сообщением и временем жизни диалога. Транзакции ограничены отдельными
  операциями хранения и lease.
- Repository использует generated jOOQ и только свою PostgreSQL-базу.
- Agent client получает предложение, Action client создаёт действие, Connection client проверяет
  внешний аккаунт и начинает OAuth.

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
                              `-> proposal -> connector strategy
                                              |-> connected -> Action -> save CONFIRMATION
                                              `-> missing -> save CONNECTION without URL
return response -> reply viewer -> add fresh OAuth URL only for CONNECTION
```

`MessageFlowService` не открывает транзакцию. `MessageService.store`, `MessageWorkService.start`,
`complete` и `fail` выполняются отдельными короткими транзакциями. Agent и Action представлены портами,
поэтому generated HTTP types не протекают в application-логику.

Карточки выбираются по `kind` через map стратегий, а обработка предложения — по `connector`.
`fake-calendar` сразу создаёт Action. `google-calendar` сначала требует ровно одно активное
подключение. При его отсутствии в БД сохраняется только provider и безопасный текст. Map viewer при
каждом HTTP-ответе получает новую OAuth URL; URL, state и токены не попадают в PostgreSQL.

Если подключений несколько, сервис не выбирает одно молча и возвращает понятный текст. После
успешного OAuth MVP просит повторить исходную команду.

## HTTP-граница

`MessageController` реализует generated `MessagesApi` и вызывает один метод `MessageFlowService`.
`tenant_id`, `sub` и исходный bearer token берутся из JWT, поэтому канал не может подменить владельца
данными request body. Проверяются подпись, issuer и audience `conversation-service`.

`RestAgentClient`, `RestActionClient` и `RestConnectionClient` используют generated transport-модели
contracts `4.0.0`, но переводят их во внутренние модели. URL, список доступных connector и timeout
задаются environment. Ошибка сети или некорректный ответ превращаются в безопасный код состояния без
сохранения текста exception.

OpenAPI Generator 7.24 некорректно генерирует Java для boolean `const`. Поэтому только временная
codegen-копия Agent API в `build/` теряет это ограничение, а адаптер обязательно проверяет
`requiresApproval == true`. Исходный snapshot остаётся неизменным.
