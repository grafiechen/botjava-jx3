param(
    [ValidateSet("formal", "sandbox", "staging")]
    [string]$Environment = "sandbox",
    [string]$Executor = "",
    [string]$EvidenceLocation = "",
    [string]$Branch = "",
    [string]$Commit = "",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

function Get-GitValue {
    param([string[]]$Arguments)
    try {
        $value = & git @Arguments 2>$null
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($value)) {
            return ($value | Select-Object -First 1).Trim()
        }
    }
    catch {
        return ""
    }
    return ""
}

if ([string]::IsNullOrWhiteSpace($Branch)) {
    $Branch = Get-GitValue @("rev-parse", "--abbrev-ref", "HEAD")
}
if ([string]::IsNullOrWhiteSpace($Commit)) {
    $Commit = Get-GitValue @("rev-parse", "HEAD")
}

$date = Get-Date -Format "yyyy-MM-dd"
$items = @(
    "JX3API online query",
    "QQ webhook and group messages",
    "QQ passive and active sending",
    "Public media domain",
    "Aliyun voice",
    "PostgreSQL multi-instance behavior",
    "Observability",
    "GitHub Actions"
)

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("### $date External Acceptance Record")
$lines.Add("")
$lines.Add("Basic information:")
$lines.Add("")
$lines.Add("- Date: $date")
$lines.Add("- Environment: $Environment")
$lines.Add("- Branch: $Branch")
$lines.Add("- Commit: $Commit")
$lines.Add("- Executor: $Executor")
$lines.Add("- Evidence location: $EvidenceLocation")
$lines.Add("")
$lines.Add("Acceptance items:")
$lines.Add("")
$lines.Add("| Item | Result | Redacted evidence |")
$lines.Add("| --- | --- | --- |")
foreach ($item in $items) {
    $lines.Add("| $item | Not executed | To be filled |")
}
$lines.Add("")
$lines.Add("Exceptions and follow-up:")
$lines.Add("")
$lines.Add("- Failed items: none")
$lines.Add("- Risk: fill after real environment acceptance")
$lines.Add("- Next action: update this record with real results and append it to docs/PROJECT_DESIGN.md")
$lines.Add("")
$lines.Add("Sensitive data reminder: do not write real token, ticket, secret, openid, message body, role private data, full upstream response, or full platform request body.")

$content = ($lines -join [Environment]::NewLine) + [Environment]::NewLine

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output $content
}
else {
    $resolved = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputPath)
    $directory = [System.IO.Path]::GetDirectoryName($resolved)
    if (-not [string]::IsNullOrWhiteSpace($directory) -and -not [System.IO.Directory]::Exists($directory)) {
        [System.IO.Directory]::CreateDirectory($directory) | Out-Null
    }
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($resolved, $content, $utf8NoBom)
    Write-Output "External acceptance record draft written to $resolved. Values were not printed."
}
