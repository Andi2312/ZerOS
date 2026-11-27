# zerocabout — приложение «О телефоне»

Отдельное приложение `org.zeroc.about` («О телефоне») со своей иконкой в меню
приложений. С версии 1.0 лежит в системе:
`/system/priv-app/ZerOSAbout/ZerOSAbout.apk`. Штатный экран настроек не
меняется — приложение самостоятельное.

## Что показывает

Material Design 2: верхняя панель, три вкладки и карточки. Логотипа в шапке нет,
только название и версия.

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
(`res/drawable-nodpi/ic_foreground.png`). Знак нарисован Rubik Bold и вписан в
безопасную зону маски.

## Сборка

Без Gradle: `aapt2`, `javac`, `dx`, `zipalign`, `apksigner`. Нужны JDK,
минимальный Android SDK (platform 29 и build-tools 29.0.3) и платформенный
ключ AOSP.

```powershell
.\build.ps1
# свои пути: .\build.ps1 -Sdk D:\android -KeysDir D:\keys
```

Скрипт копирует `res`, `src` и манифест во временный ASCII-каталог: `aapt2` и
`apksigner` не открывают кириллические пути. `classes.dex` добавляется в APK
через 7-Zip (ZipArchive в режиме Update портит архив), затем `zipalign -p -f 4`
и подпись `platform.pk8`/`platform.x509.pem`. Подпись платформенная
(SHA-256 `c8a2e9bc…2ab8`), как у системных приложений.

Готовый APK — `dist\ZerOSAbout.apk` (~25 КБ). В патч-zip и образ попадает он
же; путь в патче — `system/priv-app/ZerOSAbout/ZerOSAbout.apk`.

## Установка

В составе общего патча брендинга (`..\make_patch_zip.ps1`) или `system.img`.

Вручную (adb + root):

```powershell
adb push dist\ZerOSAbout.apk /data/local/tmp/ZerOSAbout.apk
adb shell su -c "mount -o rw /dev/block/bootdevice/by-name/system /mnt/zerosys"
adb shell su -c "mkdir -p /mnt/zerosys/system/priv-app/ZerOSAbout"
adb shell su -c "cp /data/local/tmp/ZerOSAbout.apk /mnt/zerosys/system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chmod 644 /mnt/zerosys/system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chown 0:0 /mnt/zerosys/system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb shell su -c "chcon u:object_r:system_file:s0 /mnt/zerosys/system/priv-app/ZerOSAbout/ZerOSAbout.apk"
adb reboot
```

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
