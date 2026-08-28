[CmdletBinding()]
param([switch]$NoBrowser)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeDir = Join-Path $projectRoot '.lifeskill-runtime'
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'
$envFile = Join-Path $projectRoot '.env'

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

function Test-Port([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Http([string]$Name, [string]$Url, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "[ready] $Name" -ForegroundColor Green
                return
            }
        } catch { Start-Sleep -Milliseconds 700 }
    } while ((Get-Date) -lt $deadline)
    throw "$Name did not become ready. Check logs in $runtimeDir"
}

if (-not (Test-Path $envFile)) {
    throw 'Missing .env. Copy .env.example and configure PostgreSQL and DeepSeek first.'
}

$settings = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') { $settings[$matches[1].Trim()] = $matches[2].Trim() }
}
if ($settings['LIFESKILL_MODEL_ENABLED'] -ne 'true' -or [string]::IsNullOrWhiteSpace($settings['DEEPSEEK_API_KEY'])) {
    throw 'DeepSeek is not enabled. Set LIFESKILL_MODEL_ENABLED=true and DEEPSEEK_API_KEY in .env.'
}

if (-not (Test-Port 5432)) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        & docker compose -f (Join-Path $projectRoot 'compose.yaml') up -d postgres
    } else {
        $postgres = Get-Service 'postgresql*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($postgres) { Start-Service -Name $postgres.Name } else { throw 'PostgreSQL is not running and Docker is unavailable.' }
    }
}
if (-not (Test-Port 5432)) { throw 'PostgreSQL did not start on port 5432.' }
Write-Host '[ready] PostgreSQL' -ForegroundColor Green

if (-not (Test-Port 8080)) {
    $backend = Start-Process -FilePath 'pwsh' -WindowStyle Hidden -PassThru -WorkingDirectory $backendDir `
        -ArgumentList '-NoProfile', '-Command', '.\mvnw.cmd spring-boot:run' `
        -RedirectStandardOutput (Join-Path $runtimeDir 'backend.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'backend-error.log')
    Set-Content -Path (Join-Path $runtimeDir 'backend.pid') -Value $backend.Id
}

if (-not (Test-Port 5173)) {
    if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
        & npm --prefix $frontendDir install
    }
    $frontend = Start-Process -FilePath 'pwsh' -WindowStyle Hidden -PassThru -WorkingDirectory $frontendDir `
        -ArgumentList '-NoProfile', '-Command', 'npm run dev' `
        -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'frontend-error.log')
    Set-Content -Path (Join-Path $runtimeDir 'frontend.pid') -Value $frontend.Id
}

Wait-Http 'Backend' 'http://localhost:8080/actuator/health' 90
Wait-Http 'Frontend' 'http://localhost:5173' 30
Write-Host 'LifeSkill Hub is ready: http://localhost:5173' -ForegroundColor Cyan
if (-not $NoBrowser) { Start-Process 'http://localhost:5173' }
