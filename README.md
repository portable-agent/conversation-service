# Conversation Service

Сервис хранит состояние диалога и будет управлять цепочкой `Agent Runtime → Action Service` для всех
каналов Portable Agent.

Сейчас реализован только инженерный каркас:

- Spring Boot и обычные MVC-пакеты;
- jOOQ без JPA и Hibernate;
- generated HTTP interface из contracts `2.3.0`;
- OAuth2 Resource Server;
- Flyway и PostgreSQL dependencies;
- TDD, Spotless, Testcontainers и общий CI/CD.

Endpoint сообщения ещё не реализован. До выбора правил приватности сервис не сохраняет текст и не
делает вид, что диалог уже работает.

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
