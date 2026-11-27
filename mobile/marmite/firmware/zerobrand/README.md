# zerobrand — брендинг ZerOS

Magisk-модуль брендинга: подменяет LineageOS-строки в свойствах системы,
ставит свою загрузочную анимацию, звуки интерфейса и логотип ZerOS в
LineageParts. Файлы `build.prop` не меняются, `/system` и `/vendor` не
перезаписываются — Magisk накладывает их оверлеем. Выключили модуль и
перезагрузились — вернулся LineageOS.

## Что подменяется

### Свойства системы (`system.prop`)

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

### Загрузочная анимация

`bootanimation.zip` упаковывается в `system/media/bootanimation.zip`.
Собирается генератором:

```powershell
powershell -File bootanimation\make_bootanimation.ps1
```

Чёрный фон, надпись «ZerOS I» цветом `#F2A6A0` с подчёркиванием, 720×1280,
30 fps. Две части: `part0` — появление (24 кадра, играет один раз), `part1` —
мягкая пульсация (90 кадров, крутится, пока идёт загрузка). Другой экран —
параметрами `-Width 1080 -Height 2160`. Сам zip — артефакт сборки, в git не
хранится.

Важно: кадры в `bootanimation.zip` должны быть **stored** (без сжатия) и в
архиве нужны записи каталогов `part0/`, `part1/` — иначе Android показывает
чёрный экран. Поэтому zip собирается через `python` (`zipfile.ZIP_STORED`):
PowerShell 5.1 (.NET Framework) с `CompressionLevel.NoCompression` всё равно
жмёт кадры deflate.

### Звуки интерфейса

`sounds/ui/*.ogg` → `/system/product/media/audio/ui/`. Заменяются тики,
клавиши, камера, запись видео, блокировка/разблокировка, зарядка, низкий
заряд, док:

```
ChargingStarted  Dock  Effect_Tick  KeypressDelete  KeypressInvalid
KeypressReturn   KeypressSpacebar  KeypressStandard  Lock  LowBattery
Undock  Unlock  VideoRecord  VideoStop  camera_click  camera_focus
```

Звуки короткие и мягкие (18–370 мс), 44,1 кГц, OGG Vorbis. Генератор —
`sounds/make_sounds.py` (numpy + soundfile), кладёт файлы в `sounds/ui`;
готовые `.ogg` лежат в git, чтобы сборка модуля не требовала Python-пакетов.

### Логотип в LineageParts

Статический RRO-оверлей `logo/ZerOSLogo.apk` подменяет
`drawable/logo_lineage` в `org.lineageos.lineageparts` — вместо логотипа
LineageOS в пасхалке («О телефоне» → «Версия ZerOS I», тап по версии)
рисуется wordmark «ZerOS I» с подчёркиванием. Оригинальный `PlatLogoActivity`
рисует логотип белым силуэтом, поэтому PNG — белый с прозрачным фоном,
1024×512.

Оверлей статический: он обязан лежать в системном разделе
(`/system/product/overlay`), тогда включается сам. Просто поставить APK через
`adb install` нельзя — PackageManager отвечает
`Overlay org.zeroc.logo is static but not pre-installed`. Модуль и кладёт его
туда. Подпись — платформенный ключ, как у LineageParts.

Сборка (нужны apktool, zipalign, apksigner и ключи платформы; путь только
ASCII — apksigner не открывает кириллицу):

```powershell
powershell -File logo\build_overlay.ps1
# по умолчанию ключи: C:\work\settings_apk\keys, инструменты: APK Editor Studio
# свои пути: -KeysDir D:\keys -ToolsDir D:\tools
```

## Сборка и установка

```powershell
powershell -File bootanimation\make_bootanimation.ps1
powershell -File logo\build_overlay.ps1
python make_zip.py
adb push dist\zerobrand-1.0.zip /data/local/tmp/zerobrand.zip
adb shell su -c "magisk --install-module /data/local/tmp/zerobrand.zip"
adb reboot
```

Проверка после загрузки:

```powershell
adb shell getprop ro.build.display.id
adb shell getprop ro.lineage.version
adb shell ls -l /system/product/media/audio/ui/Effect_Tick.ogg
adb shell cmd overlay list | findstr zeroc       # [x] org.zeroc.logo
```

## Откат

```powershell
adb shell su -c "magisk --remove-module zerobrand"
adb reboot
```

Или выключить модуль в приложении Magisk. Вместе с модулем уходят свойства,
анимация, звуки и логотип.

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
- Оверлей статический и привязан к `org.lineageos.lineageparts`: после
  обновления LineageParts имя ресурса может измениться — тогда логотип просто
  перестанет подменяться, остальное продолжит работать.
- Splash-экран загрузчика (`раздел splash`, логотип Wileyfox) модуль **не
  трогает**.
