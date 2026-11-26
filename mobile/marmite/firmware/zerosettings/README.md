# zerosettings — «Версия ZerOS I» в настройках

Magisk-модуль подменяет системные настройки LineageOS
(`/system/product/priv-app/Settings/Settings.apk`) на пересобранные с правкой
экрана **«Версия прошивки»** в «О телефоне»:

- строка **«Версия API»** (`lineage_api_version`, Lineage SDK) убрана;
- строка **«Версия LineageOS»** переименована в **«Версия ZerOS I»** — ключ и
  контроллер остались штатными, поэтому значение по-прежнему берётся из
  `ro.lineage.version` (сейчас `1.0`, см. `../zerobrand/`).

Остальные строки экрана не тронуты: версия Android, патч безопасности,
версия модуля, база, ядро, дата сборки, номер сборки.

## Почему пересборка, а не тема

Нужные строки лежат не в `Settings.apk`, а в общей библиотеке
`org.lineageos.platform` (`string/lineage_version`, `string/lineage_api_level`),
и на них ссылается `res/xml/firmware_version.xml`. Переименовать их темой (RRO)
нельзя: подменить чужой `@lineageos.platform:string` из RRO-пакета нельзя, а
менять общую библиотеку — значит ломать все приложения LineageOS сразу.

Поэтому правится сам `Settings.apk`: заголовки становятся локальными строками
(`zer_version`), а пункт API просто удаляется из разметки.

## Подпись

`Settings` подписан **платформенным ключом AOSP** (testkey из
`build/target/product/security`, тег `android-10.0.0_r47`). Подпись даёт
приложению signature-разрешения системы (`WRITE_SECURE_SETTINGS`,
`DEVICE_POWER` и т.п.). Пересобранный APK подписывается **тем же ключом**,
иначе настройки потеряют права и часть экранов сломается.

SHA-256 сертификата, который должен получиться:

```
C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8
```

`build.ps1` проверяет это после подписи. Ключи качаются из
`android.googlesource.com` в `keys/` (в git не попадают).

## Сборка

Нужны JDK 21, apktool, apksigner, zipalign, python и подключённый телефон
(adb). Всё собирается в `C:\work\settings_apk`: apktool и aapt2 не работают с
кириллическим путём.

```powershell
.\build.ps1
```

Что делает скрипт:

1. тянет с телефона `Settings.apk`, `framework-res.apk` и
   `org.lineageos.platform-res.apk`;
2. ставит оба фреймворка в apktool (`apktool if`) — без `lineageos.platform`
   apktool падает на `Can't find framework resources for package of id: 63`;
3. декодирует APK, правит `res/xml/firmware_version.xml` и строки
   (`patch_res.py`);
4. собирает, выравнивает `zipalign -p 4`, подписывает платформенным ключом;
5. кладёт `dist\Settings.apk` и собирает модуль `dist\zerosettings-1.0.zip`.

Правка ресурсов вынесена в `patch_res.py`: скрипт идемпотентен и падает, если
разметка изменилась (например, после обновления прошивки).

## Установка

```powershell
adb push dist\zerosettings-1.0.zip /data/local/tmp/zerosettings.zip
adb shell su -c "magisk --install-module /data/local/tmp/zerosettings.zip"
adb shell su -c "rm -rf /data/system/package_cache"
adb reboot
```

`/data/system/package_cache` нужно чистить: PackageManager кэширует разобранный
системный APK, и без очистки после подмены файла продолжает показывать старые
ресурсы до сброса данных.

Проверка:

```powershell
adb shell am start -a android.settings.DEVICE_INFO_SETTINGS
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml
```

В дампе должно быть «Версия ZerOS I» и не должно быть «Версия API».

Проверено 20.11.2026 на Swift 2: экран «Версия прошивки» показывает «Версия
ZerOS I → <`ro.lineage.version`>», строки «Версия API» нет, `logcat -b crash`
пуст. Значение берётся из `zerobrand`, поэтому после смены версии модуль
пересобирать не нужно. `md5sum
/system/product/priv-app/Settings/Settings.apk` совпадает с собранным APK,
`lastUpdateTime` обновился после перезагрузки.

## Откат

```powershell
adb shell su -c "magisk --remove-module zerosettings"
adb shell su -c "rm -rf /data/system/package_cache"
adb reboot
```

Или выключить модуль в Magisk и перезагрузиться.

## Оговорки

- Модуль подменяет только APK. После обновления прошивки (или OTA) настройки
  обновятся вместе с системой, и правку нужно будет собрать заново — версия
  LineageOS в заголовке вернётся.
- Пересобранный APK подписан тем же ключом, поэтому обновление системного
  приложения не ломает его данные и права.
- `apktool if` перезаписывает фреймворк с id 1. Если в apktool уже лежал
  другой `framework-res.apk`, после сборки его нужно вернуть (`apktool if`
  нужного файла).
