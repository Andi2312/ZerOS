# HomeWatch — AOSP Launcher3 для Wileyfox Swift 2

Домашний экран ZerOS на Swift 2 — это **стоковый AOSP Launcher3**, флейвор
`Launcher3QuickStep`, собранный из тега **android-10.0.0_r47** (Android 10,
Apache-2.0). Свой лаунчер не пишем: берём апстрим, правим только то, что мешает
ему работать на LineageOS 17.1.

- Пакет: `com.android.launcher3`, activity `com.android.launcher3/.Launcher`
- Версия APK: `10.0.0-r47` (апстримная, видно, из какого тега собрано)
- Подпись: `zeroshome.jks` (генерируется при первой сборке)
- Установка: обратимым Magisk-модулем `homewatch`, который подменяет
  `system/product/priv-app/TrebuchetQuickStep/TrebuchetQuickStep.apk`

## Что в папке

| Путь | Что это |
|---|---|
| `launcher3-q/` | исходник AOSP Launcher3, тег `android-10.0.0_r47`, с нашими правками |
| `build-l3q.ps1` | сборка APK без Gradle: protoc → aapt2 → javac → d8 → apksigner |
| `fetch-q-deps.ps1` | скачивает `SystemUI/shared`, `SystemUI/plugin`, `SystemUI/plugin_core` того же тега |
| `fetch-q-androidx.ps1` | скачивает androidx 1.1.0 / 2.1.0 (AAR → `classes.jar` + `res`) |
| `fetch-q-deps2.ps1` | собирает `deps-q/`: sysui-патчи, AIDL, proto статистики, system-SDK API 29 |
| `make_module.py` | упаковывает `dist/Launcher3QuickStep-r47.apk` в Magisk-модуль |
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
| `r8.jar` | `%TEMP%\opencode\android\r8.jar` |
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

Результат — `dist\Launcher3QuickStep-r47.apk` (`applicationId
com.android.launcher3`, minSdk 26, targetSdk 29). Другой рабочий каталог:
`-Work D:\build\l3q`.

## Установка

```powershell
python .\make_module.py
adb push .\dist\HomeWatch-magisk.zip /data/local/tmp/homewatch.zip
adb shell su -c "magisk --install-module /data/local/tmp/homewatch.zip"
adb reboot
```

После перезагрузки домашний экран — HomeWatch, рекасты открываются свайпом
вверх (или кнопкой «Обзор»). Выключить модуль в Magisk — вернётся штатный
Trebuchet.

Если PackageManager показывает старую версию пакета, удалите кэш и
перезагрузитесь: `adb shell su -c "rm -rf /data/system/package_cache/*"`.

Проверка:

```powershell
adb shell dumpsys package com.android.launcher3 | Select-String versionName
adb shell cmd package resolve-activity --brief com.android.launcher3/.Launcher
```

## Отклонения от апстрима

Всё остальное — код AOSP без изменений. Правки только под LOS 17.1:

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

Правки 4–6 вносятся/проверяются скриптом `fetch-q-deps2.ps1` и лежат в
репозитории уже применёнными.

## Лицензия

`launcher3-q/` — AOSP Launcher3, Apache License 2.0 (см. `NOTICE` и
`MODULE_LICENSE_APACHE2` в папке). Скрипты сборки — код ZerOS.
