# Запуск backend + Cloudflare Tunnel для удалённого доступа с телефона.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$backend = Join-Path $root "HealthApp-back\backend"

Write-Host "=== HealthApp: удалённый доступ ===" -ForegroundColor Cyan
Write-Host ""

$backendAlive = $false
try {
    Invoke-WebRequest -Uri "http://127.0.0.1:8001/" -UseBasicParsing -TimeoutSec 2 | Out-Null
    $backendAlive = $true
} catch {
    $backendAlive = $false
}

if ($backendAlive) {
    Write-Host "Backend уже доступен на http://127.0.0.1:8001" -ForegroundColor Green
} else {
    Write-Host "Backend не отвечает. Открываю отдельное окно uvicorn..." -ForegroundColor Yellow
    $command = "cd '$backend'; .\.venv\Scripts\activate; python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8001"
    Start-Process powershell.exe -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $command
    Start-Sleep -Seconds 3
}

Write-Host ""
Write-Host "Сейчас будет запущен Cloudflare Tunnel." -ForegroundColor Yellow
Write-Host "Скопируйте https://....trycloudflare.com в приложение:" -ForegroundColor Yellow
Write-Host "   Профиль -> Сервер API -> вставить URL -> Проверить -> Сохранить"
Write-Host "   или на экране входа -> Сервер API / удалённый доступ"
Write-Host ""
Write-Host "Документация: $root\docs\REMOTE_DEVELOPMENT.md"
Write-Host ""

if (Get-Command cloudflared -ErrorAction SilentlyContinue) {
    Write-Host "cloudflared найден. Запускаю туннель (Ctrl+C для остановки)..." -ForegroundColor Green
    cloudflared tunnel --url http://127.0.0.1:8001
} else {
    Write-Host "cloudflared не установлен. Скачайте: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/" -ForegroundColor Red
}
