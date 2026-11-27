# zerobrand — брендинг ZerOS

Magisk-модуль брендинга: подменяет LineageOS-строки в свойствах системы,
ставит свою загрузочную анимацию, тему по умолчанию и логотип ZerOS в
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

Обои модуль не трогает — остаются родные Lineage. Звуки тоже заводские.
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
`/system/product/overlay` (туда их кладёт модуль) и имеют `priority="0"`:
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

## Сборка и установка

```powershell
powershell -File bootanimation\make_bootanimation_from_video.ps1
powershell -File theme\build_theme.ps1
powershell -File logo\build_overlay.ps1
python make_zip.py
adb push dist\zerobrand-1.0.zip /data/local/tmp/zerobrand.zip
adb shell su -c "magisk --install-module /data/local/tmp/zerobrand.zip"
adb reboot
```

Обои и звуки модуль не трогает — они родные Lineage.

Проверка после загрузки:

```powershell
adb shell getprop ro.build.display.id
adb shell getprop ro.lineage.version
adb shell cmd overlay list | findstr zeroc       # [x] org.zeroc.logo (тема статическая и в списке не видна)
adb shell cmd overlay dump org.zeroc.theme.accent # mState: STATE_ENABLED_STATIC
adb shell su -c "idmap2 dump --idmap-path /data/resource-cache/product@overlay@ZerOSAccent.apk@idmap"  # color/accent_device_default_*
adb shell settings get system notification_sound # заводской Argon
adb shell settings get secure theme_customization_overlay_packages  # null — тема не выбрана, она вшита
```

Если в «Стилях» выбрать другой акцент, он ляжет поверх запечённого (runtime-
оверлеи идут после статических), а «Сброс к заводской» вернёт тему ZerOS.

## Откат

```powershell
adb shell su -c "magisk --remove-module zerobrand"
adb reboot
```

Или выключить модуль в приложении Magisk. Вместе с модулем уходят свойства,
анимация, тема и логотип: статических оверлеев в `/system/product/overlay`
больше нет, система возвращается к родной теме Lineage. Обои и звуки остаются
родными.

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
- Тема ZerOS вшита статическими оверлеями, поэтому «сброс к заводской» в
  «Стилях» возвращает именно её, а не стоковую тему Lineage. Выключить её
  можно только отключением модуля. Оверлеи темы лежат в `/system/product/overlay`
  (`ZerOSAccent.apk`, `ZerOSFont.apk`, `ZerOSShape.apk`) и привязаны к
  ресурсам `android` по именам: если в новой прошивке ресурс переименуют,
  соответствующий оверлей просто перестанет подменять значение.
- Обои (`/data/system/users/0/wallpaper`) модуль не трогает: на телефоне
  лежат родные Lineage. Их служебный `wallpaper_info.xml` при загрузке может
  переписываться самой системой — это нормально.
