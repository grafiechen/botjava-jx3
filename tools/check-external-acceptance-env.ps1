param(
    [ValidateSet("all", "jx3", "qq-openapi", "qq-group", "minio", "postgres", "sound", "observability", "production")]
    [string]$Scope = "all"
)

$ErrorActionPreference = "Stop"

function Test-EnvPresent {
    param([string]$Name)
    $value = [Environment]::GetEnvironmentVariable($Name)
    return -not [string]::IsNullOrWhiteSpace($value)
}

function Add-Check {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Group,
        [string]$Name,
        [bool]$Passed,
        [string]$Message
    )
    $Results.Add([pscustomobject]@{
        Group = $Group
        Name = $Name
        Passed = $Passed
        Message = $Message
    })
}

function Add-Required {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Group,
        [string[]]$Names
    )
    foreach ($name in $Names) {
        Add-Check $Results $Group $name (Test-EnvPresent $name) "required"
    }
}

function Add-Exact {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Group,
        [string]$Name,
        [string]$Expected
    )
    $actual = [Environment]::GetEnvironmentVariable($Name)
    Add-Check $Results $Group $Name ($actual -eq $Expected) "must equal expected confirmation"
}

function Add-UrlScheme {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Group,
        [string]$Name,
        [string]$Prefix
    )
    $actual = [Environment]::GetEnvironmentVariable($Name)
    Add-Check $Results $Group $Name (
        -not [string]::IsNullOrWhiteSpace($actual) -and
        $actual.StartsWith($Prefix, [System.StringComparison]::OrdinalIgnoreCase)
    ) "must start with $Prefix"
}

function Add-Jx3Checks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "jx3" "JX3API_LIVE_SMOKE" "true"
    Add-Required $Results "jx3" @(
        "JX3API_API_TOKEN",
        "JX3API_TICKET",
        "JX3API_SMOKE_SERVER",
        "JX3API_SMOKE_ROLE",
        "JX3API_SMOKE_UID"
    )
}

function Add-QqOpenApiChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "qq-openapi" "QQ_OPENAPI_LIVE_SMOKE" "true"
    Add-Required $Results "qq-openapi" @("TX_BOT_APP_ID", "TX_BOT_APP_SECRET")
}

function Add-QqGroupChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "qq-group" "QQ_GROUP_LIVE_SMOKE" "true"
    Add-Exact $Results "qq-group" "QQ_GROUP_LIVE_CONFIRM" "SEND_TO_TEST_GROUP"
    Add-Required $Results "qq-group" @(
        "QQ_GROUP_LIVE_MODE",
        "QQ_GROUP_LIVE_OPENID",
        "TX_BOT_APP_ID",
        "TX_BOT_APP_SECRET"
    )

    $mode = [Environment]::GetEnvironmentVariable("QQ_GROUP_LIVE_MODE")
    if ($null -ne $mode) {
        $mode = $mode.ToUpperInvariant()
    }
    switch ($mode) {
        "REPLY" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID") }
        "REFERENCE" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID") }
        "EVENT" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_EVENT_ID") }
        "IMAGE" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID", "QQ_GROUP_LIVE_IMAGE_URL") }
        "AUDIO" {
            Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID", "QQ_GROUP_LIVE_AUDIO_URL")
            Add-UrlScheme $Results "qq-group" "QQ_GROUP_LIVE_AUDIO_URL" "https://"
        }
        "MARKDOWN" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID") }
        "ARK" { Add-Required $Results "qq-group" @("QQ_GROUP_LIVE_MSG_ID") }
        "ACTIVE" { }
        default {
            Add-Check $Results "qq-group" "QQ_GROUP_LIVE_MODE" $false `
                "must be REPLY, REFERENCE, EVENT, ACTIVE, IMAGE, AUDIO, MARKDOWN, or ARK"
        }
    }
}

function Add-MinioChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "minio" "MINIO_LIVE_SMOKE" "true"
    Add-Exact $Results "minio" "MINIO_LIVE_CONFIRM" "UPLOAD_AND_FETCH_TEST_IMAGE"
    Add-Required $Results "minio" @(
        "MINIO_ENDPOINT",
        "MINIO_ACCESS_KEY",
        "MINIO_SECRET_KEY",
        "MINIO_BUCKET"
    )
    Add-UrlScheme $Results "minio" "MINIO_PUBLIC_URL" "https://"
}

function Add-PostgresChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "postgres" "POSTGRES_LIVE_SMOKE" "true"
    Add-Exact $Results "postgres" "POSTGRES_LIVE_CONFIRM" "USE_DEDICATED_SMOKE_SCHEMA"
    Add-Required $Results "postgres" @(
        "POSTGRES_LIVE_USERNAME",
        "POSTGRES_LIVE_PASSWORD",
        "POSTGRES_LIVE_SCHEMA"
    )
    Add-UrlScheme $Results "postgres" "POSTGRES_LIVE_JDBC_URL" "jdbc:postgresql://"

    $schema = [Environment]::GetEnvironmentVariable("POSTGRES_LIVE_SCHEMA")
    $jdbcUrl = [Environment]::GetEnvironmentVariable("POSTGRES_LIVE_JDBC_URL")
    $schemaLooksSafe = -not [string]::IsNullOrWhiteSpace($schema) -and
        $schema -cmatch '^botjava_smoke_[a-z0-9_]{1,40}$'
    Add-Check $Results "postgres" "POSTGRES_LIVE_SCHEMA" $schemaLooksSafe `
        "must match botjava_smoke_[a-z0-9_]"
    $urlContainsSchema = -not [string]::IsNullOrWhiteSpace($schema) -and
        -not [string]::IsNullOrWhiteSpace($jdbcUrl) -and
        $jdbcUrl.Contains("currentSchema=$schema", [System.StringComparison]::Ordinal)
    Add-Check $Results "postgres" "POSTGRES_LIVE_JDBC_URL" $urlContainsSchema `
        "must contain currentSchema matching POSTGRES_LIVE_SCHEMA"
}

function Add-SoundChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "sound" "JX3_SOUND_LIVE_SMOKE" "true"
    Add-Exact $Results "sound" "JX3_SOUND_ENABLED" "true"
    Add-Required $Results "sound" @(
        "JX3API_API_TOKEN",
        "JX3_SOUND_APPKEY",
        "JX3_SOUND_ACCESS",
        "JX3_SOUND_SECRET"
    )
}

function Add-ObservabilityChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "observability" "OBSERVABILITY_LIVE_SMOKE" "true"
    Add-Required $Results "observability" @("OBSERVABILITY_PROMETHEUS_URL")
    Add-UrlScheme $Results "observability" "OBSERVABILITY_PROMETHEUS_URL" "http"
}

function Add-ProductionChecks {
    param([System.Collections.Generic.List[object]]$Results)
    Add-Exact $Results "production" "BOT_COMMAND_RUNTIME_MODE" "PRODUCTION"
    Add-Required $Results "production" @(
        "TX_BOT_APP_ID",
        "TX_BOT_APP_SECRET",
        "JX3API_API_TOKEN",
        "JX3API_TICKET",
        "JX3API_DEFAULT_SERVER",
        "DB_USERNAME",
        "DB_PASSWORD",
        "MINIO_ENDPOINT",
        "MINIO_PUBLIC_URL",
        "MINIO_ACCESS_KEY",
        "MINIO_SECRET_KEY",
        "MINIO_BUCKET"
    )
    Add-UrlScheme $Results "production" "MINIO_PUBLIC_URL" "https://"
}

$results = [System.Collections.Generic.List[object]]::new()

switch ($Scope) {
    "all" {
        Add-Jx3Checks $results
        Add-QqOpenApiChecks $results
        Add-QqGroupChecks $results
        Add-MinioChecks $results
        Add-PostgresChecks $results
        Add-SoundChecks $results
        Add-ObservabilityChecks $results
        Add-ProductionChecks $results
    }
    "jx3" { Add-Jx3Checks $results }
    "qq-openapi" { Add-QqOpenApiChecks $results }
    "qq-group" { Add-QqGroupChecks $results }
    "minio" { Add-MinioChecks $results }
    "postgres" { Add-PostgresChecks $results }
    "sound" { Add-SoundChecks $results }
    "observability" { Add-ObservabilityChecks $results }
    "production" { Add-ProductionChecks $results }
}

$failed = $results | Where-Object { -not $_.Passed }
$results | Sort-Object Group, Name | Format-Table Group, Name, Passed, Message -AutoSize

if ($failed.Count -gt 0) {
    Write-Error "External acceptance environment preflight failed: $($failed.Count) check(s) did not pass."
    exit 1
}

Write-Output "External acceptance environment preflight passed for scope '$Scope'. Values were not printed."
