# ZerOS Mobile — Wileyfox Swift 2 (marmite)

Первый целевой аппарат ZerOS Mobile. Здесь только наш код и скрипты; образы
прошивок и бэкапы в репозиторий не входят — готовая прошивка лежит в
**Releases**.

## Паспорт устройства

| Параметр | Значение |
|---|---|
| Кодовое имя | `marmite` |
| Серийник (adb) | `a886e8b4` |
| Платформа | MSM8937, Snapdragon 430 |
| Память | 2 ГБ ОЗУ / 16 ГБ ПЗУ |
| Экран | 720×1280, 320 dpi |
| Прошивка | LineageOS 17.1 (`17.1-20210703-UNOFFICIAL-marmite`), Android 10, API 29 |
| Recovery | TWRP 3.7.0_9-0 с datafix |
| Root | Magisk 31.0 |

Заводская прошивка была 7.1.2, сборка `TOS317G` (партия СНГ). Загрузчик
разблокирован.

## Что в папке

| Путь | Что это |
|---|---|
| `home/` | **ZerOS Home** — наш лаунчер (`dev.zeroc.home`), один APK, собирается без Gradle |
| `firmware/` | `flash_los.ps1` и заметки: разблокировка, TWRP, Magisk, прошивка LineageOS |

## Сборка ZerOS Home

Нужны `android.jar` (API 36.1), `d8`, `aapt2`, `apksigner` из build-tools.
Путь сборки — только ASCII (`%TEMP%`), кириллица ломает `aapt2`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "home\build.ps1"
adb install -r "home\dist\ZerOS_Home.apk"
```

Версия задаётся в `home/build.ps1` (`$VersionCode` / `$VersionName`) и в шапке
`MainActivity`; при изменении — +0.0.1.

## Рут

Лаунчеру root нужен для трёх вещей: яркость, системная тёмная тема и «Не
беспокоить». Первый запуск после установки показывает в Magisk запрос
«Разрешить root?» для `dev.zeroc.home` — без него переключатели открывают
штатные настройки Android. Подробности и таблица команд — в `home/README.md`.

## Как прошивали

Полный порядок — в `firmware/README.md`. Кратко: разблокировка загрузчика →
TWRP с datafix → LineageOS 17.1 через `flash_los.ps1` → Magisk 31.0 →
ZerOS Home.

Не трогаем: `preloader`, `lk`, `seccfg`, раздел `recovery` (TWRP остаётся
рабочим).
