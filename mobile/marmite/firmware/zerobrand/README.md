# zerobrand — брендинг ZerOS

Брендинг ZerOS: строки LineageOS в свойствах системы, своя загрузочная
анимация, тема по умолчанию и логотип ZerOS в LineageParts.

С версии 1.0 брендинг **записан прямо в `/system`** — файлами, без Magisk.
Модуль `zerobrand` больше не устанавливается; папка остаётся генератором
файлов и заготовкой для будущей упаковки прошивки (см. «Установка прямо в
`/system`»). `/vendor` — squashfs только для чтения, поэтому vendor-строки
не меняются (см. оговорки).

## Что меняется

### Свойства системы (`system.prop`)

`system.prop` — источник блока. При установке прямо в `/system` эти строки
дописаны в конец `/system/build.prop` (одноимённые значения ниже перекрывают
верхние), исходный файл сохранён в `/data/zerobak/build.prop` и в
`C:\work\sys\build.prop.orig`.

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

Строки `ro.product.vendor.name` и `ro.product.odm.name` в списке есть, но на
marmite не действуют: `/vendor` — squashfs только для чтения, а эти свойства
читаются из vendor/odm `build.prop`. В системе они остаются `lineage_marmite`
(проверено `getprop`); остальные значения применяются.

Исходные значения (для возврата): `lineage_marmite`, `17.1-20210703-UNOFFICIAL-marmite`,
`UNOFFICIAL`, `17.1`, sdk `9`.

Версия здесь — **ZerOS I 1.0**: то же число, что в теге `I-1.0`. Схема
версий — в `../../../docs/versions.md`.

### Загрузочная анимация

`bootanimation.zip` упаковывается в `system/media/bootanimation.zip`.
Анимация — клип `bootanimation/zeros.mp4` (720×1280, 30 fps, 4,5 с,
135 кадров): чёрный фон, wordmark «ZeroS» и медленно плывущие белые линии.
Собирается без Python — ffmpeg вытаскивает кадры, 7-Zip пакует:

```powershell
powershell -File bootanimation\make_bootanimation_from_video.ps1
```

Клип не является бесшовной петлёй, поэтому скрипт делает **пинг-понг**: 135
кадров вперёд + 133 назад = 268 кадров в одной части `part0`, которая крутится
без рывков (`p 0 0 part0`). С флагом `-NoPingPong` — просто цикл как есть,
с видимым стыком. Кадры вписаны ровно в экран (`scale`+`pad`), звук из видео
отбрасывается: bootanimation его не играет.

Прежний вариант со надписью «ZerOS I» рисуется генератором
`bootanimation\make_bootanimation.ps1` (появление + пульсация) — он остался
как запасной. Сам zip — артефакт сборки, в git не хранится; в git лежит
исходное видео.

Важно: кадры в `bootanimation.zip` должны быть **stored** (без сжатия) и в
архиве нужны записи каталогов `part0/`, `part1/` — иначе Android показывает
чёрный экран. Поэтому `.NET`-сжатие не годится (`CompressionLevel.NoCompression`
всё равно жмёт deflate), а 7-Zip с ключом `-mx0` даёт ровно method 0;
`make_bootanimation_from_video.ps1` после сборки это проверяет.

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

### Обои и звуки

Обои установка не трогает — остаются родные Lineage. Звуки тоже заводские.
Свои обои из ZerOS на телефоне убраны, прежние возвращены из бэкапа
(картинка и `wallpaper_info.xml`, id сохранён).

### Тема по умолчанию

Тема ZerOS (рабочее название — «Silly») — это акцент **pink**, шрифт
**Rubik** и **скруглённые иконки**. В сборке она не «выбрана», а вшита: три
статических RRO-оверлея подменяют значения фреймворка по умолчанию, поэтому
тема действует сразу после загрузки и для всех пользователей, а «Стили» не
могут её выключить — «сброс к заводской» возвращает именно её.

| Пакет | Что подменяет | Значение |
|---|---|---|
| `org.zeroc.theme.accent` | `color/accent_device_default_dark`, `..._light` | `#ffe6125e` (pink) |
| `org.zeroc.theme.font` | `string/config_bodyFontFamily`, `...Medium`, `config_headlineFontFamily`, `...Medium` | `rubik`, `rubik-medium` |
| `org.zeroc.theme.shape` | `string/config_icon_mask`, `bool/config_useRoundIcon`, `dimen/config_dialogCornerRadius`, `...bottomDialogCornerRadius` | SVG-путь roundedrect, `false`, 2 / 4 dip |

Значения скопированы из стоковых оверлеев LineageOS
(`org.lineageos.overlay.accent.pink`, `org.lineageos.overlay.font.rubik`,
`com.android.theme.icon.roundedrect`). Своих TTF в оверлее нет: он выбирает
системные семейства `rubik`/`rubik-medium` из `/system/etc/fonts.xml`.

Оверлеи помечены `android:isStatic="true"`, лежат в
`/system/product/overlay` и имеют `priority="0"`:
OverlayManager включает их сам при каждой загрузке, а выбранные в «Стилях»
акцент/шрифт/форма (priority 1) по-прежнему главнее — если выбрать другой
акцент, он сработает. Пока ничего не выбрано, действует тема ZerOS.

`cmd overlay enable-exclusive` не используется: без `--category` он на
marmite гасит оверлеи и соседних категорий (проверено).

Исходники — `theme/<категория>/` (`AndroidManifest.xml` + `res/values`),
сборка:

```powershell
powershell -File theme\build_theme.ps1
# по умолчанию ключи: C:\work\settings_apk\keys, инструменты: APK Editor Studio
# свои пути: -KeysDir D:\keys -ToolsDir D:\tools
```

Скрипт собирает три APK, выравнивает, подписывает платформенным ключом и
проверяет подпись. Сами APK — артефакты, в git их нет.

Прежний способ (оставшийся от версии 12 `service.sh`: применить тему один раз
через `settings` и `cmd overlay`) убран. Заодно снят и сам «выбор» в
`theme_customization_overlay_packages`: тема теперь не выбрана, а вшита, так
что «Стили» показывают тему по умолчанию — это и есть ZerOS. Любой выбор в
«Стилях» по-прежнему работает поверх неё.

## Сборка

```powershell
powershell -File bootanimation\make_bootanimation_from_video.ps1
powershell -File theme\build_theme.ps1
powershell -File logo\build_overlay.ps1
```

`make_zip.py` по-прежнему собирает Magisk-модуль `dist\zerobrand-1.0.zip`:
он остался запасным способом установки и генератором файлов, но в 1.0 на
телефон не ставится.

## Установка прямо в `/system`

Брендинг записан файлами в реальный раздел system — без Magisk и без
подмен. `/system` перемонтируется rw (ext4, `mmcblk0p24`), файлы кладутся по
местам, выставляются владелец, права и SELinux-контексты:

| Файл в `/system` | Что |
|---|---|
| `build.prop` | блок свойств ZerOS дописан в конец (исходные строки выше сохранены) |
| `media/bootanimation.zip` | анимация «ZeroS» |
| `product/overlay/ZerOSAccent.apk` | статический RRO: акцент pink |
| `product/overlay/ZerOSFont.apk` | статический RRO: Rubik |
| `product/overlay/ZerOSShape.apk` | статический RRO: скруглённые иконки |
| `product/overlay/ZerOSLogo.apk` | статический RRO: логотип в LineageParts |
| `product/priv-app/Settings/Settings.apk` | «Версия ZerOS I» |
| `priv-app/Updater/Updater.apk` | «ZerOS 1.0» в обновляторе |

Владелец `root:root`; `build.prop` — `600`, остальное — `644`. Контексты:
оверлеи — `u:object_r:vendor_overlay_file:s0`, остальные файлы —
`u:object_r:system_file:s0` (именно так размечены штатные файлы этих
каталогов).

Два способа установки, оба дают одно и то же состояние:

**1. Патч-zip для TWRP** (поверх уже стоящей LineageOS 17.1):

```powershell
.\make_patch_zip.ps1 -BuildProp C:\work\sys\build.prop.new
# dist\ZerOS-I-1.0-marmite-patch.zip, ~40 МБ
adb push dist\ZerOS-I-1.0-marmite-patch.zip /sdcard/
# TWRP → Install → zip → Reboot → System
```

`make_patch_zip.ps1` берёт готовый `build.prop` (стоковый + блок ZerOS) и
собранные артефакты (`bootanimation.zip`, оверлеи, `Settings.apk`,
`Updater.apk`), пишет `md5sums.txt` и упаковывает `update-binary` с правом
`0755`. Скрипт установки сам находит раздел (монтирует `mmcblk0p24` в
`/mnt/zerosys`, ищет каталог с `build.prop` и `product/overlay`), копирует
прежние файлы в `/sdcard/zerobak-patch-<дата>/`, проверяет md5 после записи.

**2. Полный образ system** (с нуля, без базовой LOS — только system):
в релизе лежит `ZerOS-I-1.0-marmite-system.img.7z` (3,5 ГБ → 449 МБ),
внутри — образ раздела:

```powershell
7z x ZerOS-I-1.0-marmite-system.img.7z
fastboot flash system system.img
```

`boot`, `vendor` и `recovery` не входят и не трогаются. Образ снят с
работающего раздела (`dd` при смонтированном ro `/system`), `e2fsck -fn`
чистый, md5 файлов внутри сверены с исходниками.

Ручной путь (adb + root), если нет ни TWRP под рукой, ни желания шить образ:
перемонтировать раздел (`mount -o rw /dev/block/bootdevice/by-name/system
/mnt/sys`), скопировать файлы в `/mnt/sys/system/...`, выставить
`chmod`/`chown`/`chcon`, `sync`, перезагрузиться.

## Проверка после загрузки

```powershell
adb shell getprop ro.build.display.id            # Zeros_marmite 1.0 ...
adb shell getprop ro.lineage.version             # 1.0
adb shell cmd overlay list | findstr zeroc       # [x] org.zeroc.logo (остальные статические и в списке не видны)
adb shell cmd overlay dump org.zeroc.theme.accent # mState: STATE_ENABLED_STATIC, /product/overlay/ZerOSAccent.apk
adb shell su -c "idmap2 dump --idmap-path /data/resource-cache/product@overlay@ZerOSAccent.apk@idmap"  # color/accent_device_default_*
adb shell settings get secure theme_customization_overlay_packages  # null — тема не выбрана, она вшита
```

В интерфейсе: «О телефоне» → «Версия ZerOS I» = `1.0`, обновлятор — «ZerOS
1.0», акцент в переключателях — `#E6125E`. Если в «Стилях» выбрать другой
акцент, он ляжет поверх запечённого (runtime-оверлеи идут после статических),
а «Сброс к заводской» вернёт тему ZerOS.

## Откат

- Патч-zip: перепрошить LineageOS 17.1 тем же zip-ом, что ставили (вернёт
  system), либо вернуть файлы из `/sdcard/zerobak-patch-<дата>/`.
- Полный образ: перепрошить стоковую LineageOS 17.1.
- Если брендинг ставился модулем (старые версии): удалить
  `/data/adb/modules/zerobrand` и перезагрузиться.

Обои и звуки не трогаются — остаются родными Lineage.

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
- Свойства лежат в конце `/system/build.prop`, поэтому их читает уже
  `init` при загрузке: приложения видят значения ZerOS, а не подмену поверх
  стока. Ранний init (до `build.prop`) и ядро видят исходные строки.
- Оверлей статический и привязан к `org.lineageos.lineageparts`: после
  обновления LineageParts имя ресурса может измениться — тогда логотип просто
  перестанет подменяться, остальное продолжит работать.
- Splash-экран загрузчика (`раздел splash`, логотип Wileyfox) **не
  трогается**.
- Тема ZerOS вшита статическими оверлеями, поэтому «сброс к заводской» в
  «Стилях» возвращает именно её, а не стоковую тему Lineage. Убрать её можно
  только заменой/удалением файлов оверлеев в `/system/product/overlay`.
  Оверлеи темы привязаны к
  ресурсам `android` по именам: если в новой прошивке ресурс переименуют,
  соответствующий оверлей просто перестанет подменять значение.
- Обои (`/data/system/users/0/wallpaper`) установка не трогает: на телефоне
  лежат родные Lineage. Их служебный `wallpaper_info.xml` при загрузке может
  переписываться самой системой — это нормально.
