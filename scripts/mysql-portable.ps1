# MySQL 8.4 LTS portable para desarrollar sin Docker ni permisos de administrador.
# La primera vez:
#   - descarga el ZIP oficial en .tools/mysql y verifica su SHA-256;
#   - inicializa el directorio de datos .tools/mysql/data;
#   - crea las bases de datos toca y toca_test y el usuario DB_USER/DB_PASSWORD del .env,
#     y fija la contraseña de root a MYSQL_ROOT_PASSWORD.
# Después arranca el servidor en 127.0.0.1:3307 (Ctrl+C para pararlo). En el .env: DB_URL=jdbc:mysql://localhost:3307/toca
#   powershell -ExecutionPolicy Bypass -File scripts\mysql-portable.ps1
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$version = '8.4.13'
# SHA-256 de mysql-8.4.13-winx64.zip descargado de cdn.mysql.com (ver docs/DECISIONES.md, D-012).
# MD5 del mismo fichero, para cotejarlo con https://dev.mysql.com/downloads/mysql/8.4.html: 7b94489d5524dea9b27097068bea25a8
$sha256 = 'e9cd98b1d23f122987b1afca26ad942efec5891481103d48ae3cde237fa3ea54'
$port = 3307

$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$tools = Join-Path $raiz '.tools\mysql'
$base = Join-Path $tools "mysql-$version-winx64"
$datos = Join-Path $tools 'data'
$ini = Join-Path $tools 'my.ini'
$mysqld = Join-Path $base 'bin\mysqld.exe'

# --- Variables del .env ---
$envFile = Join-Path $raiz '.env'
if (-not (Test-Path $envFile)) { throw 'No existe .env en la raíz del repo: copia .env.example como .env y rellénalo.' }
$vars = @{}
foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') { $vars[$Matches[1]] = $Matches[2].Trim() }
}

# --- Descarga y descompresión ---
if (-not (Test-Path $mysqld)) {
    $descargas = Join-Path $tools 'descargas'
    $zip = Join-Path $descargas "mysql-$version-winx64.zip"
    New-Item -ItemType Directory -Force $descargas | Out-Null
    if (-not (Test-Path $zip)) {
        Write-Host "Descargando MySQL $version (unos 270 MB)..."
        Invoke-WebRequest -UseBasicParsing -OutFile $zip "https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-$version-winx64.zip"
    }
    $hash = (Get-FileHash -Algorithm SHA256 $zip).Hash.ToLower()
    if ($hash -ne $sha256) {
        Remove-Item $zip
        throw "El SHA-256 del ZIP descargado no coincide (esperado $sha256, obtenido $hash)."
    }
    Write-Host 'Descomprimiendo...'
    tar.exe -xf $zip -C $tools
    if ($LASTEXITCODE -ne 0) { throw "tar terminó con código $LASTEXITCODE." }
}

# --- Configuración (se regenera en cada arranque) ---
$basedirIni = $base.Replace('\', '/')
$datadirIni = $datos.Replace('\', '/')
@"
[mysqld]
basedir="$basedirIni"
datadir="$datadirIni"
port=$port
bind-address=127.0.0.1
mysqlx=OFF
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci

[client]
host=127.0.0.1
port=$port
"@ | Set-Content -Path $ini -Encoding ascii

# --- Inicialización (solo la primera vez) ---
if (-not (Test-Path $datos)) {
    $user = if ($vars['DB_USER']) { $vars['DB_USER'] } else { 'toca' }
    $password = $vars['DB_PASSWORD']
    $rootPassword = $vars['MYSQL_ROOT_PASSWORD']
    if (-not $password -or -not $rootPassword) { throw 'DB_PASSWORD y MYSQL_ROOT_PASSWORD deben estar rellenas en .env.' }
    function Sql-Literal([string]$s) { "'" + $s.Replace('\', '\\').Replace("'", "''") + "'" }

    # Una sentencia por línea y sin comentarios: así lo exige --init-file
    $initFile = Join-Path $tools 'init.sql'
    @(
        "ALTER USER 'root'@'localhost' IDENTIFIED BY $(Sql-Literal $rootPassword);"
        'CREATE DATABASE IF NOT EXISTS toca CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
        'CREATE DATABASE IF NOT EXISTS toca_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
        "CREATE USER IF NOT EXISTS $(Sql-Literal $user)@'localhost' IDENTIFIED BY $(Sql-Literal $password);"
        "GRANT ALL PRIVILEGES ON toca.* TO $(Sql-Literal $user)@'localhost';"
        "GRANT ALL PRIVILEGES ON toca_test.* TO $(Sql-Literal $user)@'localhost';"
    ) | Set-Content -Path $initFile -Encoding ascii
    try {
        Write-Host 'Inicializando el directorio de datos...'
        & $mysqld "--defaults-file=$ini" --initialize-insecure "--init-file=$($initFile.Replace('\', '/'))" --console
        if ($LASTEXITCODE -ne 0) {
            Remove-Item -Recurse -Force $datos -ErrorAction SilentlyContinue
            throw "La inicialización de MySQL terminó con código $LASTEXITCODE."
        }
    } finally {
        Remove-Item $initFile -ErrorAction SilentlyContinue
    }
    Write-Host "Creadas las bases de datos toca y toca_test y el usuario '$user'@'localhost'."
}

Write-Host "MySQL $version en 127.0.0.1:$port (Ctrl+C para parar)"
& $mysqld "--defaults-file=$ini" --console
