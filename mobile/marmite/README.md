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
- Брендинг **записан прямо в `/system`** (без Magisk): в конец `build.prop`
  добавлен блок свойств ZerOS, заменены загрузочная анимация, `Settings.apk`
  и `Updater.apk`, добавлены статические RRO темы и логотипа. Magisk-модулей
  брендинга на телефоне больше нет; оригиналы файлов — в `/data/zerobak/`.
- Свойства: в «О телефоне» строка «Версия ZerOS I» показывает `1.0` (значение
  из `ro.lineage.version`). Подробности — в `firmware/zerobrand/README.md`.
- Брендинг: своя загрузочная анимация (клип «ZeroS» с плывущими линиями,
  чёрный фон), тема по умолчанию (акцент pink, шрифт Rubik, форма иконок
  roundedrect) и wordmark «ZerOS I» в пасхалке LineageParts. Обои и звуки —
  родные Lineage, splash-экран загрузчика (логотип Wileyfox) не тронут. Своих
  иконок и домашнего экрана пока нет.

## Что в папке

`firmware/` — `flash_los.ps1` и заметки: разблокировка, TWRP, Magisk, прошивка
LineageOS; сборка брендинга (`zerobrand`, `zerosettings`, `zeroupdater`,
`zerocabout` — приложение «О телефоне»), `make_full_zip.ps1` — полный zip
системы для TWRP и `make_patch_zip.ps1` — патч-zip только с изменёнными
файлами.

`home/` — заметка о домашнем экране: эксперименты с лаунчерами (HomeWatch,
ZerHome) удалены, их исходники остались в истории git.

## Рут

Root (Magisk) больше не нужен для брендинга: файлы лежат в `/system` и
работают без него. Модули `zerobrand`, `zerosettings`, `zeroupdater` не
установлены — их папки в `firmware/` остались сборщиками файлов. Лаунчеру root
не нужен.

## Как прошивали

Полный порядок — в `firmware/README.md`. Кратко: разблокировка загрузчика →
TWRP с datafix → LineageOS 17.1 через `flash_los.ps1` → Magisk 31.0 →
брендинг: полный zip системы в TWRP (или патч-zip поверх LineageOS).

Не трогаем: `preloader`, `lk`, `seccfg`, раздел `recovery` (TWRP остаётся
рабочим).
