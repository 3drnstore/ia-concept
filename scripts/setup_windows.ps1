$ErrorActionPreference = "Stop"

Write-Host "N.Y.R.A. // SETUP WINDOWS" -ForegroundColor Cyan

$python = Get-Command py -ErrorAction SilentlyContinue
if ($python) {
    $pythonCmd = "py"
    $pythonArgs = @("-3.11")
} else {
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        Write-Host "Python 3.11+ não encontrado. Instale o Python e execute novamente." -ForegroundColor Red
        exit 1
    }
    $pythonCmd = "python"
    $pythonArgs = @()
}

if (-not (Test-Path ".venv")) {
    & $pythonCmd @pythonArgs -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install --upgrade pip
& .\.venv\Scripts\python.exe -m pip install -e .

Write-Host ""
Write-Host "Setup concluído." -ForegroundColor Green
Write-Host "Use scripts\run_windows.ps1 para abrir a Nyra." -ForegroundColor Cyan
