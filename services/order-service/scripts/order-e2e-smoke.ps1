[CmdletBinding()]
param(
    [string]$GatewayBaseUrl = 'http://localhost:8080',
    [string]$OrderBaseUrl = 'http://localhost:8084',

    [Parameter(Mandatory = $true)]
    [string]$CustomerAEmail,

    [Parameter(Mandatory = $true)]
    [string]$CustomerAPassword,

    [Parameter(Mandatory = $true)]
    [guid]$CartId,

    [Parameter(Mandatory = $true)]
    [guid]$AddressId,

    [ValidateSet('PREPAID', 'POSTPAID')]
    [string]$PaymentTiming = 'PREPAID',

    [ValidateSet('VNPAY', 'COD')]
    [string]$PaymentMethod = 'VNPAY',

    [string]$MerchandiseVoucherId,
    [string]$ShippingVoucherId,
    [string]$ShippingServiceCode,
    [string]$CustomerBEmail,
    [string]$CustomerBPassword
)

$ErrorActionPreference = 'Stop'
$GatewayBaseUrl = $GatewayBaseUrl.TrimEnd('/')
$OrderBaseUrl = $OrderBaseUrl.TrimEnd('/')

if ($PaymentTiming -eq 'PREPAID' -and $PaymentMethod -eq 'COD') {
    throw 'PREPAID + COD không phải cặp Payment hợp lệ.'
}

function Assert-Condition {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw "E2E assertion failed: $Message"
    }
}

function Get-AccessToken {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Email,

        [Parameter(Mandatory = $true)]
        [string]$Password
    )

    $response = Invoke-RestMethod -Method Post `
        -Uri "$GatewayBaseUrl/api/v1/auth/login" `
        -ContentType 'application/json' `
        -Body (@{ email = $Email; password = $Password } | ConvertTo-Json)

    Assert-Condition -Condition (-not [string]::IsNullOrWhiteSpace($response.accessToken)) `
        -Message "Identity không trả accessToken cho $Email."
    return $response.accessToken
}

function Get-ErrorStatusCode {
    param(
        [Parameter(Mandatory = $true)]
        $ErrorRecord
    )

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response -or $null -eq $response.StatusCode) {
        return $null
    }
    return [int]$response.StatusCode
}

function Assert-RequestRejected {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Request,

        [Parameter(Mandatory = $true)]
        [int]$ExpectedStatus,

        [Parameter(Mandatory = $true)]
        [string]$Scenario
    )

    try {
        & $Request | Out-Null
    } catch {
        $actualStatus = Get-ErrorStatusCode -ErrorRecord $_
        Assert-Condition -Condition ($actualStatus -eq $ExpectedStatus) `
            -Message "$Scenario phải trả HTTP $ExpectedStatus nhưng nhận $actualStatus."
        return
    }
    throw "E2E assertion failed: $Scenario đáng lẽ bị từ chối bằng HTTP $ExpectedStatus."
}

Write-Host '[1/8] Kiểm tra Order health...'
$health = Invoke-RestMethod -Method Get -Uri "$OrderBaseUrl/actuator/health"
Assert-Condition -Condition ($health.status -eq 'UP') -Message 'Order Service health không phải UP.'

Write-Host '[2/8] Đăng nhập Customer A...'
$tokenA = Get-AccessToken -Email $CustomerAEmail -Password $CustomerAPassword
$headersA = @{ Authorization = "Bearer $tokenA" }

Write-Host '[3/8] Tạo Checkout Session từ Cart...'
$session = Invoke-RestMethod -Method Post `
    -Uri "$GatewayBaseUrl/api/v1/checkout/sessions" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body (@{ source = 'CART'; cartId = $CartId.ToString() } | ConvertTo-Json)

Assert-Condition -Condition ($null -ne $session.id) -Message 'Checkout Session thiếu id.'
Assert-Condition -Condition ($session.source -eq 'CART') -Message 'Checkout source không phải CART.'
Assert-Condition -Condition ($session.cartId -eq $CartId.ToString()) -Message 'Checkout trả sai cartId.'
Assert-Condition -Condition ($null -ne $session.items -and $session.items.Count -gt 0) `
    -Message 'Checkout không có item server-side.'
$sessionId = $session.id

Write-Host '[4/8] Cập nhật Address và Payment selection...'
$updated = Invoke-RestMethod -Method Patch `
    -Uri "$GatewayBaseUrl/api/v1/checkout/sessions/$sessionId" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body (@{
        addressId = $AddressId.ToString()
        paymentTiming = $PaymentTiming
        paymentMethod = $PaymentMethod
    } | ConvertTo-Json)

Assert-Condition -Condition ($updated.addressId -eq $AddressId.ToString()) -Message 'Checkout trả sai addressId.'
Assert-Condition -Condition ($updated.paymentTiming -eq $PaymentTiming) -Message 'Checkout trả sai paymentTiming.'
Assert-Condition -Condition ($updated.paymentMethod -eq $PaymentMethod) -Message 'Checkout trả sai paymentMethod.'

Write-Host '[5/8] Tạo Checkout Preview và kiểm tra công thức tiền...'
$previewBody = @{}
if (-not [string]::IsNullOrWhiteSpace($MerchandiseVoucherId)) {
    $previewBody.merchandiseVoucherId = $MerchandiseVoucherId
}
if (-not [string]::IsNullOrWhiteSpace($ShippingVoucherId)) {
    $previewBody.shippingVoucherId = $ShippingVoucherId
}
if (-not [string]::IsNullOrWhiteSpace($ShippingServiceCode)) {
    $previewBody.serviceCode = $ShippingServiceCode
}

$preview = Invoke-RestMethod -Method Post `
    -Uri "$GatewayBaseUrl/api/v1/checkout/sessions/$sessionId/preview" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body ($previewBody | ConvertTo-Json)

Assert-Condition -Condition ($preview.checkoutSessionId -eq $sessionId) `
    -Message 'Preview trả sai checkoutSessionId.'
Assert-Condition -Condition ($null -ne $preview.shipping.quoteId) -Message 'Preview thiếu Shipping quote.'

$money = $preview.money
$expectedTotal = [long]$money.itemsSubtotalVnd `
    - [long]$money.productDiscountVnd `
    - [long]$money.orderDiscountVnd `
    + [long]$money.shippingFeeVnd `
    - [long]$money.shippingDiscountVnd
Assert-Condition -Condition ($expectedTotal -eq [long]$money.finalTotalVnd) `
    -Message "Sai công thức tiền: expected=$expectedTotal, actual=$($money.finalTotalVnd)."

Write-Host '[6/8] Tạo Order và replay cùng Idempotency-Key...'
$createKey = [guid]::NewGuid().ToString()
$createHeaders = @{
    Authorization = "Bearer $tokenA"
    'Idempotency-Key' = $createKey
}
$order = Invoke-RestMethod -Method Post `
    -Uri "$GatewayBaseUrl/api/v1/checkout/sessions/$sessionId/orders" `
    -Headers $createHeaders

Assert-Condition -Condition ($null -ne $order.orderId) -Message 'Create Order thiếu orderId.'
Assert-Condition -Condition (-not [bool]$order.replay) -Message 'Lần tạo đầu tiên không được là replay.'

$replay = Invoke-RestMethod -Method Post `
    -Uri "$GatewayBaseUrl/api/v1/checkout/sessions/$sessionId/orders" `
    -Headers $createHeaders

Assert-Condition -Condition ([bool]$replay.replay) -Message 'Request lặp phải trả replay=true.'
Assert-Condition -Condition ($replay.orderId -eq $order.orderId) -Message 'Replay trả orderId khác.'
Assert-Condition -Condition ($replay.sagaId -eq $order.sagaId) -Message 'Replay trả sagaId khác.'
Assert-Condition -Condition ($replay.paymentId -eq $order.paymentId) -Message 'Replay trả paymentId khác.'

Write-Host '[7/8] Xác minh key khác trên cùng Checkout bị từ chối...'
$differentKeyHeaders = @{
    Authorization = "Bearer $tokenA"
    'Idempotency-Key' = [guid]::NewGuid().ToString()
}
Assert-RequestRejected -ExpectedStatus 409 -Scenario 'Create Order bằng key khác trên cùng Checkout' -Request {
    Invoke-RestMethod -Method Post `
        -Uri "$GatewayBaseUrl/api/v1/checkout/sessions/$sessionId/orders" `
        -Headers $differentKeyHeaders
}

Write-Host '[8/8] Đọc Order và tùy chọn kiểm tra ownership chéo...'
$detail = Invoke-RestMethod -Method Get `
    -Uri "$GatewayBaseUrl/api/v1/orders/$($order.orderId)" `
    -Headers $headersA
Assert-Condition -Condition ($detail.orderId -eq $order.orderId) -Message 'Order detail trả sai orderId.'

if (-not [string]::IsNullOrWhiteSpace($CustomerBEmail) -or
    -not [string]::IsNullOrWhiteSpace($CustomerBPassword)) {
    Assert-Condition `
        -Condition (-not [string]::IsNullOrWhiteSpace($CustomerBEmail) -and
                    -not [string]::IsNullOrWhiteSpace($CustomerBPassword)) `
        -Message 'Muốn kiểm tra ownership phải truyền cả CustomerBEmail và CustomerBPassword.'

    $tokenB = Get-AccessToken -Email $CustomerBEmail -Password $CustomerBPassword
    $headersB = @{ Authorization = "Bearer $tokenB" }
    Assert-RequestRejected -ExpectedStatus 404 -Scenario 'Customer B đọc Order của Customer A' -Request {
        Invoke-RestMethod -Method Get `
            -Uri "$GatewayBaseUrl/api/v1/orders/$($order.orderId)" `
            -Headers $headersB
    }
}

[pscustomobject]@{
    Passed = $true
    CheckoutSessionId = $sessionId
    OrderId = $order.orderId
    SagaId = $order.sagaId
    PaymentId = $order.paymentId
    IdempotencyKey = $createKey
    CrossCustomerOwnershipChecked = -not [string]::IsNullOrWhiteSpace($CustomerBEmail)
}
