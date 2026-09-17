# Что уже сделано локально

## 1. Keystore ✅
- Файл: `HealtApp-front/healthapp-upload.jks`
- Конфиг: `HealtApp-front/keystore.properties` (в .gitignore)
- Бэкап паролей: `HealtApp-front/keystore.credentials.backup.txt` — **сохраните отдельно**, без него нельзя обновлять приложение в Play с тем же ключом.

## 2. local.properties ✅
Заполнено: `API_BASE_URL`, `PROD_API_BASE_URL`, privacy/terms URL под GitHub Pages, `SENTRY_DSN` пустой.

## 3. Legal HTML → HTTPS (нужен один шаг в GitHub)
После push ветки `main` с папкой `docs/`:
1. GitHub → repo **HealthApp** → **Settings → Pages**
2. Source: **Deploy from a branch**
3. Branch: `main` / folder: `/docs`
4. Через ~1 мин откроются:
   - https://s1nuso1d.github.io/HealthApp/legal/privacy.html
   - https://s1nuso1d.github.io/HealthApp/legal/terms.html

## 4. API + Postgres ✅ (локально)
```powershell
cd HealthApp-back
.\scripts\start-api.ps1
```
Сейчас: `http://localhost:8001/healthz` отвечает ok.
Secret: `HealthApp-back/.env.compose` (+ `.compose-secret.backup.txt`).

Для **release APK** нужен публичный HTTPS (Cloudflare Tunnel / VPS). Пример:
```powershell
cloudflared tunnel --url http://localhost:8001
```
Полученный `https://….trycloudflare.com/` пропишите в `PROD_API_BASE_URL` в `local.properties`.

## 5. Internal testing Play
```powershell
cd HealtApp-front
.\scripts\build-release-aab.ps1
```
Затем Play Console → Testing → Internal testing → Upload `app-release.aab`.
Data safety: `docs/PLAY_DATA_SAFETY.md`.

## 6. Демо-режим ✅ удалён
Кнопки «Попробовать демо» больше нет; guest/LocalDemoData вычищены.
