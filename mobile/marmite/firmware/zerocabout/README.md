# zerocabout — приложение «О телефоне»

Отдельное приложение `org.zeroc.about` («О телефоне») со своей иконкой в меню
приложений. С версии 1.0 лежит в системе:
`/system/priv-app/ZerOSAbout/ZerOSAbout.apk`. Штатный экран настроек не
меняется — приложение самостоятельное.

## Что показывает

Настоящий Material Design 2: библиотека
`com.google.android.material:material:1.4.0` (ветка MD2; MD3 начинается с
1.5). Компоненты — `MaterialToolbar` с подзаголовком в `AppBarLayout`,
`TabLayout` с тремя вкладками, `MaterialCardView` с карточками и
`MaterialButton` («Обновления»). Логотипа в шапке нет — только название и
версия; цвета и шрифт заданы темой `Theme.MaterialComponents.NoActionBar`
(розовый `colorPrimary`, тёмная поверхность, Rubik).

| Вкладка | Строки |
|---|---|
| **Система** | версия Android и API, патч безопасности, ядро, номер сборки, дата сборки |
| **Устройство** | модель, кодовое имя, платформа (SoC и число ядер), экран, оперативная память, накопитель |
| **ZerOS** | поколение, версия, тип сборки, Lineage SDK; кнопка «Обновления» и подпись |

Данные не зашиты в текст: приложение читает `android.os.Build`, свойства через
`getprop`, `ActivityManager.MemoryInfo`, `StatFs` и `Display.getRealMetrics()`.
Поэтому после смены версии ZerOS в `build.prop` строки меняются сами.

| Поле | Источник |
|---|---|
| Версия Android | `Build.VERSION.RELEASE`, `Build.VERSION.SDK_INT` |
| Патч безопасности | `Build.VERSION.SECURITY_PATCH` |
| Ядро | `System.getProperty("os.version")` |
| Номер сборки | `getprop ro.build.display.id` |
| Дата сборки | `getprop ro.build.date.utc` (секунды UTC) |
| Модель | `Build.MANUFACTURER` + `Build.MODEL` |
| Кодовое имя | `Build.DEVICE` |
| Платформа | `getprop ro.board.platform` (`msm8937` → Snapdragon 430), число ядер |
| Экран | `Display.getRealMetrics()`: 720 × 1280, 320 dpi |
| Оперативная память | `ActivityManager.MemoryInfo.totalMem` |
| Накопитель | `StatFs` раздела `/data` |
| Поколение | `SDK_INT` − 29 → римская цифра (10 → I) |
| Версия | `getprop ro.lineage.version` |
| Тип сборки | `getprop ro.lineage.releasetype` |
| Lineage SDK | `getprop ro.lineage.build.version.plat.sdk` |

Если свойства нет, показывается `—`, а не выдуманное значение.

## Иконка

Адаптивная иконка: розовый фон `#E6125E` и белый знак справки «?»
(`src/main/res/drawable-nodpi/ic_foreground.png`). Знак нарисован Rubik Bold и
вписан в безопасную зону маски.

## Структура

```
build.gradle.kts        Gradle: AGP 8.5.2, compileSdk 34, material 1.4.0
settings.gradle.kts
gradlew, gradlew.bat    обёртка Gradle 8.7
gradle/wrapper/
src/main/AndroidManifest.xml
src/main/java/org/zeroc/about/MainActivity.java
src/main/res/           layout, values (тема, цвета, строки), иконка
build.ps1               сборка + подпись (см. ниже)
```

## Сборка

Нужны JDK 17+ и минимальный Android SDK: `platforms/android-34` и
`build-tools/34.0.0`, плюс платформенный ключ AOSP.

```powershell
.\build.ps1
# свои пути: .\build.ps1 -Sdk D:\android -KeysDir D:\keys
```

`build.ps1` копирует проект в ASCII-временный каталог (Gradle и `aapt2` не
работают с кириллицей), пишет `local.properties`, запускает
`gradlew assembleRelease`, затем выравнивает `zipalign -p -f 4` и подписывает
`apksigner` ключом `platform.pk8`/`platform.x509.pem`. Подпись платформенная
(SHA-256 `c8a2e9bc…2ab8`), как у системных приложений.

Готовый APK — `dist\ZerOSAbout.apk`. В патч-zip и образ попадает он же; путь в
патче — `system/priv-app/ZerOSAbout/ZerOSAbout.apk`.

## Установка

В составе общего патча брендинга (`..\make_patch_zip.ps1`) или `system.img`.

Вручную (adb + root):

```powershell
adb push dist\ZerOSAbout.apk /data/local/tmp/ZerOSAbout.apk
adb shell su -c "mount -o remount,rw /"
adb shell su -c "mkdir -p /system/priv-app/ZerOSAbout"
adb shell su -c "cp /data/local/tmp/ZerOSAbout.apk /system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chmod 644 /system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chown 0:0 /system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chcon u:object_r:system_file:s0 /system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "touch /system/priv-app/ZerOSAbout; rm -f /data/system/package_cache/ZerOSAbout-*"
adb shell su -c "mount -o remount,ro /"
adb reboot
```

`touch` каталога и удаление `/data/system/package_cache/ZerOSAbout-*` обязательны:
Android 10 кэширует разобранный пакет системного приложения и считает кэш
валидным по mtime каталога, а `cp` поверх файла mtime не меняет — без этого
шага после перезагрузки берётся старый разбор и приложение падает
(«You need to use a Theme.AppCompat theme»). Общий установщик
(`..\patch`) делает это сам.

Перед перезагрузкой удалить пользовательскую копию, если она ставилась для
проверки: `adb uninstall org.zeroc.about`.

## Отладка вкладок

Приложение открывает вкладку по интенту (удобно для проверки без тапов):

```powershell
adb shell am start -n org.zeroc.about/.MainActivity --ei tab 1   # 0 система, 1 устройство, 2 ZerOS
```

## Оговорки

- `ro.product.model` даёт «Swift 2», производитель — «Wileyfox»; вместе
  получается «Wileyfox Swift 2».
- «Официальная» — это `ro.lineage.releasetype=OFFICIAL` из `build.prop`, а не
  статус на сервере LineageOS.
- Кнопка «Обновления» открывает `org.lineageos.updater`; если обновлятор
  удалён, нажатие ничего не делает.
- APK с AndroidX/Material весит больше прежней ручной сборки (около 1 МБ
  вместо 25 КБ) — это цена настоящих компонентов MD2.
