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
    "docs/decisions/0004-message-flow.md",
    "src/main/openapi/README.md"
)

$missing = $requiredFiles | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) }
if ($missing.Count -gt 0) {
    throw "Required documentation files are missing: $($missing -join ', ')"
}

$catalog = Get-Content -LiteralPath "catalog-info.yaml" -Raw
if ($catalog -notmatch "conversation-api@2\.3\.0") {
    throw "The catalog must pin Conversation API 2.3.0."
}

$service = Get-Content -LiteralPath "SERVICE.md" -Raw
if ($service -notmatch "HTTP endpoint") {
    throw "SERVICE.md must describe the current HTTP boundary."
}

Write-Host "conversation-service documentation follows the project standard."
