param(
    [string]$RepoRoot = "D:\_dx\_Games\MC\_agent\earth_on_minecraft",
    [string]$BatchName = "image2-item-refresh-20260715",
    [string]$PromptFile = "prompts-all-128-v2.jsonl",
    [string]$InstanceRoot = "D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta",
    [int]$TimeoutSeconds = 36000
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
$BatchDir = Join-Path $RepoRoot ("tmp\imagegen\" + $BatchName)
$PromptPath = Join-Path $BatchDir $PromptFile
$ReportPath = Join-Path $BatchDir 'processing-report.json'
if (!(Test-Path -LiteralPath $PromptPath)) {
    throw "Missing image prompt file: $PromptPath"
}
$Expected = @(Get-Content -LiteralPath $PromptPath | Where-Object { $_.Trim().Length -gt 0 }).Count
$Deadline = (Get-Date).AddSeconds($TimeoutSeconds)

while ((Get-Date) -lt $Deadline) {
    if (Test-Path -LiteralPath $ReportPath) {
        try {
            $Report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
            $Completed = @($Report.completed).Count
            $Failed = @($Report.failed).Count
            $Missing = @($Report.missing).Count
            Write-Output ("image batch status completed={0} failed={1} missing={2} expected={3}" -f $Completed, $Failed, $Missing, $Expected)
            if ($Completed -eq $Expected -and $Failed -eq 0 -and $Missing -eq 0) {
                break
            }
            if ($Failed -gt 0 -and $Missing -eq 0) {
                throw "Image batch finished with $Failed failed assets."
            }
        } catch [System.Management.Automation.RuntimeException] {
            throw
        } catch {
            Write-Output ("processing report not ready: {0}" -f $_.Exception.Message)
        }
    }
    Start-Sleep -Seconds 30
}

if ((Get-Date) -ge $Deadline) {
    throw "Timed out waiting for image batch $BatchName."
}

Push-Location $RepoRoot
try {
    python '.\tools\validate_resources.py'
    if ($LASTEXITCODE -ne 0) { throw 'Resource validation failed.' }

    git diff --check
    if ($LASTEXITCODE -ne 0) { throw 'git diff --check failed.' }

    Push-Location (Join-Path $RepoRoot 'neoforge-26.2')
    try {
        .\gradlew.bat build --no-daemon --offline
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    } finally {
        Pop-Location
    }

    $Jar = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'neoforge-26.2\build\libs') -File -Filter 'earth-on-minecraft-neoforge-26.2-*.jar' |
        Where-Object { $_.Name -notmatch '(sources|javadoc)' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $Jar) { throw 'Built Earth on Minecraft jar was not found.' }

    $InstanceRoot = (Resolve-Path -LiteralPath $InstanceRoot).Path
    $Mods = Join-Path $InstanceRoot 'mods'
    New-Item -ItemType Directory -Force -Path $Mods | Out-Null
    $Backup = Join-Path $Mods '.backup-earth-on-minecraft'
    New-Item -ItemType Directory -Force -Path $Backup | Out-Null
    Get-ChildItem -LiteralPath $Mods -File -Filter 'earth-on-minecraft-neoforge-26.2-*.jar' | ForEach-Object {
        $Destination = Join-Path $Backup ($_.Name + '.bak-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        Move-Item -LiteralPath $_.FullName -Destination $Destination
    }
    $Deployed = Join-Path $Mods $Jar.Name
    Copy-Item -LiteralPath $Jar.FullName -Destination $Deployed -Force
    $Hash = (Get-FileHash -LiteralPath $Deployed -Algorithm SHA256).Hash
    Write-Output ("IMAGE2_BATCH_DEPLOYED jar={0} sha256={1}" -f $Deployed, $Hash)
} finally {
    Pop-Location
}
