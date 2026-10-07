param(
    [ValidateSet('up', 'smoke', 'status', 'logs', 'down')]
    [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
$serviceRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $serviceRoot 'compose.local.yml'
$baseUrl = 'http://127.0.0.1:8084'
$dockerConfig = Join-Path ([IO.Path]::GetTempPath()) 'dynamicmart-docker-cli'
New-Item -ItemType Directory -Force -Path $dockerConfig | Out-Null

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    # Public images only. An isolated CLI config avoids a broken Docker Desktop credential helper
    # without reading, changing or deleting the user's global Docker credentials.
    & docker --config $dockerConfig compose -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose thất bại với exit code $LASTEXITCODE."
    }
}

function Assert-DockerReady {
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker daemon chưa hoạt động. Hãy mở Docker Desktop rồi chạy lại lệnh.'
    }
}

function Wait-OrderHealth {
    for ($attempt = 1; $attempt -le 120; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }
    throw 'Order Service không đạt health UP sau 120 giây.'
}

function Invoke-LocalSmoke {
    Wait-OrderHealth
    Assert-DockerReady
    $flywayVersion = (& docker --config $dockerConfig compose -f $composeFile exec --no-TTY postgres `
            psql -U order_user -d order_db -Atc 'select max(installed_rank) from flyway_schema_history where success;').Trim()
    if ($LASTEXITCODE -ne 0 -or $flywayVersion -ne '8') {
        throw "Flyway smoke thất bại; version nhận được: $flywayVersion"
    }
    & docker --config $dockerConfig compose -f $composeFile exec --no-TTY rabbitmq rabbitmq-diagnostics -q ping
    if ($LASTEXITCODE -ne 0) {
        throw 'RabbitMQ ping thất bại.'
    }

    $authenticatedApi = 'SKIPPED - cần ORDER_LOCAL_ACCESS_TOKEN do Identity Service thật cấp'
    if (-not [string]::IsNullOrWhiteSpace($env:ORDER_LOCAL_ACCESS_TOKEN)) {
        $headers = @{ Authorization = "Bearer $($env:ORDER_LOCAL_ACCESS_TOKEN)" }
        Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/orders?page=0&size=1" -Headers $headers | Out-Null
        $authenticatedApi = 'OK'
    }

    [pscustomobject]@{
        Health = 'UP'
        FlywayVersion = $flywayVersion
        RabbitMq = 'Ping succeeded'
        AuthenticatedApi = $authenticatedApi
        Note = 'Không tạo token hoặc dữ liệu giả; Checkout cần service thật ở cổng 8081/8082/8083/8085.'
    } | Format-List
}

switch ($Action) {
    'up' {
        Assert-DockerReady
        Invoke-Compose up --build --detach
        Wait-OrderHealth
        Write-Host 'Order Service local đã sẵn sàng tại http://127.0.0.1:8084'
        Write-Host 'PostgreSQL: 127.0.0.1:5434; RabbitMQ Management: http://127.0.0.1:15674'
        Write-Host 'Chạy smoke thật: .\scripts\order-local.ps1 smoke'
    }
    'smoke' { Invoke-LocalSmoke }
    'status' { Assert-DockerReady; Invoke-Compose ps }
    'logs' { Assert-DockerReady; Invoke-Compose logs -f order-service }
    'down' { Assert-DockerReady; Invoke-Compose down }
}
