param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$extensions = @(
    ".java", ".md", ".html", ".json", ".yml", ".yaml",
    ".sql", ".ps1", ".xml", ".properties"
)
$strictUtf8 = [System.Text.UTF8Encoding]::new($false, $true)
$invalidFiles = @()

$files = Get-ChildItem -LiteralPath $Root -Recurse -File | Where-Object {
    $extensions -contains $_.Extension.ToLowerInvariant() -and
    $_.FullName -notmatch '[\\/](\.git|target)[\\/]'
}

foreach ($file in $files) {
    try {
        [void]$strictUtf8.GetString([System.IO.File]::ReadAllBytes($file.FullName))
    }
    catch {
        $invalidFiles += $file.FullName
    }
}

if ($invalidFiles.Count -gt 0) {
    $invalidFiles | ForEach-Object { Write-Error "Invalid UTF-8: $_" }
    exit 1
}

Write-Output "UTF-8 verification passed: $($files.Count) files."
