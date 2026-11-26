# zerobrand — брендинг ZerOS в build.prop

Маленький Magisk-модуль: при загрузке переопределяет LineageOS-строки в
свойствах системы на ZerOS. Файлы `/system/build.prop`, `/vendor/build.prop` и
`/vendor/odm/etc/build.prop` не меняются — только значения, которые видит
`getprop` и приложения. Выключили модуль и перезагрузились — вернулся LineageOS.

## Что подменяется

| Свойство | Значение |
|---|---|
| `ro.product.name` | `Zeros_marmite` |
| `ro.product.system.name` | `Zeros_marmite` |
| `ro.product.vendor.name` | `Zeros_marmite` |
| `ro.product.odm.name` | `Zeros_marmite` |
| `ro.build.display.id` | `Zeros_marmite 1.0 QzeroDcszerosone eng.wth.2026.2026 test-keys` |
| `ro.build.flavor` | `Zeros_marmite` |
| `ro.lineage.device` | `marmite` |
| `ro.lineage.version` | `1.0` |
| `ro.lineage.display.version` | `1.0` |
| `ro.lineage.releasetype` | `OFFICIAL` |
| `ro.lineage.build.version` | `1.0` |
| `ro.lineage.build.version.plat.sdk` | `1` |
| `ro.lineage.build.version.plat.rev` | `0` |
| `ro.lineagelegal.url` | `no` |

Исходные значения (для возврата): `lineage_marmite`, `17.1-20210703-UNOFFICIAL-marmite`,
`UNOFFICIAL`, `17.1`, sdk `9`.

Версия здесь — **ZerOS I 1.0**: то же число, что в теге `I-1.0`. Схема
версий — в `../../../docs/versions.md`.

## Сборка и установка

```powershell
python make_zip.py
adb push dist\zerobrand-1.0.zip /data/local/tmp/zerobrand.zip
adb shell su -c "magisk --install-module /data/local/tmp/zerobrand.zip"
adb reboot
```

Проверка:

```powershell
adb shell getprop ro.build.display.id
adb shell getprop ro.lineage.version
```

## Загрузочная анимация

`bootanimation.zip` лежит в корне модуля и упаковывается в
`system/media/bootanimation.zip` — Magisk подменяет им системную анимацию.
Собирается генератором:

```powershell
powershell -File bootanimation\make_bootanimation.ps1
```

Чёрный фон, надпись «ZerOS I» цветом `#F2A6A0` с подчёркиванием, 720×1280,
30 fps (24 кадра появления + 36 держать кадр). Другой экран — параметрами
`-Width 1080 -Height 2160`. Сам zip — артефакт сборки, в git не хранится.

## Откат

```powershell
adb shell su -c "magisk --remove-module zerobrand"
adb reboot
```

Или выключить модуль в приложении Magisk.

## Оговорки

- `ro.build.description` и `ro.build.fingerprint` не трогаются: в этой сборке
  они от стока Wileyfox (`marmite-user 8.1.0 ...`, `Wileyfox/Swift2/...`), строк
  Lineage в них нет.
- `ro.lineage.releasetype=OFFICIAL` и версия `1.0` заставят
  Lineage Updater считать сборку официальной: проверка обновлений и установка
  OTA в таком виде работать не будут. За обновления отвечает `zeroupdater`
  (см. `../zeroupdater/`).
- `ro.lineage.build.version.plat.sdk=1` — версия API Lineage SDK. Приложения,
  которые по ней выбирают доступные функции (LineageParts и т.п.), могут
  спрятать часть настроек.
- `ro.lineagelegal.url=no` — ссылка на юридическую страницу станет нерабочей.
- Свойства применяются на этапе `post-fs-data`, до старта zygote, поэтому
  приложения видят уже подменённые значения. Ранний init и ядро видят исходные.
