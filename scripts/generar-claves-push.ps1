# Genera un par de claves VAPID (P-256) para los avisos push (D-078) y las escribe en el formato del .env:
#   PUSH_CLAVE_PUBLICA=...   (punto sin comprimir, 65 bytes, en Base64 URL)
#   PUSH_CLAVE_PRIVADA=...   (escalar, 32 bytes, en Base64 URL)
#
#   powershell -ExecutionPolicy Bypass -File scripts\generar-claves-push.ps1                  # las muestra
#   powershell -ExecutionPolicy Bypass -File scripts\generar-claves-push.ps1 -Anadir .env     # las añade al .env
#
# La clave privada es un secreto, como JWT_SECRET: no se versiona ni se comparte. Si se cambian las claves, los
# navegadores suscritos tienen que volver a activar los avisos.
param([string]$Anadir)
$ErrorActionPreference = 'Stop'

function ConvertTo-Base64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$ecdsa = [System.Security.Cryptography.ECDsa]::Create([System.Security.Cryptography.ECCurve+NamedCurves]::nistP256)
try {
    $parametros = $ecdsa.ExportParameters($true)
    $publica = [byte[]](@(4) + $parametros.Q.X + $parametros.Q.Y)
    $lineas = @(
        "PUSH_CLAVE_PUBLICA=$(ConvertTo-Base64Url $publica)",
        "PUSH_CLAVE_PRIVADA=$(ConvertTo-Base64Url $parametros.D)"
    )
} finally {
    $ecdsa.Dispose()
}

if ($Anadir) {
    if (Select-String -Path $Anadir -Pattern '^PUSH_CLAVE_(PUBLICA|PRIVADA)=.' -Quiet -ErrorAction SilentlyContinue) {
        throw "$Anadir ya tiene claves VAPID: bórralas antes si quieres otras."
    }
    Add-Content -Path $Anadir -Value (@('', '# Avisos push (D-078), generadas con scripts\generar-claves-push.ps1') + $lineas) -Encoding UTF8
    Write-Host "Claves VAPID añadidas a $Anadir"
} else {
    $lineas
}
