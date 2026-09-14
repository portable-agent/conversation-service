$ErrorActionPreference = "Stop"

$requiredFiles = @(
    "README.md",
    "AGENTS.md",
    "SERVICE.md",
    "catalog-info.yaml",
    "mkdocs.yml",
    "docs/index.md",
    "docs/architecture.md",
    "docs/development.md",
    "docs/runbook.md",
    "docs/decisions/0001-foundation-boundary.md",
    "src/main/openapi/README.md"
)

$missing = $requiredFiles | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) }
if ($missing.Count -gt 0) {
    throw "Нет обязательных файлов: $($missing -join ', ')"
}

$catalog = Get-Content -LiteralPath "catalog-info.yaml" -Raw
if ($catalog -notmatch "conversation-api@2\.3\.0") {
    throw "В каталоге должна быть закреплена версия Conversation API 2.3.0."
}

$service = Get-Content -LiteralPath "SERVICE.md" -Raw
if ($service -notmatch "ещё не\s+реализован") {
    throw "SERVICE.md должен честно описывать текущую границу каркаса."
}

Write-Host "Документация conversation-service соответствует стандарту."
