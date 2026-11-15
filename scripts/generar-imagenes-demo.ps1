# Genera las imágenes placeholder de las recetas de demostración (originales, sin material de terceros):
# un degradado con el color del tipo de receta y el título. Salida: backend/src/main/resources/demo/imagenes/
#   powershell -ExecutionPolicy Bypass -File scripts\generar-imagenes-demo.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$salida = Join-Path $PSScriptRoot '..\backend\src\main\resources\demo\imagenes'
New-Item -ItemType Directory -Force $salida | Out-Null

$colores = @{
    'Plato'    = @('#C8763E', '#7A3E22')
    'Entrante' = @('#6A994E', '#2F5233')
    'Postre'   = @('#E7A1A6', '#A8505C')
    'Bebida'   = @('#5AA9E6', '#1D4E89')
    'Desayuno' = @('#F2C14E', '#C9722F')
    'Salsa'    = @('#C8504F', '#6E1C1C')
}

$recetas = @(
    @('demo-tortilla.jpg', 'Tortilla de patata rellena de jamón y queso', 'Plato'),
    @('demo-pure-calabaza.jpg', 'Puré de calabaza y puerro', 'Entrante'),
    @('demo-boquerones.jpg', 'Boquerones fritos', 'Entrante'),
    @('demo-crema-catalana.jpg', 'Crema catalana', 'Postre'),
    @('demo-rollos-canela.jpg', 'Rollos de canela con chocolate blanco', 'Desayuno'),
    @('demo-pollo-tallarines.jpg', 'Pollo con tallarines', 'Plato'),
    @('demo-esparragos.jpg', 'Espárragos con tomate', 'Entrante'),
    @('demo-lentejas.jpg', 'Lentejas estofadas', 'Plato'),
    @('demo-ensalada-garbanzos.jpg', 'Ensalada de garbanzos', 'Plato'),
    @('demo-batido.jpg', 'Batido de plátano y fresa', 'Bebida'),
    # Galería de la tortilla (D-083)
    @('demo-tortilla-corte.jpg', 'La tortilla cortada, con su relleno', 'Plato'),
    @('demo-tortilla-ingredientes.jpg', 'Los ingredientes de la tortilla', 'Plato')
)

$jpeg = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$calidad = New-Object Drawing.Imaging.EncoderParameters 1
$calidad.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), 85L

$ancho = 1200; $alto = 800
foreach ($r in $recetas) {
    $fichero, $titulo, $tipo = $r
    $bmp = New-Object Drawing.Bitmap $ancho, $alto
    $g = [Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'

    $c1 = [Drawing.ColorTranslator]::FromHtml($colores[$tipo][0]); $c2 = [Drawing.ColorTranslator]::FromHtml($colores[$tipo][1])
    $fondo = New-Object Drawing.Drawing2D.LinearGradientBrush (New-Object Drawing.Rectangle 0, 0, $ancho, $alto), $c1, $c2, 35.0
    $g.FillRectangle($fondo, 0, 0, $ancho, $alto)

    # Plato estilizado: dos círculos translúcidos
    $blanco = New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(40, 255, 255, 255))
    $g.FillEllipse($blanco, 760, 330, 560, 560)
    $g.FillEllipse($blanco, 840, 410, 400, 400)

    $sombra = New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(90, 0, 0, 0))
    $texto = New-Object Drawing.SolidBrush ([Drawing.Color]::White)
    $fuente = New-Object Drawing.Font 'Segoe UI', 64, ([Drawing.FontStyle]::Bold), ([Drawing.GraphicsUnit]::Pixel)
    $formato = New-Object Drawing.StringFormat; $formato.Alignment = 'Near'; $formato.LineAlignment = 'Center'
    $caja = New-Object Drawing.RectangleF 80, 160, 900, 420
    $cajaSombra = New-Object Drawing.RectangleF 84, 164, 900, 420
    $g.DrawString($titulo, $fuente, $sombra, $cajaSombra, $formato)
    $g.DrawString($titulo, $fuente, $texto, $caja, $formato)

    $pie = New-Object Drawing.Font 'Segoe UI', 26, ([Drawing.FontStyle]::Regular), ([Drawing.GraphicsUnit]::Pixel)
    $g.DrawString("$tipo · imagen de demostración · Alimentarse", $pie, $texto, 80, 700)

    $bmp.Save((Join-Path $salida $fichero), $jpeg, $calidad)
    $g.Dispose(); $bmp.Dispose()
    Write-Host "Generada $fichero"
}
