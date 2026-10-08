# Windows PowerShell launcher for EVOQ EMS Backend with .env auto-load
$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "[EVOQ EMS] Loading configuration from backend\.env..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and ($line -match '^\s*([^#=]+)\s*=\s*(.*)$')) {
            $k = $matches[1].Trim()
            $v = $matches[2].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
} else {
    Write-Warning "[EVOQ EMS] No backend\.env found. Using system defaults."
}

Write-Host "[EVOQ EMS] Starting Spring Boot backend on port $($env:SERVER_PORT)..." -ForegroundColor Green
& (Join-Path $PSScriptRoot "mvnw.cmd") spring-boot:run
