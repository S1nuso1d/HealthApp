# Сборка подписанного AAB для Internal testing в Play Console.
# Нужны: keystore.properties, Docker/API не обязателен для самой сборки.

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (-not (Test-Path "keystore.properties")) {
    throw "Нет keystore.properties — сначала создайте upload keystore"
}

.\gradlew.bat :app:bundleRelease
Write-Host ""
Write-Host "AAB: app\build\outputs\bundle\release\app-release.aab"
Write-Host "Дальше: Play Console → Testing → Internal testing → Create release → Upload AAB"
Write-Host "Data safety: заполните по docs/PLAY_DATA_SAFETY.md"
