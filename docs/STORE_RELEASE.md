# Чеклист релиза HealthApp в Google Play

## Уже сделано в коде

- [x] `applicationId` = `com.healthapp.android` (debug: `.debug`, wear: `.wear`)
- [x] `versionName` 1.1.0 / `versionCode` 2
- [x] Release signing через `keystore.properties`
- [x] Release: фиксированный HTTPS `PROD_API_BASE_URL`, без cleartext, без смены URL сервера
- [x] Debug: LAN/override разрешены
- [x] Privacy / Terms HTML + ссылки в приложении
- [x] Sentry (включается при `SENTRY_DSN`)
- [x] Alembic + Docker Compose (Postgres)
- [x] Честный демо-режим (без фейковой соцленты)
- [x] Data safety тексты: `docs/PLAY_DATA_SAFETY.md`

## Ваши шаги перед заливкой в Play

1. Создать upload keystore → заполнить `HealtApp-front/keystore.properties` (см. `.example`).
2. Выложить `docs/legal/privacy.html` и `terms.html` на HTTPS.
3. В `local.properties` задать:
   - `PROD_API_BASE_URL=https://ваш-api/`
   - `PRIVACY_POLICY_URL=…`
   - `TERMS_OF_SERVICE_URL=…`
   - `SENTRY_DSN=…` (рекомендуется)
4. Поднять API: `cd HealthApp-back && SECRET_KEY=… docker compose up -d`
5. `./gradlew :app:assembleRelease` и Internal testing в Play Console.
6. Заполнить Data safety по `docs/PLAY_DATA_SAFETY.md`.
