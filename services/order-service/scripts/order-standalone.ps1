param(
    [ValidateSet('up', 'smoke', 'status', 'logs', 'down')]
    [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
$localScript = Join-Path $PSScriptRoot 'order-local.ps1'
$baseUrl = 'http://127.0.0.1:8084'
$previousProfiles = $env:ORDER_SPRING_PROFILES_ACTIVE

try {
    $env:ORDER_SPRING_PROFILES_ACTIVE = 'local,standalone'
    & $localScript $Action

    if ($Action -eq 'up') {
        Write-Host 'Standalone mock API: http://127.0.0.1:8084/api/v1/standalone/data'
        Write-Host 'Run full smoke: .\scripts\order-standalone.ps1 smoke'
    }

    if ($Action -eq 'smoke') {
        $tokenResponse = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/standalone/tokens/customer"
        $headers = @{ Authorization = "Bearer $($tokenResponse.accessToken)" }
        $orders = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/orders?page=0&size=1" -Headers $headers
        [pscustomobject]@{
            StandaloneToken = 'OK'
            AuthenticatedOrdersApi = 'OK'
            ReturnedOrders = $orders.content.Count
        } | Format-List
    }
} finally {
    if ($null -eq $previousProfiles) {
        Remove-Item Env:ORDER_SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
    } else {
        $env:ORDER_SPRING_PROFILES_ACTIVE = $previousProfiles
    }
}
