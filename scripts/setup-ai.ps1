$ErrorActionPreference = 'Stop'

Write-Host 'NYRA // AI CORE SETUP' -ForegroundColor Cyan
Write-Host 'Este processo instala o llama.cpp e prepara o Qwen3 8B local.'
Write-Host 'O modelo Q5_K_M tem aproximadamente 5.85 GB e sera baixado na primeira inicializacao.'

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw 'winget nao foi encontrado neste Windows. Instale/atualize o App Installer da Microsoft Store.'
}

if (-not (Get-Command llama -ErrorAction SilentlyContinue)) {
    Write-Host 'Instalando llama.cpp...' -ForegroundColor Yellow
    winget install --id llama.cpp -e --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0) {
        throw 'Falha ao instalar llama.cpp pelo winget.'
    }
    $env:PATH = [Environment]::GetEnvironmentVariable('PATH', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('PATH', 'User')
}

$nyraDir = Join-Path $env:LOCALAPPDATA 'NYRA'
$aiDir = Join-Path $nyraDir 'ai'
New-Item -ItemType Directory -Force -Path $aiDir | Out-Null

$launcher = @'
$ErrorActionPreference = 'Stop'
$env:NYRA_LLAMA_URL = 'http://127.0.0.1:8080'
$env:NYRA_MODEL = 'Qwen3-8B'
Write-Host 'NYRA // LOCAL AI CORE' -ForegroundColor Cyan
Write-Host 'Qwen3 8B Q5_K_M | localhost:8080 | GPU offload automatic/maximo'
llama serve -hf Qwen/Qwen3-8B-GGUF:Q5_K_M --host 127.0.0.1 --port 8080 -ngl 99 -c 8192
'@

$launcherPath = Join-Path $aiDir 'start-ai.ps1'
Set-Content -Path $launcherPath -Value $launcher -Encoding UTF8

Write-Host ''
Write-Host 'Preparacao concluida.' -ForegroundColor Green
Write-Host "Launcher criado em: $launcherPath"
Write-Host 'Na primeira execucao, o llama.cpp baixara o modelo oficial Qwen3 8B Q5_K_M.'
Write-Host 'Depois que o servidor mostrar que esta ouvindo em 127.0.0.1:8080, a Nyra podera conversar com o modelo local.'
