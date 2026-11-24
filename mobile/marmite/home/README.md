# HomeWatch — AOSP Launcher3 для Wileyfox Swift 2

Домашний экран ZerOS на Swift 2 — это **стоковый AOSP Launcher3**, флейвор
`Launcher3QuickStep`, собранный из тега **android-10.0.0_r47** (Android 10,
Apache-2.0). Свой лаунчер не пишем: берём апстрим, правим только то, что мешает
ему работать на LineageOS 17.1, и добавляем плитки ZerOS.

- Пакет: `com.android.launcher3`, activity `com.android.launcher3/.Launcher`
- Версия APK: `0.1.0-r47` (`ZerOS I 0.1.0`, в основе апстрим `r47`)
- Подпись: `zeroshome.jks` (генерируется при первой сборке)
- Установка: обратимым Magisk-модулем `homewatch`, который подменяет
  `system/product/priv-app/TrebuchetQuickStep/TrebuchetQuickStep.apk`

## Плитки ZerOS

Рабочий стол — сетка **4 × 6**. Каждый ярлык на рабочем столе рисуется плиткой
в стиле Windows Phone: плоский прямоугольник цвета марсала `#4A0A18`, иконка
в левом верхнем углу, подпись — в левом нижнем. Цвет нажатия `#5C0D1E`,
подпись `#F2A6A0` (см. `../../docs/palette.md`).

Размер плитки меняется долгим нажатием → **«Размер плитки»**:

| Пункт | Занятых ячеек |
|---|---|
| 1 × 1 | 1 × 1 |
| 2 × 1 | 2 × 1 |
| 2 × 2 | 2 × 2 |
| 4 × 2 | 4 × 2 |

Размер хранится в `favorites.spanX/spanY` и переживает перезапуск лаунчера.
Если в текущем месте плитка нового размера не помещается, она переезжает в
ближайшее свободное место; если места нет совсем — показывается сообщение.
Папки и hotseat остаются стоковыми, размер там всегда 1 × 1.

## Что в папке

| Путь | Что это |
|---|---|
| `launcher3-q/` | исходник AOSP Launcher3, тег `android-10.0.0_r47`, с нашими правками |
| `build-l3q.ps1` | сборка APK без Gradle: protoc → aapt2 → javac → d8 → apksigner |
| `fetch-q-deps.ps1` | скачивает `SystemUI/shared`, `SystemUI/plugin`, `SystemUI/plugin_core` того же тега |
| `fetch-q-androidx.ps1` | скачивает androidx 1.1.0 / 2.1.0 (AAR → `classes.jar` + `res`) |
| `fetch-q-deps2.ps1` | собирает `deps-q/`: sysui-патчи, AIDL, proto статистики, system-SDK API 29 |
| `make_module.py` | упаковывает `dist/Launcher3QuickStep-0.1.0.apk` в Magisk-модуль |
| `deps/` | `protoc.exe`, рантайм protobuf-javanano, `add_dex.py` |
| `deps-q/` | шимы (`stats-src`, `compat-src`, `proto`, `gen-aidl`), манифест, sysui-исходники |

`deps-q/libs`, `deps-q/res` и большие jar'ы (`android-all-10.jar`,
`android-system-29.jar`) в репозитории не лежат — их создают скрипты.

## Требования

| Что | Где ожидается |
|---|---|
| JDK 21 | `C:\Program Files\Java\jdk-21.0.12.1` или `JAVA_HOME` |
| `android.jar` (API 33+) для aapt2 | `G:\Android-sdk\platforms\android-36.1\android.jar` |
| `aapt2.exe` | ищется в кэше Gradle |
| `d8.jar` | `G:\Android-sdk\build-tools\*\lib\d8.jar` (если нет `%TEMP%\opencode\android\r8.jar`) |
| `zipalign.exe`, `apksigner.jar` | `C:\Program Files (x86)\APK Editor Studio\tools` |
| `protoc.exe` | `deps\tools\protoc.exe` (в репозитории) |
| python 3, `tar` | в `PATH` |
| `android-all-10.jar` (125 МБ) | `deps-q\android-all-10.jar`, скачивается вручную |

`android-all-10.jar` — это framework API 29, скомпилированный для JVM
(Robolectric): обычный `android.jar` не содержит `com.android.internal.*`,
которые нужны `SystemUISharedLib`. Только для `javac`, в APK не попадает:

```powershell
Invoke-WebRequest -UseBasicParsing `
  -OutFile "deps-q\android-all-10.jar" `
  "https://repo1.maven.org/maven2/org/robolectric/android-all/10-robolectric-5803371/android-all-10-robolectric-5803371.jar"
```

## Сборка

Сборка идёт в `C:\work\aospl3q` (ASCII-путь): `aapt2` и `aidl.exe` ломаются на
кириллице в пути. Каталог пересоздаётся на каждом запуске.

```powershell
cd mobile\marmite\home

# один раз: зависимости, которых нет в исходнике Launcher3
powershell -NoProfile -ExecutionPolicy Bypass -File .\fetch-q-deps.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\fetch-q-androidx.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\fetch-q-deps2.ps1

# сборка
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-l3q.ps1
```

Результат — `dist\Launcher3QuickStep-0.1.0.apk` (`applicationId
com.android.launcher3`, minSdk 26, targetSdk 29). Другой рабочий каталог:
`-Work D:\build\l3q`.

## Установка

Штатный путь — Magisk-модуль. На части устройств
`magisk --install-module` теряет файлы модуля, поэтому надёжнее скопировать
каталог модуля вручную:

```powershell
python .\make_module.py
adb push .\dist\HomeWatch-magisk.zip /data/local/tmp/homewatch.zip
adb shell su -c "magisk --install-module /data/local/tmp/homewatch.zip"
adb reboot
```

Либо: распаковать `HomeWatch-magisk.zip` в `/data/adb/modules/homewatch/`
и перезагрузиться. После перезагрузки домашний экран — HomeWatch, рекасты
открываются свайпом вверх (или кнопкой «Обзор»). Выключить модуль в Magisk —
вернётся штатный Trebuchet.

Для проверки правок без перезагрузки можно заменить APK в
`/data/adb/modules/homewatch/system/product/priv-app/TrebuchetQuickStep/` и
перезапустить процесс:

```powershell
adb shell su -c "am force-stop com.android.launcher3"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

Если PackageManager показывает старую версию пакета, удалите кэш и
перезагрузитесь: `adb shell su -c "rm -rf /data/system/package_cache/*"`.

Проверка:

```powershell
adb shell dumpsys package com.android.launcher3 | Select-String versionName
adb shell cmd package resolve-activity --brief com.android.launcher3/.Launcher
```

## Отклонения от апстрима

Всё остальное — код AOSP без изменений. Правки под LOS 17.1 и плитки ZerOS:

1. `res/values/config.xml`, `quickstep/res/values/strings.xml` —
   `derived_app_name` = `HomeWatch`.
2. `PredictionAppTracker.createPredictor` — в прошивке нет сервиса
   `app_prediction`, поэтому предиктор возвращает `null` вместо падения с
   `NullPointerException` в `UiThreadHelper`.
3. `LauncherRecentsView.onTaskLaunchAnimationUpdate` / `redrawLiveTile` —
   проверка `mRecentsAnimationWrapper == null`: при кнопочной навигации
   анимация запуска задачи не создаётся.
4. `deps-q/sysui/.../SystemGestureExclusionListenerCompat.java` — добавлена
   3-аргументная форма `onSystemGestureExclusionChanged`: её вызывает framework
   LineageOS 17.1, иначе `AbstractMethodError` в binder-потоке.
5. `deps-q/sysui/.../PluginManagerImpl.java` — `Thread.get/setUncaughtException
   PreHandler` (скрытый API libcore) вызывается через `compat-src`-шим, иначе
   код не компилируется обычным JDK.
6. `deps-q/stats-src/.../Launcher.java` — protoc 3.1 не создаёт внешние enum'ы
   при `java_multiple_files`, поэтому интерфейс объявлен вручную.
7. Плитки:
   - `src/.../ZerTileView.java` — отрисовка плитки; подключён корнем
     `res/layout/app_icon.xml` вместо `DoubleShadowBubbleTextView` (только
     `container == CONTAINER_DESKTOP`, папки и hotseat остаются стоковыми).
   - `src/.../ZerTileSizer.java` — смена размера: правит `LayoutParams`,
     occupancy-карту, `WorkspaceItemInfo` и строку в `favorites`.
   - `src/.../popup/ZerTileShortcut.java` — пункт «Размер плитки» в меню
     долгого нажатия (зарегистрирован в `SystemShortcutFactory`).
   - `res/xml/device_profiles.xml` — профиль `4_by_6` / `ZerOS Tiles`
     (336 × 568 dp, совпадает с `getCurrentSizeRange` на marmite).
   - `InvariantDeviceProfile.initGrid` — сохранённое имя сетки, отличное от
     `4_by_6`, игнорируется, чтобы стоковый `4_by_4` не блокировал плитки.
   - `CellLayout.isRegionFreeForView` / `findNearestFreeRegion` — поиск места
     при изменении размера.
   - `model/LoaderTask` — читает `spanX/spanY` для ярлыков рабочего стола;
     hotseat и папки всегда 1 × 1 (`Workspace.onDrop` сбрасывает размер при
     переносе плитки в hotseat).
   - строки `tile_size`, `tile_no_room`, `tile_size_options` (RU/EN) и
     иконка `zer_ic_tile_size`.

Правки 4–6 вносятся/проверяются скриптом `fetch-q-deps2.ps1` и лежат в
репозитории уже применёнными.

## Лицензия

`launcher3-q/` — AOSP Launcher3, Apache License 2.0 (см. `NOTICE` и
`MODULE_LICENSE_APACHE2` в папке). Скрипты сборки и плитки — код ZerOS.
