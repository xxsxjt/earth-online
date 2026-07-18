param(
    [Parameter(Mandatory = $true)]
    [string]$PromptJsonl,
    [string]$BatchName = "",
    [string]$Model = "gpt-image-2",
    [string]$Quality = "low",
    [string]$Size = "1024x1024",
    [string]$BaseUrl = "",
    [string]$BackupBaseUrl = "https://api.xxssxx.top/v1",
    [ValidateSet("auto", "primary", "backup", "agnes")]
    [string]$ProviderMode = "auto",
    [ValidateRange(1, 12)]
    [int]$MaxConcurrent = 4,
    [ValidateRange(1, 12)]
    [int]$MaxAttempts = 6
)
$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$PromptJsonl = (Resolve-Path -LiteralPath $PromptJsonl).Path
if ([string]::IsNullOrWhiteSpace($BatchName)) {
    $BatchName = Split-Path -Leaf (Split-Path -Parent $PromptJsonl)
}
$OutRoot = Join-Path $RepoRoot ("tmp\imagegen\" + $BatchName)
New-Item -ItemType Directory -Force -Path $OutRoot | Out-Null
$ImageScript = 'C:\Users\du_ji\.codex\skills\.system\imagegen\scripts\image_gen.py'
if (!(Test-Path -LiteralPath $ImageScript)) {
    throw "Missing image generation script: $ImageScript"
}
$jobs = Get-Content -LiteralPath $PromptJsonl | Where-Object { $_.Trim().Length -gt 0 } | ForEach-Object { $_ | ConvertFrom-Json }
if (!$jobs) {
    throw "No prompt jobs found in $PromptJsonl"
}
$runningProcesses = @()
foreach ($job in $jobs) {
    $id = [string]$job.id
    if ([string]::IsNullOrWhiteSpace($id)) { throw 'Every prompt line needs an id.' }
    $safeId = $id -replace '[^A-Za-z0-9_.-]', '_'
    $prompt = [string]$job.prompt
    if ([string]::IsNullOrWhiteSpace($prompt)) { throw "Job $id has an empty prompt." }
    $promptPath = Join-Path $OutRoot ($safeId + '.prompt.txt')
    Set-Content -LiteralPath $promptPath -Value $prompt -Encoding UTF8
    if ($job.PSObject.Properties.Name -contains 'out' -and ![string]::IsNullOrWhiteSpace([string]$job.out)) {
        $outPath = [string]$job.out
        if (![System.IO.Path]::IsPathRooted($outPath)) { $outPath = Join-Path $RepoRoot $outPath }
    } else {
        $outPath = Join-Path $OutRoot ($safeId + '.png')
    }
    $runner = Join-Path $OutRoot ('run_' + $safeId + '.ps1')
    $stdout = Join-Path $OutRoot ($safeId + '.out.log')
    $stderr = Join-Path $OutRoot ($safeId + '.err.log')
    $runnerContent = @"
`$ErrorActionPreference = 'Stop'
`$ConfigPath = 'C:\Users\du_ji\.codex\config.toml'
`$cfg = if (Test-Path -LiteralPath `$ConfigPath) { Get-Content -LiteralPath `$ConfigPath -Raw } else { '' }
`$base = [regex]::Match(`$cfg, 'base_url\s*=\s*"([^\"]+)"').Groups[1].Value
`$baseOverride = '$BaseUrl'
`$backupBase = '$BackupBaseUrl'
`$providerMode = '$ProviderMode'
`$primarySecretPath = 'C:\Users\du_ji\.agents\secrets\image2-openai-key.clixml'
`$backupSecretPath = 'C:\Users\du_ji\.agents\secrets\image2-backup-key.clixml'
`$providers = [System.Collections.Generic.List[object]]::new()

if ('$Model' -like 'agnes-*' -or `$providerMode -eq 'agnes') {
    if (Test-Path -LiteralPath 'C:\_dx\files\ls.txt') {
        `$providers.Add([pscustomobject]@{
            Name = 'Agnes'
            BaseUrl = if (![string]::IsNullOrWhiteSpace(`$baseOverride)) { `$baseOverride } else { 'https://apihub.agnes-ai.com/v1' }
            SecretPath = ''
            PlainKeySource = 'C:\_dx\files\ls.txt'
        })
    }
} else {
    if (`$providerMode -in @('auto', 'primary') -and (Test-Path -LiteralPath `$primarySecretPath)) {
        `$providers.Add([pscustomobject]@{
            Name = 'primary image-2'
            BaseUrl = if (![string]::IsNullOrWhiteSpace(`$baseOverride)) { `$baseOverride } else { `$base }
            SecretPath = `$primarySecretPath
            PlainKeySource = ''
        })
    }
    if (`$providerMode -in @('auto', 'backup') -and (Test-Path -LiteralPath `$backupSecretPath) -and ![string]::IsNullOrWhiteSpace(`$backupBase)) {
        `$providers.Add([pscustomobject]@{
            Name = 'backup image-2'
            BaseUrl = `$backupBase
            SecretPath = `$backupSecretPath
            PlainKeySource = ''
        })
    }
    `$token = [regex]::Match(`$cfg, 'experimental_bearer_token\s*=\s*"([^\"]+)"').Groups[1].Value
    if (`$providerMode -in @('auto', 'primary') -and ![string]::IsNullOrWhiteSpace(`$token)) {
        `$providers.Add([pscustomobject]@{
            Name = 'Codex config token'
            BaseUrl = if (![string]::IsNullOrWhiteSpace(`$baseOverride)) { `$baseOverride } else { `$base }
            SecretPath = ''
            PlainKeySource = 'config-token'
        })
    }
}

if (`$providers.Count -eq 0) { throw 'No compatible image provider is configured.' }
`$prompt = Get-Content -LiteralPath '$promptPath' -Raw
`$lastExitCode = 1
foreach (`$provider in `$providers) {
    if ([string]::IsNullOrWhiteSpace(`$provider.BaseUrl)) { continue }
    Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
    `$env:OPENAI_BASE_URL = `$provider.BaseUrl
    if (![string]::IsNullOrWhiteSpace(`$provider.SecretPath)) {
        `$secret = Import-Clixml -LiteralPath `$provider.SecretPath
        `$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(`$secret)
        try { `$env:OPENAI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(`$bstr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR(`$bstr) }
    } elseif (`$provider.PlainKeySource -eq 'config-token') {
        `$env:OPENAI_API_KEY = `$token
    } elseif (![string]::IsNullOrWhiteSpace(`$provider.PlainKeySource)) {
        `$env:OPENAI_API_KEY = (Get-Content -LiteralPath `$provider.PlainKeySource -Raw).Trim()
    }
    if ([string]::IsNullOrWhiteSpace(`$env:OPENAI_API_KEY)) { continue }
    `$attemptLimit = if (`$provider.Name -eq 'backup image-2') { $MaxAttempts } else { 1 }
    for (`$attempt = 1; `$attempt -le `$attemptLimit; `$attempt++) {
        Write-Output ("trying image provider: {0} attempt={1}/{2}" -f `$provider.Name, `$attempt, `$attemptLimit)
        python '$ImageScript' generate --model '$Model' --quality '$Quality' --size '$Size' --prompt `$prompt --out '$outPath' --force
        `$lastExitCode = `$LASTEXITCODE
        if (`$lastExitCode -eq 0) { break }
        if (`$attempt -lt `$attemptLimit) {
            Start-Sleep -Seconds ([Math]::Min(120, 15 * `$attempt))
        }
    }
    if (`$lastExitCode -eq 0) { break }
}
Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:OPENAI_BASE_URL -ErrorAction SilentlyContinue
if (`$lastExitCode -ne 0) { exit `$lastExitCode }
"@
    Set-Content -LiteralPath $runner -Value $runnerContent -Encoding UTF8
    while (@($runningProcesses | Where-Object { !$_.HasExited }).Count -ge $MaxConcurrent) {
        Start-Sleep -Seconds 2
        $runningProcesses = @($runningProcesses | Where-Object { !$_.HasExited })
    }
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $runner) -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    $runningProcesses += $process
    Write-Output ("started {0} pid={1} out={2}" -f $safeId, $process.Id, $outPath)
}
