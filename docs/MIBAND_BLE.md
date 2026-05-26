# Mi Band 8 — прямое BLE в HealthApp

## Что сделано

HealthApp подключается к **Xiaomi Smart Band 8** (и совместимым protobuf-устройствам) по **Bluetooth Low Energy** без Mi Fitness в фоне:

- GATT-сервис `fe95`, шифрованный канал protobuf;
- аутентификация по **auth key** (16 байт), как в [Gadgetbridge Huami/Xiaomi pairing](https://gadgetbridge.org/basics/pairing/huami-xiaomi-server/);
- синхронизация **шагов и пульса** (realtime stats) → импорт на backend HealthApp.

Путь в приложении: **Профиль → Mi Band 8 (BLE)** или **Интеграции → Настроить Mi Band BLE**.

## Подготовка (обязательно)

1. Привяжите браслет в **Mi Fitness** (`com.xiaomi.wearable`).
2. Получите **auth_key** (32 hex-символа). На rooted/adb-устройстве часто берут из БД Mi Fitness (`device_db`, поле `auth_key` в JSON). Без ключа браслет отклонит стороннее приложение.
3. Включите Bluetooth, выдайте разрешения **Bluetooth** (и **геолокация** на Android ≤ 11 при сканировании).
4. Откройте экран Mi Band BLE → вставьте ключ → **Сканировать** → выберите устройство → **Подключить**.

## Ограничения

| Функция | Статус |
|--------|--------|
| Шаги, калории, пульс (realtime) | Реализовано |
| Полный архив сна / стадии сна | Нужен разбор activity-файлов (как в Gadgetbridge), пока нет |
| Уведомления на браслет | Не реализовано |
| Прошивка / циферблаты | Не реализовано |

**Альтернатива:** Mi Fitness → **Health Connect** → импорт в HealthApp (уже есть в «Интеграции»), если сон попадает в HC.

## Технически

- Протокол: Xiaomi protobuf (открытые UUID `fe95` / `51`–`55`), реализация **с нуля** в Kotlin (не копия GPL-кода Gadgetbridge).
- Зависимости: `protobuf-javalite`, BouncyCastle (AES-CCM).
- Foreground service `connectedDevice` на время сессии BLE.

## Сборка

```bash
cd HealtApp-front
./gradlew assembleDebug
```
