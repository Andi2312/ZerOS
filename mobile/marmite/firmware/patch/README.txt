ZerOS I 1.0 — Wileyfox Swift 2 (marmite)
Патч брендинга: пишет файлы прямо в /system, без Magisk.

Что делает
----------
Заменяет в системном разделе:
  system/build.prop                            (добавлен блок свойств ZerOS)
  system/media/bootanimation.zip               (анимация «ZeroS»)
  system/product/overlay/ZerOSAccent.apk       (акцент pink #E6125E)
  system/product/overlay/ZerOSFont.apk         (шрифт Rubik)
  system/product/overlay/ZerOSShape.apk        (скруглённые иконки)
  system/product/overlay/ZerOSLogo.apk         (логотип в LineageParts)
  system/product/priv-app/Settings/Settings.apk  («Версия ZerOS I»)
  system/priv-app/Updater/Updater.apk          («ZerOS 1.0» в обновляторе)
  system/priv-app/ZerOSAbout/ZerOSAbout.apk    (приложение «О телефоне»)

Перед заменой копирует прежние файлы в /sdcard/zerobak-patch-<дата>/.

Требования
----------
- Wileyfox Swift 2 (marmite), уже установлена LineageOS 17.1
  (17.1-20210703-UNOFFICIAL-marmite), Android 10.
- TWRP. Загрузчик разблокирован.
- Свободно ~100 МБ на /sdcard для бэкапа.

Установка
---------
1. Скопировать zip на телефон (или SD-карту).
2. TWRP → Install → выбрать zip → подтвердить.
3. Reboot → System.

Проверка
--------
Настройки → О телефоне: «Версия ZerOS I» = 1.0.
В меню приложений — «О телефоне» (значок «?»): вкладки «Система»,
«Устройство», «ZerOS».
Обновлятор: в шапке «ZerOS 1.0».
Тема: акцент розовый, шрифт Rubik, иконки скруглённые.

Откат
-----
Проще всего перепрошить LineageOS 17.1 тем же zip-ом, что ставили
(он вернёт system). Либо вернуть файлы из /sdcard/zerobak-patch-<дата>/
в те же пути вручную (TWRP → Advanced → Terminal, раздел system смонтировать rw).

Важно
-----
- boot и vendor не трогаются.
- Раздел splash (заставка Wileyfox) не трогается.
- После записи установщик обновляет mtime каталогов приложений и удаляет их
  устаревший кэш разобранного пакета (/data/system/package_cache/<имя>-*).
  Android 10 считает этот кэш валидным по mtime каталога, а замена файла
  «на месте» mtime не меняет — без этого шага система берёт старый разбор и
  приложение падает при первом запуске после патча.
- После патча не нужны Magisk-модули zerobrand/zerosettings/zeroupdater;
  если они были установлены — удалить, иначе их подмены перекроют файлы.
- Это не полный образ: сначала нужна базовая LineageOS 17.1.
  Полный system.img — отдельным файлом в релизе.
- Root в комплект не входит: для ZerOS Magisk не нужен. Нужен root —
  поставить zip Magisk в TWRP отдельно (boot патчится только этим zip-ом).
