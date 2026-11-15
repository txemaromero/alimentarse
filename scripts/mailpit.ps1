# Descarga Mailpit la primera vez (sin Docker ni permisos de administrador) y lo arranca.
# SMTP en localhost:1025 e interfaz web en http://localhost:8025. Ctrl+C para pararlo.
#   powershell -ExecutionPolicy Bypass -File scripts\mailpit.ps1
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$version = 'v1.33.1'
# SHA-256 de mailpit-windows-amd64.zip publicado por GitHub para esta versión
$sha256 = '0bcf8f1e7c21005b30c206064ff2f37dac602ef66774e558379d326f5673d652'

$dir = Join-Path $PSScriptRoot "..\.tools\mailpit\$version"
$exe = Join-Path $dir 'mailpit.exe'

if (-not (Test-Path $exe)) {
    New-Item -ItemType Directory -Force $dir | Out-Null
    $zip = Join-Path $dir 'mailpit.zip'
    Write-Host "Descargando Mailpit $version..."
    Invoke-WebRequest -UseBasicParsing -OutFile $zip `
        "https://github.com/axllent/mailpit/releases/download/$version/mailpit-windows-amd64.zip"
    $hash = (Get-FileHash -Algorithm SHA256 $zip).Hash.ToLower()
    if ($hash -ne $sha256) {
        Remove-Item $zip
        throw "El SHA-256 del ZIP descargado no coincide (esperado $sha256, obtenido $hash)."
    }
    Expand-Archive -Force $zip -DestinationPath $dir
    Remove-Item $zip
}

Write-Host 'Mailpit: SMTP en localhost:1025, interfaz en http://localhost:8025 (Ctrl+C para parar)'
& $exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
