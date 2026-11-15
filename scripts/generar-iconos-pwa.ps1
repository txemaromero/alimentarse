# Genera los iconos PNG de la PWA a partir de los SVG originales de frontend/public/icons/ (icono.svg e
# icono-maskable.svg). Los dibuja Microsoft Edge en modo headless, que viene con Windows; no hace falta instalar nada.
#   powershell -ExecutionPolicy Bypass -File scripts\generar-iconos-pwa.ps1
$ErrorActionPreference = 'Stop'

$iconos = (Resolve-Path (Join-Path $PSScriptRoot '..\frontend\public\icons')).Path
$edge = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $edge) { throw 'No se encuentra Microsoft Edge (msedge.exe).' }

# Perfil temporal propio: así no choca con un Edge abierto
$perfil = Join-Path $env:TEMP 'toca-iconos-edge'

function Dibujar([string]$svg, [int]$lado, [string]$destino) {
    $url = 'file:///' + (Join-Path $iconos $svg).Replace('\', '/')
    $salida = Join-Path $iconos $destino
    if (Test-Path $salida) { Remove-Item $salida }
    $argumentos = @(
        '--headless=new', '--disable-gpu', '--hide-scrollbars', '--no-first-run',
        "--user-data-dir=$perfil", '--default-background-color=00000000',
        "--window-size=$lado,$lado", "--screenshot=$salida", $url
    )
    Start-Process -FilePath $edge -ArgumentList $argumentos -Wait -WindowStyle Hidden
    if (-not (Test-Path $salida)) { throw "Edge no ha generado $destino" }
    Write-Host "  $destino"
}

function Reducir([string]$origen, [int]$lado, [string]$destino) {
    $imagen = [System.Drawing.Image]::FromFile((Join-Path $iconos $origen))
    try {
        $mapa = New-Object System.Drawing.Bitmap $lado, $lado, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($mapa)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($imagen, 0, 0, $lado, $lado)
        $g.Dispose()
        $mapa.Save((Join-Path $iconos $destino), [System.Drawing.Imaging.ImageFormat]::Png)
        $mapa.Dispose()
    } finally {
        $imagen.Dispose()
    }
    Write-Host "  $destino"
}

Write-Host "Iconos en $iconos"
# Edge no deja la ventana más pequeña que unos cientos de píxeles: se dibuja a 512 y se reduce con System.Drawing
Add-Type -AssemblyName System.Drawing
Dibujar 'icono.svg' 512 'icono-512.png'
Dibujar 'icono-maskable.svg' 512 'icono-maskable-512.png'

# «any»: fondo transparente; también el favicon
foreach ($lado in 32, 72, 96, 128, 144, 152, 192, 384) {
    Reducir 'icono-512.png' $lado "icono-$lado.png"
}
# «maskable» (Android recorta la forma) e iOS (no admite transparencia)
Reducir 'icono-maskable-512.png' 192 'icono-maskable-192.png'
Reducir 'icono-maskable-512.png' 180 'apple-touch-icon.png'
