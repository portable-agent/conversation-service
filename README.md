# Conversation Service

Сервис хранит состояние диалога и будет управлять цепочкой `Agent Runtime → Action Service` для всех
каналов Portable Agent.

Сейчас реализованы инженерный каркас и слой хранения сообщения:

- Spring Boot и обычные MVC-пакеты;
- jOOQ без JPA и Hibernate;
- generated HTTP interface из contracts `3.1.0`;
- OAuth2 Resource Server;
- Flyway и PostgreSQL dependencies;
- TDD, Spotless, Testcontainers и общий CI/CD.
- таблицы диалога и сообщения через Flyway и generated jOOQ;
- идемпотентность сообщения по пользователю и `requestKey`;
- удаление исходного текста после закрытия или 24 часов.
- состояния обработки с атомарным захватом работы и восстановлением зависшего worker;
- временно сохранённый ответ для безопасного повтора запроса.
- application-сценарий `MessageFlowService` без сетевых вызовов внутри транзакции;
- стратегия карточек и первая карточка подтверждения `calendar.create_event`;
- безопасные порты Agent Runtime и Action Service, не привязанные к HTTP-моделям.
- generated HTTP-модели Agent, Action и Connection из contracts `3.1.0`;
- endpoint `POST /api/v1/messages` с JWT issuer и audience validation;
- HTTP-адаптеры с настраиваемыми URL и timeout без hardcode окружения.
- проверка Google Calendar подключения до создания Action;
- общий `connection` widget с новой OAuth-ссылкой для каждого HTTP-ответа.

Conversation Service принимает сообщение и проходит путь до вопроса, виджета подключения либо
сохранённого действия. Одноразовая OAuth-ссылка не хранится в PostgreSQL.

## Проверка

```powershell
./gradlew.bat clean check
pwsh ./scripts/check-docs.ps1
```

## Документация

- [Паспорт сервиса](SERVICE.md)
- [Архитектура](docs/architecture.md)
- [Разработка](docs/development.md)
- [Правила AI-агентов](AGENTS.md)

## Лицензия

Apache License 2.0.
