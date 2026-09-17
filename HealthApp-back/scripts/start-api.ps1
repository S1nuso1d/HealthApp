# Запуск API + Postgres для релиза/локальной проверки.
# Требуется запущенный Docker Desktop.

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (-not (Test-Path ".env.compose")) {
    $secret = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
    @"
SECRET_KEY=$secret
ALLOWED_ORIGINS=https://s1nuso1d.github.io,https://healthapp.app
LLM_ENABLED=false
"@ | Set-Content ".env.compose" -Encoding UTF8
    Write-Host "Created .env.compose with new SECRET_KEY"
}

$env:SECRET_KEY = (Get-Content ".env.compose" | Where-Object { $_ -match '^SECRET_KEY=' }).Substring(11)
docker compose --env-file .env.compose up -d --build
Write-Host "API: http://localhost:8001/healthz"
Write-Host "Для публичного HTTPS используйте Cloudflare Tunnel / ngrok и пропишите URL в PROD_API_BASE_URL"
