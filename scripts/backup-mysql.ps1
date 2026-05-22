param(
    [string]$Database = $env:DB_NAME,
    [string]$HostName = $env:DB_HOST,
    [int]$Port = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$Username = $env:DB_USERNAME,
    [string]$Password = $env:DB_PASSWORD,
    [string]$OutputDir = "backups/mysql",
    [int]$RetentionDays = 7,
    [string]$MysqldumpPath = "mysqldump"
)

$ErrorActionPreference = "Stop"

function Set-DefaultsFromJdbcUrl {
    if (-not $env:DB_URL) {
        return
    }

    $match = [regex]::Match($env:DB_URL, "^jdbc:mysql://(?<host>[^:/?]+)(:(?<port>\d+))?/(?<database>[^?]+)")
    if (-not $match.Success) {
        return
    }

    if (-not $script:HostName) {
        $script:HostName = $match.Groups["host"].Value
    }

    if (-not $env:DB_PORT -and $match.Groups["port"].Success) {
        $script:Port = [int]$match.Groups["port"].Value
    }

    if (-not $script:Database) {
        $script:Database = $match.Groups["database"].Value
    }
}

Set-DefaultsFromJdbcUrl

if (-not $Database) {
    $Database = "petcare"
}

if (-not $HostName) {
    $HostName = "localhost"
}

if (-not $Username) {
    $Username = "root"
}

if (-not $Password) {
    $Password = "admin"
}

$resolvedOutputDir = Resolve-Path -Path $OutputDir -ErrorAction SilentlyContinue
if (-not $resolvedOutputDir) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    $resolvedOutputDir = Resolve-Path -Path $OutputDir
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "$Database-$timestamp"
$sqlPath = Join-Path $resolvedOutputDir "$baseName.sql"
$zipPath = Join-Path $resolvedOutputDir "$baseName.sql.zip"

$previousMysqlPwd = $env:MYSQL_PWD
$env:MYSQL_PWD = $Password

try {
    $arguments = @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$Username",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--events",
        "--default-character-set=utf8mb4",
        "--databases",
        $Database,
        "--result-file=$sqlPath"
    )

    & $MysqldumpPath @arguments

    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump finalizo con codigo $LASTEXITCODE"
    }

    Compress-Archive -Path $sqlPath -DestinationPath $zipPath -Force
    Remove-Item -LiteralPath $sqlPath -Force

    if ($RetentionDays -gt 0) {
        $limit = (Get-Date).AddDays(-$RetentionDays)
        Get-ChildItem -Path $resolvedOutputDir -Filter "*.sql.zip" |
            Where-Object { $_.LastWriteTime -lt $limit } |
            Remove-Item -Force
    }

    Write-Host "Backup creado: $zipPath"
}
finally {
    if ($null -eq $previousMysqlPwd) {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $previousMysqlPwd
    }
}
