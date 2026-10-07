param(
    [ValidateSet("doctor", "run", "smoke")]
    [string]$Command = "doctor",
    [string]$BaseUrl = "http://localhost:8082"
)

$ErrorActionPreference = "Stop"
$serviceDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $serviceDir ".env"

function Import-CatalogEnv {
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw "Thiếu $envFile. Hãy sao chép .env.example thành .env và điền thông tin PostgreSQL/JWT."
    }
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) { continue }
        $parts = $trimmed.Split("=", 2)
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
}

function Invoke-Doctor {
    Import-CatalogEnv
    Write-Host "[1/3] Java"
    & java -version
    if ($LASTEXITCODE -ne 0) { throw "Không tìm thấy Java. Catalog yêu cầu JDK 17." }

    Write-Host "[2/3] PostgreSQL"
    $dbUrl = $env:DB_URL
    if ($dbUrl -notmatch '^jdbc:postgresql://(?<host>[^:/]+):(?<port>\d+)/(?<database>[^?]+)') {
        throw "DB_URL không đúng dạng jdbc:postgresql://host:port/database."
    }
    $databaseHost = $Matches.host
    $databasePort = [int]$Matches.port
    if (-not (Test-NetConnection -ComputerName $databaseHost -Port $databasePort -InformationLevel Quiet)) {
        throw "PostgreSQL chưa lắng nghe tại ${databaseHost}:${databasePort}. Hãy khởi động dịch vụ PostgreSQL trước."
    }
    Write-Host "PostgreSQL sẵn sàng tại ${databaseHost}:${databasePort}."

    Write-Host "[3/3] Cấu hình bắt buộc"
    foreach ($name in @("DB_USERNAME", "DB_PASSWORD", "JWT_HMAC_SECRET_BASE64", "INTERNAL_API_KEY")) {
        $value = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrWhiteSpace($value)) { throw "Biến $name đang trống trong .env." }
    }
    Write-Host "Doctor hoàn tất. Chế độ standalone-demo không cần RabbitMQ, Identity, Cart hoặc Order Service."
}

function Invoke-Run {
    Import-CatalogEnv
    $env:SPRING_PROFILES_ACTIVE = "local,standalone-demo"
    $env:OUTBOX_ENABLED = "false"
    Push-Location $serviceDir
    try {
        & (Join-Path $serviceDir "mvnw.cmd") clean spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw "catalog-service kết thúc với mã lỗi $LASTEXITCODE." }
    } finally {
        Pop-Location
    }
}

function Invoke-Smoke {
    Import-CatalogEnv
    $health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
    if ($health.status -ne "UP") { throw "Health check không ở trạng thái UP." }

    $products = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/catalog/products?sort=BEST_SELLER&size=10"
    $first = $products.data.content | Select-Object -First 1
    if ($null -eq $first) { throw "Catalog demo chưa có sản phẩm seed." }
    if (-not $first.bestSeller) { throw "Sản phẩm đầu tiên chưa được đánh dấu bán chạy." }
    if ($null -eq $first.representativePrice.salePriceVnd -or
        $first.representativePrice.salePriceVnd -ge $first.representativePrice.listPriceVnd) {
        throw "Giá direct-sale demo chưa được áp dụng."
    }

    $combined = Invoke-RestMethod -Method Get -Uri `
        "$BaseUrl/api/v1/catalog/products?keyword=Dynamic&categoryId=$($first.category.id)&minimumPriceVnd=1&maximumPriceVnd=50000000&attribute=BRAND:SAMSUNG&sort=PRICE_ASC&size=10"
    if ($combined.data.content.Count -lt 1) { throw "Tìm kiếm/lọc kết hợp không trả dữ liệu mong đợi." }

    $customerBody = @{ email = "demo@dynamicmart.local"; password = "Demo@123" } | ConvertTo-Json
    $customerLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/catalog/demo/auth/login" `
        -ContentType "application/json" -Body $customerBody -SessionVariable customerSession
    $customerHeaders = @{ Authorization = "Bearer $($customerLogin.data.accessToken)" }

    $cartBody = @{
        productId = $first.id
        variantId = $first.representativeVariantId
        quantity = 1
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/catalog/demo/cart/items" `
        -Headers $customerHeaders -ContentType "application/json" -Body $cartBody

    $checkoutBody = @{
        source = "BUY_NOW"
        variantId = $first.representativeVariantId
        quantity = 1
    } | ConvertTo-Json
    $checkout = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/catalog/demo/checkout/sessions" `
        -Headers $customerHeaders -ContentType "application/json" -Body $checkoutBody
    $loadedCheckout = Invoke-RestMethod -Method Get `
        -Uri "$BaseUrl/api/v1/catalog/demo/checkout/sessions/$($checkout.data.id)" -Headers $customerHeaders
    if ($loadedCheckout.data.variantId -ne $first.representativeVariantId) {
        throw "Phiên Mua ngay trả về sai Variant."
    }

    $internalHeaders = @{
        "X-Internal-Api-Key" = $env:INTERNAL_API_KEY
        "Idempotency-Key" = [guid]::NewGuid().ToString()
    }
    $reserveBody = @{
        checkoutSessionId = [guid]::NewGuid().ToString()
        expiresAt = [DateTimeOffset]::UtcNow.AddMinutes(15).ToString("o")
        items = @(@{ variantId = $first.representativeVariantId; quantity = 1 })
    } | ConvertTo-Json -Depth 4
    $reservation = Invoke-RestMethod -Method Post `
        -Uri "$BaseUrl/api/v1/catalog/internal/inventory/reservations" `
        -Headers $internalHeaders -ContentType "application/json" -Body $reserveBody
    $reservationReplay = Invoke-RestMethod -Method Post `
        -Uri "$BaseUrl/api/v1/catalog/internal/inventory/reservations" `
        -Headers $internalHeaders -ContentType "application/json" -Body $reserveBody
    if ($reservation.data.reservationId -ne $reservationReplay.data.reservationId) {
        throw "Reserve lặp lại không trả đúng kết quả idempotent."
    }
    $releaseHeaders = @{
        "X-Internal-Api-Key" = $env:INTERNAL_API_KEY
        "Idempotency-Key" = [guid]::NewGuid().ToString()
    }
    $releaseBody = @{ reason = "STANDALONE_DEMO_SMOKE" } | ConvertTo-Json
    $released = Invoke-RestMethod -Method Post `
        -Uri "$BaseUrl/api/v1/catalog/internal/inventory/reservations/$($reservation.data.reservationId)/release" `
        -Headers $releaseHeaders -ContentType "application/json" -Body $releaseBody
    if ($released.data.status -ne "RELEASED") { throw "Luồng trả tồn demo không thành công." }

    $adminBody = @{ email = "admin@dynamicmart.local"; password = "Demo@123" } | ConvertTo-Json
    $adminLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/catalog/demo/auth/login" `
        -ContentType "application/json" -Body $adminBody
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.data.accessToken)" }
    $inventory = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/catalog/admin/inventory?size=10" `
        -Headers $adminHeaders
    if ($inventory.data.content.Count -lt 1) { throw "Trang quản trị chưa đọc được tồn kho." }

    Write-Host "SMOKE PASS: search/filter, direct-sale, bestseller, auth, cart/buy-now, inventory idempotency và admin."
    Write-Host "Product: $($first.name) | Checkout session: $($checkout.data.id)"
}

switch ($Command) {
    "doctor" { Invoke-Doctor }
    "run" { Invoke-Run }
    "smoke" { Invoke-Smoke }
}
