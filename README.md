# ZhirGram

Android-клиент Telegram на Jetpack Compose и TDLib. Material 3 и Material 3 Expressive
(пружинные анимации), переключатель стиля в настройках.

Рабочее название и заготовка. Статус и план в [AGENTS.md](AGENTS.md).
**Проект ещё ни разу не собирался**, первый CI-прогон может потребовать правок.

## Как собрать (GitHub Actions)

1. Создай репозиторий, залей проект.
2. Settings → Secrets and variables → Actions, добавь секреты:
   `TG_APP_ID`, `TG_APP_HASH` (my.telegram.org), и для подписи (необязательно)
   `KEYSTORE_B64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Actions → **Build TDLib** → Run workflow. Один раз, это долго. Результат лежит в Releases (`tdlib-latest`).
4. Actions → **Build APK** → Run workflow. APK в Artifacts. Тег `v0.1.0` выложит его в Releases.

## Локальная сборка (необязательно)

Нужны JDK 21 и Android SDK 36. Скопируй `local.properties.example` в `local.properties` и впиши ключи.
Скачай `tdlib-android.zip` из Releases и распакуй в `app/src/main`
(появятся `java/org/drinkless/tdlib` и `jniLibs/arm64-v8a`). Затем `gradle :app:assembleDebug`.

## Структура

```
app/src/main/kotlin/com/qwenz/zhirgram/
  td/          TdRepository: TDLib, вход, список чатов
  data/        настройки (DataStore), UiStyle
  ui/theme/    ZhirTheme: MaterialExpressiveTheme + MotionScheme
  ui/components/  springPress, ZhirButton, ChatAvatar
  ui/auth/ ui/chats/ ui/settings/  экраны
.github/workflows/  build-tdlib.yml, build-apk.yml
.claude/skills/     material-3, material-3-expressive (справка для агента)
```

## Лицензии

TDLib: Boost Software License 1.0. Не называй приложение «Telegram» и не используй его логотип.
