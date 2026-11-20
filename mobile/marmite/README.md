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
| `home/` | **HomeWatch** — стоковый AOSP Launcher3 (`Launcher3QuickStep`, `android-10.0.0_r47`), исходник и сборка |
| `firmware/` | `flash_los.ps1` и заметки: разблокировка, TWRP, Magisk, прошивка LineageOS; Magisk-модули `zerobrand` (свойства системы), `zerosettings` (правка «О телефоне»), `zeroupdater` (название в обновляторе) |

## Сборка HomeWatch

Домашний экран — не самописное приложение, а AOSP Launcher3 из
`android-10.0.0_r47`, собранный без Gradle. Нужны JDK 21, `android.jar`,
`aapt2`, `d8`, `apksigner`, python; сборка идёт в ASCII-путь (`C:\work`),
кириллица ломает `aapt2`. Подробности — в `home/README.md`.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "home\fetch-q-deps.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "home\fetch-q-androidx.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "home\fetch-q-deps2.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "home\build-l3q.ps1"
```

Ставится обратимым Magisk-модулем (`home\make_module.py`), который подменяет
штатный `TrebuchetQuickStep`.

## Рут

Самому лаунчеру root не нужен. Root (Magisk) нужен только установщику: модуль
`homewatch` кладёт APK в `system/product/priv-app/TrebuchetQuickStep`. Выключили
модуль в Magisk — вернулся штатный лаунчер.

## Как прошивали

Полный порядок — в `firmware/README.md`. Кратко: разблокировка загрузчика →
TWRP с datafix → LineageOS 17.1 через `flash_los.ps1` → Magisk 31.0 →
HomeWatch.

Не трогаем: `preloader`, `lk`, `seccfg`, раздел `recovery` (TWRP остаётся
рабочим).
