# Conversation Service

Сервис хранит состояние диалога и будет управлять цепочкой `Agent Runtime → Action Service` для всех
каналов Portable Agent.

Сейчас реализованы инженерный каркас и слой хранения сообщения:

- Spring Boot и обычные MVC-пакеты;
- jOOQ без JPA и Hibernate;
- generated HTTP interface из contracts `2.3.0`;
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

HTTP endpoint и реальные HTTP-адаптеры Agent Runtime и Action Service ещё не подключены. Они будут
следующим пакетом; бизнесовый сценарий пока не объявлен готовым.

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
