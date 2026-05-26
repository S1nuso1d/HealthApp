# Удалённая работа с HealthApp (вне дома)

Телефон по USB к ПК **не обязателен**. Чтобы приложение работало на улице, телефон должен достучаться до backend по **интернету** (обычно HTTPS-туннель на ваш домашний ПК).

## Схема

```
Телефон (LTE/Wi‑Fi) → https://xxxx.trycloudflare.com → туннель → ПК :8001 → uvicorn
```

## 1. На домашнем ПК (один раз за сессию)

### Backend

```powershell
cd C:\Project\HealthApp\HealthApp-back\backend
.\.venv\Scripts\activate
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

Проверка: http://127.0.0.1:8001/docs

### Туннель (рекомендуется Cloudflare — бесплатно)

Установите [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/), затем:

```powershell
cloudflared tunnel --url http://127.0.0.1:8001
```

В консоли появится строка вида:

```text
https://something-random.trycloudflare.com
```

Скопируйте этот **https**-адрес.

**Альтернатива:** [ngrok](https://ngrok.com/) — `ngrok http 8001`

### Скрипт (опционально)

```powershell
C:\Project\HealthApp\scripts\start-remote-tunnel.ps1
```

## 2. На телефоне

1. Установите/запустите приложение (debug).
2. Если вы ещё не вошли: **экран входа → Сервер API / удалённый доступ**.
3. Если вы уже вошли: **Профиль → Сервер API**.
4. Вставьте URL: `https://something-random.trycloudflare.com/` (со слэшем в конце или без — приложение нормализует).
5. **Проверить связь** → **Сохранить и применить**.
6. Войдите в аккаунт как обычно.

Отключите **VPN** на телефоне, если локальная сеть/туннель не открывается.

## 3. Дома (та же Wi‑Fi, без туннеля)

**Профиль → Сервер API → Домашняя Wi‑Fi** — используется адрес из `HealtApp-front/local.properties`:

```properties
API_BASE_URL=http://192.168.31.XXX:8001/
```

После смены IP ПК обновите `API_BASE_URL` и пересоберите приложение, либо вручную введите новый IP на экране «Сервер API».

## 4. Эмулятор Android Studio

Пресет **Эмулятор** или URL: `http://10.0.2.2:8001/`

## 5. USB + adb (только рядом с ПК)

Если телефон подключён по кабелю и не нужен интернет-туннель:

```powershell
adb reverse tcp:8001 tcp:8001
```

В приложении: `http://127.0.0.1:8001/` — трафик идёт на ПК через USB.

## Важно

| Тема | Рекомендация |
|------|----------------|
| ПК спит | Туннель не работает; нужен VPS или облачный backend |
| URL туннеля | Меняется при каждом запуске cloudflared (бесплатный режим) |
| Безопасность | Не публикуйте туннель публично надолго; для прода — VPS + HTTPS + сильный SECRET_KEY |
| Фаервол | Разрешите Python/порт 8001 в частной сети |

## Постоянный удалённый сервер (следующий шаг)

Чтобы не держать домашний ПК включённым: развернуть backend на Railway / VPS, в приложении указать постоянный `https://api.yourdomain.com/`.
