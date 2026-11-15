# Prepara el MySQL local (sin Docker) para desarrollo y tests:
#   - bases de datos toca y toca_test (utf8mb4, colación que no distingue mayúsculas ni acentos)
#   - usuario DB_USER con la contraseña DB_PASSWORD del fichero .env, con permisos sobre ambas
# Requisitos: servicio de MySQL arrancado (Start-Service MySQL80, como administrador) y la contraseña de root,
# que mysql pedirá por consola.
#   powershell -ExecutionPolicy Bypass -File scripts\mysql-init-local.ps1
$ErrorActionPreference = 'Stop'

$envFile = Join-Path $PSScriptRoot '..\.env'
if (-not (Test-Path $envFile)) { throw 'No existe .env en la raíz del repo: copia .env.example como .env y rellénalo.' }

$vars = @{}
foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') { $vars[$Matches[1]] = $Matches[2].Trim() }
}
$user = if ($vars['DB_USER']) { $vars['DB_USER'] } else { 'toca' }
$password = $vars['DB_PASSWORD']
if (-not $password) { throw 'DB_PASSWORD está vacía en .env.' }

$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $mysql = Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mysql) { throw 'No se encuentra mysql.exe (ni en el PATH ni en C:\Program Files\MySQL).' }

$sqlPassword = $password.Replace('\', '\\').Replace("'", "''")
$sql = @"
CREATE DATABASE IF NOT EXISTS toca CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS toca_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$user'@'localhost' IDENTIFIED BY '$sqlPassword';
ALTER USER '$user'@'localhost' IDENTIFIED BY '$sqlPassword';
GRANT ALL PRIVILEGES ON toca.* TO '$user'@'localhost';
GRANT ALL PRIVILEGES ON toca_test.* TO '$user'@'localhost';
FLUSH PRIVILEGES;
"@

$tmp = New-TemporaryFile
try {
    Set-Content -Path $tmp -Value $sql -Encoding ascii
    Write-Host "Usando $mysql. Introduce la contraseña de root de MySQL:"
    & $mysql -u root -p --execute="source $($tmp.FullName.Replace('\', '/'))"
    if ($LASTEXITCODE -ne 0) { throw "mysql terminó con código $LASTEXITCODE." }
    Write-Host "Listo: bases de datos toca y toca_test y usuario '$user'@'localhost'."
} finally {
    Remove-Item $tmp -ErrorAction SilentlyContinue
}
