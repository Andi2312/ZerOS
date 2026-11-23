# zeroupdater — «ZerOS 1.0» в обновляторе

Magisk-модуль подменяет системный обновлятор LineageOS
(`/system/priv-app/Updater/Updater.apk`) на пересобранный с другим названием
системы в интерфейсе.

| Было | Стало |
|---|---|
| `LineageOS\n1.0` в шапке (`header_title_text`) | `ZerOS\n1.0` |
| `LineageOS %1$s` в списке сборок (`list_build_version`) | `ZerOS %1$s` |
| `LineageOS %1$s - %2$s` (`list_build_version_date`) | `ZerOS %1$s - %2$s` |

Число (`1.0`) берётся из `ro.lineage.build.version` — его выставляет
`zerobrand/system.prop`, а не этот модуль. Хочешь `ZerOS 2.0` — правь
`zerobrand` и перезагружайся, APK пересобирать не нужно.

## Что не меняется

Адреса и пути остаются стоковыми, иначе обновлятор перестанет работать:

- `updater_server_url` = `https://download.lineageos.org/api/v1/...`
- `menu_changelog_url` = `https://download.lineageos.org/%1$s/changes`
- `blocked_update_info_url` = `http://wiki.lineageos.org/...`
- `download_path` = `/data/lineageos_updates/`
- `export_path` = `LineageOS updates/`

Меняются только три подписи в `res/values*/strings.xml` во всех локалях, в том
числе в `values-ru` — язык системы на телефоне русский.

## Подпись

Обновлятор подписан **AOSP testkey** (`build/target/product/security`,
тег `android-10.0.0_r47`), а не платформенным ключом, как настройки.
Пересобранный APK подписывается тем же ключом, SHA-256 сертификата:

```
A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC
```

`build.ps1` проверяет это после подписи. Ключи качаются из
`android.googlesource.com` в `keys/` (в git не попадают).

## Сборка

Нужны JDK 21, apktool, apksigner, zipalign, python и подключённый телефон
(adb). Сборка идёт в `C:\work\updater_apk` — apktool и aapt2 не работают с
кириллическим путём. APK маленький (2,5 МБ), сборка занимает минуту-две.

```powershell
.\build.ps1
```

Шаги: `adb pull` → `apktool if framework-res.apk` → `apktool d` →
`patch_res.py` → `apktool b` → `zipalign -p 4` → `apksigner sign` →
проверка сертификата → `make_module.py`.

## Установка

```powershell
adb push dist\zeroupdater-0.0.1.zip /data/local/tmp/zeroupdater.zip
adb shell su -c "magisk --install-module /data/local/tmp/zeroupdater.zip"
adb shell su -c "rm -rf /data/system/package_cache"
adb reboot
```

`package_cache` чистится потому, что PackageManager иначе продолжает отдавать
закэшированный разбор системного APK.

Проверка: открыть «Настройки → Система → Обновление» или запустить
`org.lineageos.updater/.UpdatesActivity`, в шапке должно быть `ZerOS` и `1.0`.

## Откат

```powershell
adb shell su -c "magisk --remove-module zeroupdater"
adb shell su -c "rm -rf /data/system/package_cache"
adb reboot
```

## Оговорки

- Обновлятор по-прежнему ходит на серверы LineageOS: с `ro.lineage.releasetype=OFFICIAL`
  и `ro.lineage.build.version=1.0` (см. `../zerobrand/`) он может предлагать
  несуществующие обновления или падать с ошибкой проверки. Название в шапке
  это не лечит — это отдельная задача.
- После OTA/перепрошивки обновлятор вернётся стоковым, модуль нужно собрать
  заново.
