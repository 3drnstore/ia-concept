$ErrorActionPreference = "Stop"

if (-not (Test-Path ".venv\Scripts\python.exe")) {
    Write-Host "Ambiente da Nyra ainda não foi preparado." -ForegroundColor Yellow
    Write-Host "Execute primeiro: scripts\setup_windows.ps1" -ForegroundColor Cyan
    exit 1
}

& .\.venv\Scripts\python.exe -m nyra.main
