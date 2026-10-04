# Снимок контракта

- Источник: `portable-agent/contracts`
- Target release: `v4.0.0` from contracts PR #21
- Исходные API: `openapi/conversation-api.yaml`, `openapi/agent-runtime-api.yaml`,
  `openapi/action-api.yaml` и `openapi/connection-api.yaml`

Файл собран в self-contained вид для OpenAPI Generator. Его нельзя менять вручную. Новая версия
сначала выпускается в `contracts`, затем обновляется отдельным pull request.

`conversation-api.yaml` генерирует server interface. Снимки в `clients/` генерируют только модели
HTTP-клиентов; application-слой от generated types не зависит.

OpenAPI Generator 7.24 не компилирует boolean `const` из Agent API. Задача `prepareAgentSpec` удаляет
только это ограничение из временной копии в `build/`; committed snapshot не меняется. Agent adapter
обязан отдельно отклонять ответ, если `requiresApproval` не равен `true`.
