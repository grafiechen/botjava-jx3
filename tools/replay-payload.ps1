param(
    [Parameter(Position = 0)]
    [string]$PayloadPath = "docs/testing/payloads/group-message-create.json",

    [Parameter(Position = 1)]
    [string]$Endpoint = "http://localhost:8081/bot/message",

    [string]$Signature,

    [string]$Timestamp
)

$ErrorActionPreference = "Stop"
$resolvedPath = (Resolve-Path -LiteralPath $PayloadPath).Path
$utf8 = [System.Text.UTF8Encoding]::new($false, $true)
$json = [System.IO.File]::ReadAllText($resolvedPath, $utf8)
[void]($json | ConvertFrom-Json)

$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($Signature)) {
    $headers["X-Signature-Ed25519"] = $Signature
}
if (-not [string]::IsNullOrWhiteSpace($Timestamp)) {
    $headers["X-Signature-Timestamp"] = $Timestamp
}

$response = Invoke-RestMethod `
    -Method Post `
    -Uri $Endpoint `
    -ContentType "application/json; charset=utf-8" `
    -Headers $headers `
    -Body $utf8.GetBytes($json)

if ($null -ne $response) {
    $response | ConvertTo-Json -Depth 20
}
