# Wileyfox Swift 2 (marmite) — рекавери и разблокировка

Устройство: Wileyfox Swift 2, `marmite`, MSM8937 (Snapdragon 430), 2/16 ГБ,
заводская прошивка была Android 7.1.2 (API 25), сборка `TOS317G - N2G48B`.

## Текущее состояние (27.11.2026)

- Загрузчик **разблокирован** (`ro.boot.flash.locked=0`,
  `verifiedbootstate=orange`).
- Стоит **LineageOS 17.1** — `17.1-20210703-UNOFFICIAL-marmite`,
  `lineage_marmite-userdebug 10 QQ3A.200805.001`, патч безопасности 2021-06-05.
  Прошито 17.11.2026 по шагам `flash_los.ps1` (см. «Основная прошивка для
  ZerOS Mobile»).
- В разделе `recovery` — **TWRP 3.7.0_9-0** в пропатченном виде
  (`twrp-3.7.0_9-0-marmite-datafix.img`): грузится с раздела, `/data`
  и `/sdcard` монтируются rw (см. «Патч TWRP»).
- `/data` **отформатирован заново** (`make_ext4fs`, ext4, 9.6 ГБ, занято
  ~0.6 ГБ под ROM и Magisk). Внутренняя память пустая.
- **Magisk 31.0** стоит поверх **стокового `boot` от LineageOS**
  (`magisk -v` → `31.0:MAGISK:D`).
- Домашний экран — **штатный лаунчер прошивки**: `com.android.launcher3`,
  activity `.lineage.LineageLauncher`, APK в
  `/system/product/priv-app/TrebuchetQuickStep/`. Эксперименты с лаунчерами
  (HomeWatch, ZerHome) сняты с телефона и удалены из репозитория — исходники
  остались в истории git.
- Брендинг **записан прямо в `/system`**, без Magisk-модулей: в конец
  `/system/build.prop` добавлен блок свойств ZerOS (`getprop
  ro.build.display.id` → `Zeros_marmite 1.0 ...`, `ro.lineage.version` →
  `1.0`), заменены загрузочная анимация, `Settings.apk` и `Updater.apk`,
  добавлены статические RRO темы и логотипа. Оригиналы — в `/data/zerobak/`.
  Текущая версия — **ZerOS I 1.0**.
- Личные файлы прежней системы сохранены локально и в репозиторий не попадают.
- Образы и архивы (recovery, сток 7.1.2, crDroid, Magisk, логи) лежат локально
  и в репозиторий не входят: готовая прошивка публикуется в **Releases**.

Важно: заводской `ro.build.display.id` = **TOS317G**, а на 4PDA эта сборка
помечена как
**«Россия и СНГ»**. Значит, аппарат из СНГ-партии, и сток качать надо тоже
CIS (`TOS317G`), а не международный (`TOS319I` / `TOS373J`).

Всё, что ниже, делается **на свой риск**. Разблокировка загрузчика стирает все
данные телефона.

## Что выбрать

На 4PDA для marmite версии рекавери разведены по прошивкам:

| Прошивка | Рекавери |
|----------|----------|
| Сток 7.1.2 / 8.x | **OrangeFox R11.0** (рекомендуется) или TWRP 3.2.3 by vm03 |
| Android 9+ (кастомные ROM) | официальный TWRP 3.7.0_9-0 или TWRP for PT by DinD_Ykt |

OrangeFox R11.0 — сборка от 13.08.2020, помечена `archived`, устройство снято
с поддержки. Но для стока она рабочая и проверена сообществом (в том числе
в гайдах по LineageOS 17.1).

## Стоковая прошивка 7.1.2

В теме 4PDA «Официальные прошивки» сборки идут с пометкой региона:

| Сборка | Регион |
|--------|--------|
| `TOS317G` | Россия и СНГ |
| `TOS205E` | Россия и СНГ |
| `TOS319I`, `TOS249H`, `TOS208G`, `TOS163B`, `TOS373J` | международная |

Текущая сборка аппарата — **`TOS317G - N2G48B`** (дата 13.11.2017, патч
2017-11-01), то есть СНГ-ветка. Поэтому качать надо **`TOS317G`** (Fastboot),
а не международную: другая привязка модема/преднастроек даёт риск «нет сети»
и чужих APN.

Учти: код `N2G48B` — это общий ID сборки Android 7.1.2 от Google, он одинаков
у всех вариантов, а различает их именно префикс `TOS...`. Смотреть надо на
`ro.build.display.id`.

### Где взять (ссылки в шапке темы не работают)

В шапке 4PDA-темы ссылки ведут на `cloud.mail.ru/public/JEWW/bMe4rXfbg` и на
`wileyfox.wetransfer.com` — **оба источника мертвы**:

- `cloud.mail.ru` — весь публичный шар удалён (API отдаёт `not_exists`);
- wetransfer-ссылки отдают `403` (срок хранения истёк).

Живое зеркало того же набора: **Яндекс.Диск**
<https://yadi.sk/d/gV6mNH0X3J8QBy> → `Official ROM` → `Fastboot` / `Recovery`.

Нужный файл (СНГ-ветка):

| Файл | Размер | MD5 |
|------|--------|-----|
| `marmite-7.1.2-TOS317G (fastboot).zip` | 1 630 852 835 | `41f80a60d72276c6525a6c5b58a7e923` |
| `marmite-7.1.2-TOS317G (recovery).zip` | 1 616 925 282 | `c19980207981a0e9540943e698771f5b` |

Рядом лежат `Swift 2 fastboot flash.bat` (MD5 `e97eadc6f0d70a5ccfda43e450871c7d`)
и `How to flash fastboot.doc` (`952cae4103ef35f89f161b129e0acea1`) — уже скачаны
в `stock\`.

Международные сборки там же: `TOS205E`, `TOS249H` (`Android_7.1.2_...TOS249H`),
`TOS208G`, `TOS163B`, `TOS118C`, `TOS089A`. **Не берите
`marmite-7.1.2-TOS319I(fastboot).zip`** — он в зеркале битый, всего 3566 байт.

**Скачанный `TOS317G (fastboot).zip` тоже битый:** MD5 файла
`e935571ca31346db6baf08512dac6bdc` вместо заявленного
`41f80a60d72276c6525a6c5b58a7e923`, `7z t` даёт `Data Error : system.img`.
Остальные файлы (`boot.img`, `recovery.img`, `NON-HLOS.bin`, `aboot` и т.д.)
распаковываются без ошибок — годен только `system.img`. Если понадобится
возврат на сток, качать архив заново (лучше с проверкой CRC).

### Порядок прошивки стока

1. Разблокировать загрузчик (см. выше) — только после этого fastboot даст писать.
2. Распаковать архив, зайти в папку и запустить `Swift 2 fastboot flash.bat`
   (в PATH должны быть `adb.exe` и `fastboot.exe`).
   Скрипт делает:

   ```
   fastboot flash modem NON-HLOS.bin
   fastboot flash dsp adspso.bin
   fastboot flash devcfg devcfg.mbn
   fastboot flash aboot emmc_appsboot.mbn
   fastboot flash keymaster keymaster.mbn
   fastboot flash rpm rpm.mbn
   fastboot flash sbl1 sbl1.mbn
   fastboot flash splash splash.img
   fastboot flash tz tz.mbn
   fastboot flash boot boot.img
   fastboot flash recovery recovery.img
   fastboot flash system system.img
   fastboot format userdata
   fastboot reboot
   ```

   `oem lock` в скрипте нет — загрузчик останется разблокированным, это то, что
   нужно. `format userdata` стирает данные.

   Учти: строка `fastboot flash recovery` на этом загрузчике **не пишет
   в раздел** (см. раздел «Установка OrangeFox / TWRP»), так что после стока
   рекавери всё равно надо будет записывать через `dd`/zip.
3. После прошивки сток вернёт своё рекавери — OrangeFox надо прошить заново.

### Альтернатива: свой бэкап вместо чужого стока

Если цель — просто иметь «точку возврата», то лучший её вариант — не чужой
архив, а **свой** дамп: сразу после установки OrangeFox сделать
`Backup` всех разделов (в первую очередь `Boot`, `System`, `Modem`/`EFS`,
`Data`) и скопировать на ПК. Это байт-в-байт именно твоя CIS-сборка `TOS317G`.


## Файлы в `recovery\`

- `OrangeFox-R11.0-marmite-recovery.img` — образ для fastboot (достать из
  официального zip), 28 476 716 байт.
  - MD5: `8e043bb02258bdaebfea49a01670d739`
  - SHA-256: `ca889f88a2f35e9f2b7470a0a892a3b912edbd4a20a76029b3d321ebe9251acd`
  - заголовок `ANDROID!`
- `OrangeFox-marmite-stable@R11.0.zip` — официальный zip с orangefox.download,
  40 728 987 байт, MD5 `be30ad3fd193dd4a03fb992e0a0701e9` (совпал).
- `twrp-3.7.0_9-0-marmite.img` — официальный TWRP (для Android 9+),
  25 092 096 байт, SHA-256 `7cb1791c…97b836` (совпал с dl.twrp.me).

## Разблокировка загрузчика

1. Настройки → О телефоне → 7 раз по «Номер сборки».
2. Для разработчиков → включить **«Заводская разблокировка»** и «Отладка по USB».
3. Подключить телефон, проверить:

   ```
   adb devices
   adb reboot bootloader
   fastboot devices
   ```

4. Разблокировать (данные будут стёрты, телефон спросит подтверждение):

   ```
   fastboot oem unlock
   ```

   Если просит код — попробовать `fastboot oem unlock-go`.

5. Проверить: `fastboot getvar unlocked` → `unlocked: yes`.

## Установка OrangeFox / TWRP

**Важно:** на этом загрузчике (`marmite-N-ge8bbd8f`, `secure:yes`)
`fastboot flash recovery` **не пишет в раздел**: команда отвечает `OKAY`,
но `getvar partition-size:recovery` отдаёт пусто, и в разделе остаётся
прежний образ. Проверено дампом раздела после прошивки.

Рабочие способы:

1. **Через zip в рекавери** (штатный): загрузить рекавери в RAM
   (`fastboot boot ...img`), закинуть `adb push OrangeFox-...zip /sdcard/`,
   затем `Install` → zip.
2. **Прямой `dd`** из загруженного в RAM рекавери:

   ```
   adb push twrp-3.7.0_9-0-marmite.img /tmp/twrp.img
   adb shell "dd if=/tmp/twrp.img of=/dev/block/bootdevice/by-name/recovery bs=4096; sync"
   adb shell "head -c 25092096 /dev/block/bootdevice/by-name/recovery | md5sum"   # сверять с образом
   ```

   После `dd` md5 первых N байт раздела должен совпадать с md5 файла.

Загрузить рекавери с раздела можно из fastboot:

```
fastboot oem ...            # не поддерживается: "unknown command"
```

либо записав BCB в `misc` и перезагрузившись:

```
printf 'boot-recovery' | dd of=/dev/block/bootdevice/by-name/misc bs=1 seek=0 conv=notrunc
printf 'recovery\n'   | dd of=/dev/block/bootdevice/by-name/misc bs=1 seek=64 conv=notrunc
```

`misc` перед этим лучше сохранить (`dd if=...misc of=/tmp/misc.bin`) — BCB
загрузчик после загрузки рекавери **не очищает**, и без очистки телефон будет
снова уходить в рекавери.

### Патч TWRP: чтобы монтировался `/data` от Android 10 (crDroid / LineageOS)

Официальный `twrp-3.7.0_9-0-marmite.img` **не монтирует** `/data`, созданный
Android 10. Причин две, обе надо снять:

1. В `etc/recovery.fstab` (в ramdisk) строка
   `/data ext4 /dev/block/bootdevice/by-name/userdata  length=-16384` —
   старый трюк под crypto-footer, обрезает последние 16 КБ раздела.
   Нужно убрать `length=-16384`.
2. Ядро TWRP собрано **без `CONFIG_QUOTA`**, а crDroid форматирует `/data`
   с фичей `quota`:
   `EXT4-fs (mmcblk0p48): Filesystem with quota feature cannot be mounted RDWR
   without CONFIG_QUOTA`. Ядро TWRP лечит только заменой на ядро из ROM.

Готовый образ: `recovery\twrp-3.7.0_9-0-marmite-datafix.img`, 19 138 560 Б,
MD5 `6f7bfa9ed4fc7c50429e3c31b5dc72f4` — ramdisk TWRP (fstab без
`length=-16384`) + ядро и заголовок из `boot.img` crDroid. Проверено: грузится
с раздела, `/data` и `/sdcard` монтируются rw, запись работает.

Пересобрать можно скриптами в `recovery\`:

```
python recovery\patch_boot.py    # ramdisk TWRP без length=-16384
python recovery\build_combo.py   # ядро ROM + ramdisk TWRP
```

Оговорка: ядро в рекавери теперь от ROM. После обновления прошивки его можно
пересобрать тем же `build_combo.py` из нового `boot.img` (иначе рекавери
останется на старом ядре — обычно это не мешает).

Проверено 17.11.2026: после перехода на LineageOS 17.1 тот же
`twrp-3.7.0_9-0-marmite-datafix.img` работает как есть — `/data` (ext4 без
quota) монтируется rw, стирание и установка zip-ов проходят.

### Что было с OrangeFox на стоке 7.1.2

OrangeFox R11.0 запускается и с раздела, и из RAM. Но у него нет поддержки
шифрования стока для TWRP 3.7-ветки и он не умеет SAR — для crDroid 10 нужен
именно TWRP 3.7.0_9-0.

### Грабли: «Your device is corrupt»

Если в рекавери сделать `mount -o remount,rw /system` на стоковой системе,
ext4 перепишет суперблок, dm-verity перестанет сходиться и телефон при загрузке
показывает **«Your device is corrupt. It can't be trusted and will not boot»**,
после чего уходит в fastboot. Раздел `/system` на стоке **не монтировать rw**.

Лечится только перепрошивкой `system.img` из стока (или установкой кастомной
прошивки, которая затирает систему целиком).

## Основная прошивка для ZerOS Mobile: LineageOS 17.1

Решением от 17.11.2026 основной базой выбрана **LineageOS 17.1** (Android 10,
API 29) — чище crDroid, меньше предустановленной обвязки под свою оболочку.
**Прошито и проверено в тот же день.**

| Файл | Размер | MD5 |
|------|--------|-----|
| `lineage-17.1-20210703-UNOFFICIAL-marmite.zip` | 607 608 612 | `14c7084173e4302c17717b208a23e18a` |
| `Magisk_v31.0 (2).zip` | 40 456 082 | — |

Архив проверен: zip целый (`testzip` чистый, 14 записей), `updater-script`
требует `ro.product.device == marmite`, кладёт `system` в `system`, `vendor`
в `oem` и `boot.img` в `boot`.

Результат установки:

```
Target: Wileyfox/Swift2/marmite:8.1.0/OPM5.171019.017/38c792b31d:user/release-keys
Patching system image unconditionally...
Patching vendor image unconditionally...
script succeeded: result was [1.000000]
```

После перезагрузки: `ro.build.version.release=10`,
`ro.lineage.version=17.1-20210703-UNOFFICIAL-marmite`, `sys.boot_completed=1`.

### Порядок (проверенный)

Скрипт: `flash_los.ps1` (требует `-Yes`, данные и внутренняя память стираются).

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File flash_los.ps1 -Yes
```

Первый проход остановился на `twrp wipe data` (см. «Грабли `twrp` CLI» ниже).
Прошивка была добита теми же командами вручную, после чего скрипт приведён
в соответствие с рабочим порядком: пауза на подъём GUI, `wipe cache` →
`wipe data` → `format data`, проверка `script succeeded`. Синтаксис проверен
парсером PowerShell; целиком с нуля после правок не прогонялся — аппарат уже
прошит.

Что делает по шагам:

1. `adb reboot recovery`, ждёт состояния `recovery` и `ro.twrp.version`;
2. снимок текущих `getprop` в `logs\before-<дата>.txt`;
3. пауза 40 с, чтобы поднялся GUI TWRP;
4. `twrp wipe cache`, `twrp wipe data`, `twrp format data`;
5. `adb push` ROM (607 МБ, ~76 с на 7.6 МБ/с) и Magisk в `/sdcard`;
6. `twrp install '/sdcard/lineage-17.1-marmite.zip'`;
7. `twrp install '/sdcard/Magisk-v31.0.zip'`;
8. `adb reboot`, ожидание `device` + `sys.boot_completed`.

### Грабли `twrp` CLI (важно)

`/sbin/twrp` в TWRP — это «openrecoveryscript command line tool»: он не делает
работу сам, а пишет команду в скрипт и ждёт, пока её выполнит GUI.

- Если дёрнуть его **сразу после** того, как recovery поднялся по adb, он
  печатает `TWRP does not appear to be running. Waiting for TWRP to start . . .`
  и висит (а `adb shell` в этот момент отваливается с `device not found`).
  Первый запуск скрипта на этом и упал. Лечится паузой ~30–40 с после
  подключения рекавери: GUI успевает подняться, и дальше команды отвечают
  сразу.
- `twrp wipe data` **не трогает `/data/media`** (внутреннюю память): в логе
  прямо пишет `Очистка /data без очистки /data/media`. Для полного сброса нужен
  `twrp format data` — только он пересоздаёт раздел целиком.
- После `format data` TWRP пишет «возможно, потребуется перезагрузить
  рекавери, чтобы снова использовать /data», но `/sdcard` уже создан заново и
  `adb push` в него работает — перезагружать не понадобилось.
- Проверить, что CLI живой, можно безобидным `twrp wipe cache`
  (форматирование cache) — им же и начинаем.

### Грабли PowerShell: кириллица в `.ps1`

Windows PowerShell 5.1 читает `.ps1` без BOM как ANSI. Файл, записанный в
UTF-8 без BOM, превращается в мусор, и парсер падает на апострофах внутри
русских строк (`Непредвиденная лексема`). Скрипты с русскими сообщениями надо
сохранять **с BOM**:

```python
open(path, 'w', encoding='utf-8-sig', newline='\r\n').write(text)
```

## Запасной вариант: crDroid 6.15

**crDroid 6.15** для marmite — Android 10 (LineageOS 17.1), автор nikith290.
Стоял на аппарате до 17.11.2026, оставлен как резерв.

- Файл: `crDroidAndroid-10.0-20210212-marmite-v6.15.zip`, 699 629 222 байт,
  MD5 `a6f06bf052d4ed30e0007b609676ef79`
- Зеркало: пост 4PDA `p=96749951` → Google Drive
  `file/d/1KJl9nDix3efzDkd4uo9Yj2-Ga7KMtP2r`
- В архиве: `system.new.dat.br` (588 МБ) → раздел `system`,
  `vendor.new.dat.br` (99 МБ) → раздел **`oem`** (в ROM он играет роль
  `vendor`), `boot.img` → раздел `boot`.
- Ставить через **TWRP 3.7.0_9-0**: ROM system-as-root, в его `fstab.qcom`
  у `userdata` — `encryptable=footer`, то есть данные не шифруются.

Порядок установки:

1. `TWRP` → `Wipe` → `Format Data` (снимает стоковое шифрование, стирает
   `/sdcard`).
2. `Install` → `crDroidAndroid-10.0-...zip`.
3. Перезагрузка в систему (первый запуск ~2–5 минут).

## Magisk (root)

Сейчас стоит **Magisk 31.0**, поставленный **zip-ом в TWRP** уже после
LineageOS 17.1: инсталлятор сам распаковал стоковый `boot` от LOS
(`Legacy SAR`, `System-as-root`, `keep dm-verity`), пропатчил ramdisk и записал
обратно в `boot`. Проверено на системе: `magisk -v` → `31.0:MAGISK:D`.

**В релиз `marmite/I-1.0` root не входит.** ZerOS работает без него: брендинг и
приложения лежат файлами в `/system`, Magisk-модули не нужны. «Встроить» Magisk
означало бы прошить пропатченный `boot.img` — это привязка релиза к одной
сборке LineageOS 17.1 и риск кирпича при несовпадении, поэтому в патч и образ
`boot` не входит (и по правилам TK-P86 не прошивается). Нужен root — ставится
отдельно, zip-ом Magisk в TWRP, как описано выше.

Файлы в `magisk\` — от прежней системы (crDroid), **для текущей прошивки они
уже не актуальны**, оставлены как история:

| Файл | Что это |
|------|---------|
| `boot_crdroid_stock.img` | чистый `boot.img` из zip crDroid (11 628 544 Б, MD5 `7ab4fc6ffafce7aa381d321ac93af412`) |
| `magisk_patched-31000-marmite.img` | он же, пропатченный Magisk 31.0 (11 831 296 Б, MD5 `66c9b068ec0c6baec93f3122d1004101`) |
| `boot_backup_magisk22.img` | что лежало в `boot` до этого — старый патч Magisk 22.1 (kernel = crDroid + dtb от 22.1) |

Чистый `boot.img` от LineageOS 17.1 лежит внутри самого zip-а
(`boot.img`, 11 601 920 Б) — если понадобится перепатчить вручную, брать его.

### Как пропатчить/переустановить без приложения

Ассеты Magisk (те, что достаёт само приложение) лежат в `/data/adb/magisk`.
Патчить надо **чистый** `boot.img`, а не уже пропатченный — иначе magiskboot
скажет `patched by old (unsupported) Magisk`.

В TWRP (adb root):

```
rm -rf /tmp/mg && mkdir -p /tmp/mg && cp -a /data/adb/magisk/. /tmp/mg/
cd /tmp/mg && rm -f kernel kernel_dtb ramdisk.cpio new-boot.img stock_boot.img config *.xz
export BOOTMODE=false OUTFD=1 KEEPVERITY=true KEEPFORCEENCRYPT=false
sh boot_patch.sh /tmp/boot_clean.img        # → /tmp/mg/new-boot.img
dd if=/tmp/mg/new-boot.img of=/dev/block/bootdevice/by-name/boot bs=4096
sync
```

`BOOTMODE` обязательно выставлять вручную: в скрипте он используется как
`$BOOTMODE && ...`, и без него shell падает на синтаксисе.

Затем `rm -rf /data/adb/magisk` (старые бинарники) и перезагрузка — демон
распакует свои файлы заново.

Признак того, что в `boot` старый Magisk: в ramdisk (magiskboot unpack) есть
`overlay.d/sbin/magisk32.xz` и `magisk64.xz`, а `magisk -v` отвечает 22.1 —
новое приложение такой патч не видит.

## Брендинг ZerOS

С версии 1.0 брендинг **записан файлами в реальный `/system`** — без Magisk.
Папки `zerobrand/`, `zerosettings/`, `zeroupdater/`, `zerocabout/` остались
сборщиками артефактов; модули из первых трёх больше не устанавливаются.
Установщики лежат в `patch/` (только изменённые файлы) и `full/` (весь образ
`system.img` для TWRP).

| Файл в `/system` | Что |
|---|---|
| `build.prop` | блок свойств ZerOS дописан в конец (последнее значение выигрывает), оригинал в `/data/zerobak/` |
| `media/bootanimation.zip` | анимация «ZeroS» |
| `product/overlay/ZerOS{Accent,Font,Shape}.apk` | статические RRO: pink, Rubik, roundedrect |
| `product/overlay/ZerOSLogo.apk` | статический RRO: логотип в LineageParts |
| `product/priv-app/Settings/Settings.apk` | «Версия ZerOS I» |
| `priv-app/Updater/Updater.apk` | «ZerOS 1.0» в обновляторе |
| `priv-app/ZerOSAbout/ZerOSAbout.apk` | приложение «О телефоне» |

`/vendor` — squashfs (`ro`), поэтому `ro.product.vendor.name` и
`ro.product.odm.name` остались `lineage_marmite`; строки в `build.prop` на
них не влияют. Всё остальное применяется.

Сборка артефактов:

```powershell
powershell -File zerobrand\bootanimation\make_bootanimation_from_video.ps1   # bootanimation.zip из видео (ffmpeg + 7z)
powershell -File zerobrand\theme\build_theme.ps1                  # три статических RRO темы
powershell -File zerobrand\logo\build_overlay.ps1                 # RRO-оверлей логотипа
.\zerosettings\build.ps1                                          # Settings.apk
.\zeroupdater\build.ps1                                           # Updater.apk
```

Установка — три способа (подробности в `zerobrand/README.md`):

```powershell
# 1) полный zip для TWRP: весь system.img внутри, ставится как ROM (~0,55 ГБ)
7z x dist\ZerOS-I-1.0-marmite-system.img.7z -oC:\work\zos
.\make_full_zip.ps1 -SystemImg C:\work\zos\system.img
adb push dist\ZerOS-I-1.0-marmite-full.zip /sdcard/

# 2) патч-zip поверх уже стоящей LineageOS 17.1 (~40 МБ), ставится в TWRP
.\make_patch_zip.ps1 -BuildProp C:\work\sys\build.prop.new
adb push dist\ZerOS-I-1.0-marmite-patch.zip /sdcard/

# 3) полный образ system (449 МБ в 7z, 3,5 ГБ внутри) — fastboot flash system
7z x dist\ZerOS-I-1.0-marmite-system.img.7z
fastboot flash system system.img
```

Полный zip пишет образ напрямую в раздел (`unzip -p … | dd`), затем читает
раздел обратно и сверяет md5, после чего чистит `/data/system/package_cache`
и `/data/resource-cache`. Установщик — `full/META-INF/.../update-binary`,
сборка — `make_full_zip.ps1` (образ сжат deflate внутри zip).


`boot`, `vendor` и `recovery` в релиз не входят и не прошиваются. Образ снят
с раздела при смонтированном ro `/system`, `e2fsck -fn` чистый, md5 файлов
внутри сверены с исходниками.

Загрузочная анимация — клип `zerobrand\bootanimation\zeros.mp4` (720×1280,
30 fps, 4,5 с): чёрный фон, wordmark «ZeroS» и медленно плывущие белые линии.
Скрипт `make_bootanimation_from_video.ps1` вытаскивает кадры ffmpeg'ом и
упаковывает 7-Zip'ом (`-mx0`, stored); клип не бесшовный, поэтому кадры идут
вперёд и назад — 268 кадров в цикле `part0` без рывков. Прежний вариант с
надписью «ZerOS I» рисует `make_bootanimation.ps1` — оставлен запасным.

Обои — родные Lineage, установка их не трогает: свои с телефона убраны,
прежние возвращены из бэкапа (картинка и `wallpaper_info.xml`). Звуки тоже
заводские.

Тема по умолчанию — акцент «pink», шрифт Rubik, форма иконок roundedrect —
вшита тремя статическими RRO-оверлеями (`org.zeroc.theme.accent`,
`.font`, `.shape`): они лежат в `/system/product/overlay`, включаются сами
при каждой загрузке и из «Стилей» не выключаются. Исходники —
`zerobrand\theme\`, сборка `zerobrand\theme\build_theme.ps1`.

Звуки — заводские: своих `.ogg` нет, звук отключения зарядки тоже убран
(в LineageOS 17.1 его нет).

Логотип — статический RRO-оверлей `zerobrand\logo\ZerOSLogo.apk` в
`/system/product/overlay`: подменяет `drawable/logo_lineage` в LineageParts,
поэтому в пасхалке («О телефоне» → тап по «Версия ZerOS I») рисуется wordmark
«ZerOS I» вместо логотипа LineageOS. Оверлей подписан платформенным ключом;
поставить его отдельным APK нельзя — только предустановленным в системе.

Откат: перепрошить LineageOS 17.1 (вернёт system) или вернуть файлы из
`/sdcard/zerobak-patch-<дата>/`. Таблица значений, детали сборки и оговорки
(Updater, Lineage SDK) — в `zerobrand/README.md`.

## Настройки: «Версия ZerOS I»

Пересобранный `Settings.apk` лежит прямо в
`/system/product/priv-app/Settings/Settings.apk`: в «О телефоне» убрана строка
«Версия API», а «Версия LineageOS» переименована в «Версия ZerOS I». Значение
по-прежнему берётся из `ro.lineage.version` (сейчас `1.0`).

```powershell
.\zerosettings\build.ps1        # → zerosettings\dist\Settings.apk
```

APK подписывается платформенным ключом AOSP (`android-10.0.0_r47`), иначе
настройки теряют signature-разрешения. В патч-zip и образ попадает готовый
APK; отдельная установка модулем больше не нужна. Подробности — в
`zerosettings/README.md`.

## Обновлятор: «ZerOS 1.0»

`Updater.apk` в `/system/priv-app/Updater/`: в шапке обновлятора `ZerOS` с
версией `1.0` (номер из `ro.lineage.build.version`) вместо `LineageOS`, так
же в списке сборок. Адреса сервера, ссылки на wiki и пути загрузок не
тронуты. Подпись — AOSP testkey, как у штатного обновлятора.

```powershell
.\zeroupdater\build.ps1         # → zeroupdater\dist\Updater.apk
```

Число в шапке берётся из `ro.lineage.build.version`, поэтому менять версию
можно без пересборки APK. Подробности — в `zeroupdater/README.md`.

## «О телефоне»: приложение ZerOSAbout

Отдельное приложение `org.zeroc.about` в
`/system/priv-app/ZerOSAbout/ZerOSAbout.apk` — своя иконка «О телефоне» в меню
приложений. Настоящий Material Design 2 (`MaterialToolbar`, `TabLayout`,
`MaterialCardView`, `MaterialButton` из `com.google.android.material:1.4.0`):
вкладки «Система», «Устройство», «ZerOS», розовый `colorPrimary`, Rubik.
Показывает живые данные устройства (версия Android, патч безопасности, ядро,
сборка, модель, экран, память, накопитель) и блок ZerOS (поколение, версия,
тип сборки, Lineage SDK), кнопку «Обновления». Штатный экран в настройках не
меняется.

```powershell
.\zerocabout\build.ps1          # → zerocabout\dist\ZerOSAbout.apk
```

Собирается Gradle (AGP 8.5.2, `compileSdk 34`): нужны JDK 17+ и минимальный
SDK (`platforms/android-34`, `build-tools/34.0.0`) и платформенный ключ.
Сборка идёт во временном ASCII-каталоге — Gradle и `aapt2` не работают с
кириллицей; после сборки APK выравнивается и подписывается. Подробности — в
`zerocabout/README.md`.

## Важно

- **Не закрывать загрузчик обратно.** На Swift 2 после `fastboot oem lock`
  телефон перестаёт загружаться (известный кирпич).
- После разблокировки при старте может появляться предупреждение на 5 секунд —
  это нормально (verified boot `orange`).
- Если что-то пошло не так, при разблокированном загрузчике можно вернуть сток:
  официальные fastboot-образы — в теме 4PDA «Wileyfox Swift 2 / Swift 2 Plus /
  Swift 2 X — Официальные прошивки».
- LineageOS 17.1 для marmite (неофициальная, Android 10) ставится через
  OrangeFox/TWRP после разблокировки.
