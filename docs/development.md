# Разработка

```powershell
./gradlew.bat clean check
pwsh ./scripts/check-docs.ps1
```

Generated API создаётся задачей `openApiGenerate` в `build/generated-src/openapi`. Файлы в `build/`
нельзя коммитить или редактировать.

Тесты делятся на быстрые unit, MVC slice и PostgreSQL integration через Testcontainers. H2 не
используется.
