$ErrorActionPreference = 'Stop'
# One-time installation; synthesis is offline after the model is downloaded.
$voiceRoot = Join-Path $env:LOCALAPPDATA 'NYRA\voice'
$runtimeDir = Join-Path $voiceRoot 'runtime'
$modelDir = Join-Path $voiceRoot 'models'
New-Item -ItemType Directory -Force -Path $modelDir | Out-Null
python -m venv $runtimeDir
if ($LASTEXITCODE -ne 0) { throw 'Instale Python 3.11 ou 3.12 e tente novamente.' }
$voicePython = Join-Path $runtimeDir 'Scripts\python.exe'
& $voicePython -m pip install 'piper-tts==1.8.0'
if ($LASTEXITCODE -ne 0) { throw 'Falha na instalação do Piper.' }
& $voicePython -m piper.download_voices pt_BR-faber-medium --data-dir $modelDir
if ($LASTEXITCODE -ne 0) { throw 'Falha ao baixar a voz.' }
Write-Host "Voz instalada. Reinicie a Nyra e use Configurações > Testar voz."
Write-Host "Piper: $(Join-Path $runtimeDir 'Scripts\piper.exe')"
Write-Host "Modelo: $(Join-Path $modelDir 'pt_BR-faber-medium.onnx')"
