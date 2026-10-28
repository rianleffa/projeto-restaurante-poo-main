$ErrorActionPreference = 'Stop'
$projectDirectory = Join-Path $PSScriptRoot 'projeto-restaurante-poo-main'
$projectLauncher = Join-Path $projectDirectory 'executar.ps1'

if (-not (Test-Path -LiteralPath $projectLauncher)) {
    throw "Não encontrei o projeto Java em: $projectDirectory"
}

Push-Location -LiteralPath $projectDirectory
try {
    & $projectLauncher
} finally {
    Pop-Location
}
