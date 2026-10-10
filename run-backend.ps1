[CmdletBinding()]
param(
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$logDirectory = Join-Path $root ".run\logs"
$startedProcesses = [System.Collections.Generic.List[object]]::new()

$services = @(
    @{ Name = "identity";   Path = "services/identity-service";   Port = 8081 },
    @{ Name = "catalog";    Path = "services/catalog-service";    Port = 8082 },
    @{ Name = "cart";       Path = "services/cart-service";       Port = 8083 },
    @{ Name = "order";      Path = "services/order-service";      Port = 8084 },
    @{ Name = "payment";    Path = "services/payment-service";    Port = 8085 },
    @{ Name = "engagement"; Path = "services/engagement-service"; Port = 8086 }
)
$gateway = @{ Name = "gateway"; Path = "api-gateway"; Port = 8080 }

function Test-TcpPort([int]$Port) {
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync("127.0.0.1", $Port)
        return $task.Wait(300) -and $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Read-EnvFile([string]$Path) {
    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing configuration file: $Path"
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $value = $matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$matches[1]] = $value
        }
    }
    return $values
}

function Set-ProcessEnvironment($Values) {
    foreach ($key in $Values.Keys) {
        [Environment]::SetEnvironmentVariable($key, $Values[$key], "Process")
    }
}

function Start-BackendProcess($Service, $SharedSecurity, [switch]$OrderFallback) {
    if (Test-TcpPort $Service.Port) {
        Write-Host ("{0,-12} already running at http://localhost:{1}" -f $Service.Name, $Service.Port) -ForegroundColor DarkGreen
        return $null
    }

    $workingDirectory = Join-Path $root $Service.Path
    $wrapper = Join-Path $workingDirectory "mvnw.cmd"
    if (-not (Test-Path -LiteralPath $wrapper)) {
        throw "Maven Wrapper not found: $wrapper"
    }

    Set-ProcessEnvironment (Read-EnvFile (Join-Path $workingDirectory ".env"))
    Set-ProcessEnvironment $SharedSecurity
    $env:SPRING_FLYWAY_VALIDATE_ON_MIGRATE = "false"
    $env:MAVEN_OPTS = "-Xms64m -Xmx256m"
    $jvmArgs = "-Xms128m -Xmx320m -XX:MaxMetaspaceSize=192m -XX:TieredStopAtLevel=1"

    if ($OrderFallback) {
        $env:SPRING_FLYWAY_ENABLED = "false"
        $env:SPRING_JPA_HIBERNATE_DDL_AUTO = "none"
    }
    else {
        Remove-Item Env:SPRING_FLYWAY_ENABLED -ErrorAction SilentlyContinue
        Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO -ErrorAction SilentlyContinue
    }

    $suffix = if ($OrderFallback) { "-fallback" } else { "" }
    $stdout = Join-Path $logDirectory "$($Service.Name)$suffix.log"
    $stderr = Join-Path $logDirectory "$($Service.Name)$suffix.error.log"
    Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue

    $process = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/d", "/c", "mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=""$jvmArgs""" `
        -WorkingDirectory $workingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    $record = [pscustomobject]@{
        Service = $Service
        Process = $process
        Stdout = $stdout
        Stderr = $stderr
        IsOrderFallback = [bool]$OrderFallback
    }
    $startedProcesses.Add($record)
    Write-Host "Starting $($Service.Name) on port $($Service.Port)..." -ForegroundColor Cyan
    return $record
}

function Wait-BackendPort($Record, [int]$TimeoutSeconds) {
    if ($null -eq $Record) {
        return $true
    }

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-TcpPort $Record.Service.Port) {
            Write-Host ("{0,-12} ready at http://localhost:{1}" -f $Record.Service.Name, $Record.Service.Port) -ForegroundColor Green
            return $true
        }

        $Record.Process.Refresh()
        if ($Record.Process.HasExited) {
            return $false
        }
        Start-Sleep -Milliseconds 700
    }
    return $false
}

function Get-StartupError($Record) {
    $details = @()
    if (Test-Path -LiteralPath $Record.Stderr) {
        $details += Get-Content -LiteralPath $Record.Stderr -Tail 20
    }
    if (Test-Path -LiteralPath $Record.Stdout) {
        $details += Get-Content -LiteralPath $Record.Stdout -Tail 35
    }
    return ($details -join [Environment]::NewLine)
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java was not found in PATH. JDK 17 or newer is required."
}

if (-not (Test-TcpPort 5432)) {
    throw "PostgreSQL is not running at localhost:5432. Start PostgreSQL first."
}

if (-not (Test-TcpPort 5672)) {
    Write-Warning "RabbitMQ is not running at localhost:5672. Core APIs can still run, but asynchronous features may report health 503."
}

$identityEnv = Read-EnvFile (Join-Path $root "services/identity-service/.env")
$sharedSecurity = @{
    JWT_HMAC_SECRET_BASE64 = $identityEnv["JWT_HMAC_SECRET_BASE64"]
    JWT_ISSUER = $identityEnv["JWT_ISSUER"]
    INTERNAL_API_KEY = $identityEnv["INTERNAL_API_KEY"]
}

foreach ($entry in $sharedSecurity.GetEnumerator()) {
    if ([string]::IsNullOrWhiteSpace($entry.Value)) {
        throw "Missing $($entry.Key) in services/identity-service/.env"
    }
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

try {
    $records = @()
    foreach ($service in $services) {
        $record = Start-BackendProcess $service $sharedSecurity
        if ($null -ne $record) {
            $records += $record
            if (-not (Wait-BackendPort $record $StartupTimeoutSeconds)) {
                if ($record.Service.Name -eq "order" -and -not $record.IsOrderFallback) {
                    $log = Get-StartupError $record
                    if ($log -match 'payment_id.*already exists|V9__add_order_saga_payment_checkpoint') {
                        Write-Warning "Order DB has legacy Flyway history. Restarting Order in local compatibility mode."
                        $fallback = Start-BackendProcess $record.Service $sharedSecurity -OrderFallback
                        if (-not (Wait-BackendPort $fallback $StartupTimeoutSeconds)) {
                            throw "Order Service failed to start.`n$(Get-StartupError $fallback)"
                        }
                        continue
                    }
                }
                throw "$($record.Service.Name) failed to start.`n$(Get-StartupError $record)"
            }
        }
    }

    $gatewayRecord = Start-BackendProcess $gateway $sharedSecurity
    if (-not (Wait-BackendPort $gatewayRecord $StartupTimeoutSeconds)) {
        throw "API Gateway failed to start.`n$(Get-StartupError $gatewayRecord)"
    }

    Write-Host ""
    Write-Host "All backend services are ready:" -ForegroundColor Green
    Write-Host "  gateway      http://localhost:8080"
    Write-Host "  identity     http://localhost:8081"
    Write-Host "  catalog      http://localhost:8082"
    Write-Host "  cart         http://localhost:8083"
    Write-Host "  order        http://localhost:8084"
    Write-Host "  payment      http://localhost:8085"
    Write-Host "  engagement   http://localhost:8086"
    Write-Host "Log: $logDirectory" -ForegroundColor DarkGray
    Write-Host "Press Ctrl+C to stop processes started by this window." -ForegroundColor Yellow

    while ($true) {
        Start-Sleep -Seconds 2
        foreach ($record in $startedProcesses) {
            $record.Process.Refresh()
            if ($record.Process.HasExited -and -not (Test-TcpPort $record.Service.Port)) {
                throw "$($record.Service.Name) stopped unexpectedly. See log: $($record.Stdout)"
            }
        }
    }
}
finally {
    Write-Host "Stopping backend processes started by this script..." -ForegroundColor DarkYellow
    foreach ($record in $startedProcesses) {
        try {
            $record.Process.Refresh()
            if (-not $record.Process.HasExited) {
                & taskkill.exe /PID $record.Process.Id /T /F 2>$null | Out-Null
            }
        }
        catch {
        }
    }
}
