# ZerOS Mobile — Wileyfox Swift 2 (marmite)

Первый целевой аппарат ZerOS Mobile. Здесь только наш код и скрипты; образы
прошивок и бэкапы в репозиторий не входят — готовая прошивка лежит в
**Releases**. Текущая версия — **ZerOS I 1.0** (Android 10, API 29).

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

## Состояние

- Домашний экран — **штатный лаунчер прошивки**: `com.android.launcher3`,
  activity `.lineage.LineageLauncher`, APK в
  `/system/product/priv-app/TrebuchetQuickStep/`. Своего лаунчера в ZerOS I
  пока нет.
- Активны три Magisk-модуля: `zerobrand` (свойства системы), `zerosettings`
  («О телефоне»), `zeroupdater` (название в обновляторе).
- Свойства: в «О телефоне» строка «Версия ZerOS I» показывает `1.0` (значение
  из `ro.lineage.version`). Подробности — в `firmware/zerobrand/README.md`.
- Своя загрузочная анимация — в `zerobrand`: чёрный фон и надпись «ZerOS I»
  акцентным цветом. Иконок, звуков и своих экранов пока нет: это следующие
  шаги.

## Что в папке

`firmware/` — `flash_los.ps1` и заметки: разблокировка, TWRP, Magisk, прошивка
LineageOS; Magisk-модули `zerobrand`, `zerosettings`, `zeroupdater`.

`home/` — заметка о домашнем экране: эксперименты с лаунчерами (HomeWatch,
ZerHome) удалены, их исходники остались в истории git.

## Рут

Root (Magisk) нужен только для установки модулей: они подкладывают файлы в
`/system/product` через overlay Magisk. Выключили модуль и перезагрузились —
вернулось стоковое значение. Лаунчеру root не нужен.

## Как прошивали

Полный порядок — в `firmware/README.md`. Кратко: разблокировка загрузчика →
TWRP с datafix → LineageOS 17.1 через `flash_los.ps1` → Magisk 31.0 → модули
`zerobrand`, `zerosettings`, `zeroupdater`.

Не трогаем: `preloader`, `lk`, `seccfg`, раздел `recovery` (TWRP остаётся
рабочим).
