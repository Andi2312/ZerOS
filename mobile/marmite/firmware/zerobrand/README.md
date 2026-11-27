# zerobrand — брендинг ZerOS

Magisk-модуль брендинга: подменяет LineageOS-строки в свойствах системы,
ставит свою загрузочную анимацию, обои и тему по умолчанию и логотип ZerOS в
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

### Обои по умолчанию

`wallpaper/wallpaper.jpg` — «Орбита», 720×1280, палитра ZerOS.
`post-fs-data.sh` при первом запуске после установки кладёт картинку в
`/data/system/users/0/wallpaper` и пишет рядом `wallpaper_info.xml` (id
сохраняется, размеры и цвета — от новой картинки). Дальше скрипт молчит:
маркер `/data/adb/zerobrand/wallpaper.applied`, поэтому выбранные потом обои
не перебиваются.

Почему 720×1280: `ImageWallpaperRenderer` берёт размер полотна из самого файла
и рисует его 1:1 (в логе загрузки — `surface size=Rect(0, 0 - 720, 1280)`),
так что картинка шире экрана была бы обрезана по краям, а выше — не заполнила
бы экран. Заводские обои на marmite тоже 720×1280, хотя в их
`wallpaper_info.xml` ширина 1440: это «желаемая» ширина для прокрутки, на
отрисовку она не влияет.

Перед заменой старые обои и их `wallpaper_info.xml` сохраняются в
`/data/adb/zerobrand/`. `uninstall.sh` вернёт их при удалении модуля, но
только если обои с тех пор не меняли (сравнивает файл с нашим); если меняли —
ничего не трогает.

Генератор — `wallpaper/make_wallpapers.py` (numpy + PIL), три варианта:
`1-horizon` (свечение снизу и линия горизонта), `2-orbit` (концентрические
кольца и точка, выбран), `3-eclipse` (тёмный диск с акцентной кромкой).
Готовый `wallpaper/wallpaper.jpg` лежит в git, чтобы сборка модуля не требовала
Python-пакетов. Чтобы поставить другой вариант, нужно заменить
`wallpaper/wallpaper.jpg`, удалить на телефоне маркер
`/data/adb/zerobrand/wallpaper.applied` (бэкап прежних обоев при этом
сохранится) и перезагрузиться — скрипт отработает заново при загрузке.

### Тема по умолчанию

Встроенную тему по умолчанию в ROM модуль переписать не может, поэтому тема
выставляется один раз — при первой загрузке с модулем, — теми же действиями,
что делает «Стили»: в каждой категории остаётся ровно один оверлей, выбор
пишется в `settings secure theme_customization_overlay_packages`.

| Категория | Оверлей |
|---|---|
| акцент | `org.lineageos.overlay.accent.pink` |
| шрифт | `org.lineageos.overlay.font.rubik` |
| форма иконок | `com.android.theme.icon.roundedrect` |

Делает это `service.sh` (в `post-fs-data.sh` нельзя: до подъёма
`system_server` нет ни `settings`, ни `cmd overlay`). Скрипт ждёт
`sys.boot_completed`, гасит чужие акценты/шрифты/формы, включает свои и
проверяет результат чтением обратно; только после этого ставит маркер
`/data/adb/zerobrand/theme.applied`. Дальше он молчит — сменили тему в
«Стилях», она останется.

`cmd overlay enable-exclusive` здесь не используется: без `--category` он на
marmite гасит оверлеи и соседних категорий (проверено), поэтому чужие оверлеи
снимаются по именам, а свои включаются обычным `enable`.

Прежний выбор темы сохраняется в `/data/adb/zerobrand/theme.orig.json`, и
`uninstall.sh` возвращает его — но только если тему с тех пор не меняли
(сравнивает текущее значение с тем, что записал модуль, без
`_applied_timestamp`).

## Сборка и установка

```powershell
powershell -File bootanimation\make_bootanimation_from_video.ps1
powershell -File logo\build_overlay.ps1
python make_zip.py
adb push dist\zerobrand-1.0.zip /data/local/tmp/zerobrand.zip
adb shell su -c "magisk --install-module /data/local/tmp/zerobrand.zip"
adb reboot
```

Обои уже лежат готовыми в `wallpaper\wallpaper.jpg`; пересобрать варианты —
`python wallpaper\make_wallpapers.py`.

Проверка после загрузки:

```powershell
adb shell getprop ro.build.display.id
adb shell getprop ro.lineage.version
adb shell cmd overlay list | findstr zeroc       # [x] org.zeroc.logo
adb shell settings get system notification_sound # заводской Argon
adb shell settings get secure theme_customization_overlay_packages  # pink + rubik + roundedrect
adb shell su -c "ls -l /data/system/users/0/wallpaper"   # 130977 байт
adb shell logcat -d | findstr ImageWallpaperRenderer     # surface size=Rect(0, 0 - 720, 1280)
```

## Откат

```powershell
adb shell su -c "magisk --remove-module zerobrand"
adb reboot
```

Или выключить модуль в приложении Magisk. Вместе с модулем уходят свойства,
анимация и логотип; `uninstall.sh` возвращает прежние обои и тему, если с тех
пор их не меняли. Звуки остаются заводскими.

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
- `wallpaper_info.xml` — служебный файл WallpaperManagerService: при загрузке
  система может переписать его (ширину приводит к «желаемой» 1440). На
  отрисовку это не влияет: размер полотна берётся из самого `wallpaper.jpg`.
- Тема по умолчанию выставляется один раз, поэтому «сброс к заводской» в
  «Стилях» вернёт стоковую тему, а не ZerOS: чтобы вернуть нашу, надо удалить
  маркер `/data/adb/zerobrand/theme.applied` и перезагрузиться. Оверлеи темы —
  системные (`/product/overlay`), модуль их только включает, сами APK не
  подменяются.
