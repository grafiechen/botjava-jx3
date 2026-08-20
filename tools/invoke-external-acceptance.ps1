param(
    [ValidateSet("all", "jx3", "qq-openapi", "qq-group", "minio", "postgres", "sound", "observability", "production")]
    [string]$Scope = "all",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$preflight = Join-Path $PSScriptRoot "check-external-acceptance-env.ps1"

function Invoke-Preflight {
    param([string]$Name)
    $commandText = "./tools/check-external-acceptance-env.ps1 -Scope $Name"
    if ($DryRun) {
        Write-Output "[dry-run] $commandText"
        return
    }
    & $preflight -Scope $Name
}

function Invoke-SmokeTest {
    param(
        [string]$Name,
        [string]$TestClass
    )
    $commandText = "mvn ""-Dtest=$TestClass"" test"
    if ($DryRun) {
        Write-Output "[dry-run] $commandText"
        return
    }
    & mvn "-Dtest=$TestClass" test
    if ($LASTEXITCODE -ne 0) {
        throw "External acceptance smoke test failed for scope '$Name'."
    }
}

function Invoke-Scope {
    param([string]$Name)
    Invoke-Preflight $Name
    switch ($Name) {
        "jx3" { Invoke-SmokeTest $Name "Jx3ApiLiveSmokeIT" }
        "qq-openapi" { Invoke-SmokeTest $Name "QqOpenApiLiveSmokeIT" }
        "qq-group" { Invoke-SmokeTest $Name "QqGroupMessageLiveSmokeIT" }
        "minio" { Invoke-SmokeTest $Name "MinioMediaDomainLiveSmokeIT" }
        "postgres" { Invoke-SmokeTest $Name "PostgresTwoInstanceLiveSmokeIT" }
        "sound" { Invoke-SmokeTest $Name "SoundConverterLiveSmokeIT" }
        "observability" { Invoke-SmokeTest $Name "ObservabilityLiveSmokeIT" }
        "production" {
            if ($DryRun) {
                Write-Output "[dry-run] production scope runs preflight only; no external service smoke test will be called."
                return
            }
            Write-Output "Production preflight passed. This scope validates required deployment variables only and does not call external services."
        }
        default {
            throw "Unsupported external acceptance scope: $Name"
        }
    }
}

if ($Scope -eq "all") {
    foreach ($name in @("jx3", "qq-openapi", "qq-group", "minio", "postgres", "sound", "observability", "production")) {
        Invoke-Scope $name
    }
}
else {
    Invoke-Scope $Scope
}

Write-Output "External acceptance runner finished for scope '$Scope'. Values were not printed."
