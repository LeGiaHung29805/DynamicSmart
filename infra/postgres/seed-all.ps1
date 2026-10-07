[CmdletBinding()]
param(
    [ValidateSet("migrate", "validate", "info")]
    [string]$Command = "migrate",

    [string[]]$Service,

    # Có thể dùng một .env làm nguồn credential PostgreSQL chung cho cả 6 database.
    # Script giữ host/port/user/password và tự thay tên database theo từng service.
    [string]$SharedEnvFile,

    # Mặc định chỉ cho phép localhost để tránh nạp dữ liệu demo nhầm vào môi trường thật.
    [switch]$AllowNonLocalDatabase
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$flywayPlugin = "org.flywaydb:flyway-maven-plugin:12.4.0"

$definitions = @(
    [pscustomobject]@{ Name = "identity-service";   Database = "identity_db" },
    [pscustomobject]@{ Name = "catalog-service";    Database = "catalog_db" },
    [pscustomobject]@{ Name = "cart-service";       Database = "cart_db" },
    [pscustomobject]@{ Name = "order-service";      Database = "order_db" },
    [pscustomobject]@{ Name = "payment-service";    Database = "payment_db" },
    [pscustomobject]@{ Name = "engagement-service"; Database = "engagement_db" }
)

function Read-DotEnv([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) { continue }
        $parts = $trimmed.Split("=", 2)
        $value = $parts[1].Trim()
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$parts[0].Trim()] = $value
    }
    return $values
}

function Parse-JdbcUrl([string]$Url) {
    if ($Url -notmatch '^jdbc:postgresql://(?<host>[^/:?]+)(?::(?<port>\d+))?/(?<database>[^?]+)(?<query>\?.*)?$') {
        throw "DB_URL không đúng dạng jdbc:postgresql://host:port/database."
    }
    return [pscustomobject]@{
        Host = $Matches.host
        Port = if ($Matches.port) { [int]$Matches.port } else { 5432 }
        Database = $Matches.database
        Query = $Matches.query
    }
}

function Assert-UsableCredential([string]$Name, [string]$Value, [string]$Source) {
    if ([string]::IsNullOrWhiteSpace($Value) -or
        $Value -match '^(your_|replace_|base64_)' -or
        $Value -match 'replace.with') {
        throw "$Name trong $Source chưa có giá trị local hợp lệ."
    }
}

$selected = if ($Service) {
    $unknown = $Service | Where-Object { $_ -notin $definitions.Name }
    if ($unknown) { throw "Service không hợp lệ: $($unknown -join ', ')." }
    $definitions | Where-Object { $_.Name -in $Service }
} else {
    $definitions
}

$shared = $null
$sharedJdbc = $null
if ($SharedEnvFile) {
    $resolvedSharedEnv = (Resolve-Path -LiteralPath $SharedEnvFile).Path
    $shared = Read-DotEnv $resolvedSharedEnv
    foreach ($key in @("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
        Assert-UsableCredential $key $shared[$key] $resolvedSharedEnv
    }
    $sharedJdbc = Parse-JdbcUrl $shared.DB_URL
}

# Preflight toàn bộ trước khi migration đầu tiên thay đổi database.
$plans = @()
foreach ($definition in $selected) {
    $serviceDir = Join-Path $repoRoot "services\$($definition.Name)"
    $mavenWrapper = Join-Path $serviceDir "mvnw.cmd"
    if (-not (Test-Path -LiteralPath $mavenWrapper)) {
        throw "Không tìm thấy Maven Wrapper: $mavenWrapper"
    }

    if ($shared) {
        $query = if ($sharedJdbc.Query) { $sharedJdbc.Query } else { "" }
        $dbUrl = "jdbc:postgresql://$($sharedJdbc.Host):$($sharedJdbc.Port)/$($definition.Database)$query"
        $dbUser = $shared.DB_USERNAME
        $dbPassword = $shared.DB_PASSWORD
        $source = $resolvedSharedEnv
    } else {
        $envFile = Join-Path $serviceDir ".env"
        if (-not (Test-Path -LiteralPath $envFile)) {
            throw "Thiếu $envFile. Hãy sao chép .env.example thành .env hoặc dùng -SharedEnvFile."
        }
        $settings = Read-DotEnv $envFile
        foreach ($key in @("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
            Assert-UsableCredential $key $settings[$key] $envFile
        }
        $dbUrl = $settings.DB_URL
        $dbUser = $settings.DB_USERNAME
        $dbPassword = $settings.DB_PASSWORD
        $source = $envFile
    }

    $jdbc = Parse-JdbcUrl $dbUrl
    if ($jdbc.Database -ne $definition.Database) {
        throw "$($definition.Name) phải trỏ tới $($definition.Database), hiện đang là $($jdbc.Database) trong $source."
    }
    if (-not $AllowNonLocalDatabase -and $jdbc.Host -notin @("localhost", "127.0.0.1", "::1")) {
        throw "Từ chối seed host không phải localhost ($($jdbc.Host)). Chỉ dùng -AllowNonLocalDatabase khi bạn chắc chắn đây là môi trường dev riêng."
    }

    $plans += [pscustomobject]@{
        Definition = $definition
        ServiceDir = $serviceDir
        MavenWrapper = $mavenWrapper
        DbUrl = $dbUrl
        DbUser = $dbUser
        DbPassword = $dbPassword
    }
}

$oldFlywayUrl = $env:FLYWAY_URL
$oldFlywayUser = $env:FLYWAY_USER
$oldFlywayPassword = $env:FLYWAY_PASSWORD
$oldFlywayLocations = $env:FLYWAY_LOCATIONS

try {
    foreach ($plan in $plans) {
        Write-Host "[$Command] $($plan.Definition.Name) -> $($plan.Definition.Database)"
        $env:FLYWAY_URL = $plan.DbUrl
        $env:FLYWAY_USER = $plan.DbUser
        $env:FLYWAY_PASSWORD = $plan.DbPassword
        $env:FLYWAY_LOCATIONS = "filesystem:src/main/resources/db/migration,filesystem:src/main/resources/db/seed/local"

        Push-Location $plan.ServiceDir
        try {
            & $plan.MavenWrapper -q -DskipTests "$flywayPlugin`:$Command"
            if ($LASTEXITCODE -ne 0) {
                throw "Flyway $Command thất bại tại $($plan.Definition.Name) (exit code $LASTEXITCODE)."
            }
        } finally {
            Pop-Location
        }
    }
} finally {
    $env:FLYWAY_URL = $oldFlywayUrl
    $env:FLYWAY_USER = $oldFlywayUser
    $env:FLYWAY_PASSWORD = $oldFlywayPassword
    $env:FLYWAY_LOCATIONS = $oldFlywayLocations
}

Write-Host "Hoàn tất Flyway $Command cho $($plans.Count) database local."
