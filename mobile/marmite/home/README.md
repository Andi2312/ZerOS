# ZerOS Home — архив экспериментов с домашним экраном Swift 2

> **Архив, на телефоне этого нет.** Домашний экран сейчас — штатный лаунчер
> прошивки `com.android.launcher3` (`.lineage.LineageLauncher`), см.
> `../README.md`. Здесь остались исходники двух заброшенных подходов: свой
> лаунчер ZerHome и форк AOSP Launcher3 (HomeWatch). Собираются по-прежнему,
> но не устанавливаются.

ZerHome был написан с нуля, без Launcher3 и без androidx: обычное приложение
`com.zeroc.home` с activity `.HomeActivity`. Ставился как APK и назначался
домашним штатной командой Android.

- Версия: `0.1.0` (`ZerOS I 0.1.0`)
- minSdk 26, targetSdk 29, Java 8, зависимости — только framework
- Подпись: `zeroshome.jks` (генерируется при первой сборке в
  `%TEMP%\opencode\android\zeroshome.jks`)

## Что умел

### Экран «Пуск»

- Сетка **4 × 6**, плитки 1 × 1, 2 × 2 и 4 × 2; ячейка считается и по ширине,
  и по высоте, поэтому сетка никогда не уезжает под панель навигации.
- Фон — системные обои плюс тёмный градиентный скрим; статус-бар прозрачный.
- Плоская плитка: цветной прямоугольник и иконка приложения **на всю плитку**
  (пропорции сохраняются, лишнее обрезается по центру). Рисование иконки
  ограничено прямоугольником плитки через `clipRect`, поэтому крупные иконки
  не залезают на соседние плитки и на зазоры. Подпись — в левом нижнем углу
  поверх затемнения, чтобы читалась на любой иконке. У 1 × 1 подписи нет —
  иконка занимает всю плитку.
- Палитра из 8 приглушённых цветов, индекс выбирается хешем компонента:

  | | | | |
  |---|---|---|---|
  | `#0078D7` | `#1F7A6B` | `#C0504D` | `#E36C09` |
  | `#7A3B8F` | `#2D7D46` | `#A0522D` | `#4A5568` |

- Долгое нажатие поднимает плитку и включает перенос; отпускание без движения
  открывает меню: **размер плитки**, **сменить цвет**, **убрать с экрана**,
  **информация о приложении**. Плитки хранятся в
  `shared_prefs/zerhome.xml` строкой `component,col,row,spanX,spanY,color`.
- При первом запуске Start заполняется сам: звонок 2 × 2, SMS/MMS 2 × 2,
  браузер 4 × 2, камера / часы / настройки 1 × 1, дальше — свободные
  приложения. Приложения ищутся по пакету (`dialer`, `messaging`, `jelly`,
  `snap`, `deskclock`, `settings`), а не только по интенту: так плитка не
  уедет на чужой лаунчер, перехвативший `DIAL` или `VIEW http`.
- Удалённые приложения вычищаются из Start при следующей загрузке списка.

### Нижняя панель

Своя чёрная полоса 48 dp: **Назад** слева, **Пуск** по центру, **Поиск**
справа — всё нарисовано вручную (`NavBarView`). Над панелью, в правом нижнем
углу экрана, висит отдельная круглая кнопка **списка приложений** (три полосы,
`AppsButton`, 52 dp, отступ 12 dp от края); список открывается поверх неё, так
что она не перекрывает строки. Нажатие на кнопку уводит Start влево (плитки
уезжают с параллаксом), а список въезжает справа; при возврате Start плитки
падают на место слева одна за другой. Системный navbar скрывается на время
работы лаунчера:

```powershell
adb shell settings put global policy_control immersive.navigation=com.zeroc.home
# откат: adb shell settings delete global policy_control
```

Назад в списке сначала очищает поиск, второй раз — закрывает список; на Start
ничего не делает, как в Windows Phone. Пуск закрывает список, Поиск открывает
его сразу с полем ввода, круглая кнопка над панелью открывает и закрывает
список.

### Список приложений

Полноэкранная панель поверх Start: заголовки по алфавиту, буквенный указатель
справа, поиск по подписи (поле видно всегда, клавиатура — только если список
открыт кнопкой с поиском), «Ничего не найдено» для пустой выдачи. Долгое
нажатие на строке — **закрепить на «Пуск»** или **информация о приложении**.
Свой лаунчер в списке скрыт.

## Что в папке

| Путь | Что это |
|---|---|
| `zerhome/src/com/zeroc/home/HomeActivity.java` | точка входа: сборка UI, загрузка приложений, плитки по умолчанию, запуск |
| `zerhome/src/com/zeroc/home/TileGrid.java` | сетка 4 × 6, перенос, смена размера, меню плитки |
| `zerhome/src/com/zeroc/home/TileView.java` | отрисовка одной плитки |
| `zerhome/src/com/zeroc/home/NavBarView.java` | Назад / Пуск / Поиск |
| `zerhome/src/com/zeroc/home/AppsButton.java` | круглая кнопка списка над панелью |
| `zerhome/src/com/zeroc/home/AppListPane.java` | список приложений, поиск, указатель, закрепление |
| `zerhome/src/com/zeroc/home/AppListAdapter.java` | строки и заголовки списка |
| `zerhome/src/com/zeroc/home/LetterBar.java` | алфавитный указатель |
| `zerhome/src/com/zeroc/home/AppEntry.java`, `TileInfo.java`, `Palette.java` | модели и палитра |
| `zerhome/res/` | строки, тема, иконка, строки списка, скрим |
| `zerhome/build.ps1` | сборка APK: aapt2 → javac → d8 → add_dex → zipalign → apksigner |
| `zerhome/dist/ZerHome-0.1.0.apk` | готовая сборка |

## Сборка

Проект лежит на кириллическом пути, а `aapt2` на Windows такое не переваривает,
поэтому `build.ps1` сначала копирует исходники в `C:\work\zerhome`.

```powershell
cd mobile\marmite\home\zerhome
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Нужны JDK 21 (`C:\Program Files\Java\jdk-21.0.12.1` или `JAVA_HOME`),
`android.jar` (`G:\Android-sdk\platforms\android-36.1\android.jar`), `aapt2.exe`
из кэша Gradle, `d8.jar` из build-tools, `zipalign.exe` и `apksigner.jar` из
`C:\Program Files (x86)\APK Editor Studio\tools`, python 3.

Результат — `zerhome\dist\ZerHome-0.1.0.apk`.

## Как ставился

```powershell
adb install -r .\zerhome\dist\ZerHome-0.1.0.apk
adb shell cmd package set-home-activity com.zeroc.home/.HomeActivity
adb shell settings put global policy_control immersive.navigation=com.zeroc.home
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Проверка без тапов по экрану

`HomeActivity` понимает отладочные extras, поэтому список и его секции можно
открыть из `am start`, а состояние прочитать дампом:

```powershell
adb shell am start -n com.zeroc.home/.HomeActivity --ez open_apps true
adb shell am start -n com.zeroc.home/.HomeActivity --ez open_apps true --ez open_search true
adb shell am start -n com.zeroc.home/.HomeActivity --ez open_apps true --es letter S
adb shell "uiautomator dump /sdcard/ui.xml; cat /sdcard/ui.xml"
```

Плитки и панель отдают `content-desc` (подпись приложения и «Навигация…»),
так что дамп показывает, что нарисовано.

## Архив: эксперимент с Launcher3

Ещё раньше домашним экраном был форк AOSP Launcher3 (`launcher3-q/`,
`build-l3q.ps1`, Magisk-модуль `homewatch`). Он подменял штатный
`TrebuchetQuickStep`, но с прошивкой не ужился: пакетный менеджер не выдавал
новому APK привилегии, и лаунчер падал. Модуль снят, слот возвращён прошивке.
От эксперимента остались плитки и правки под LineageOS 17.1; собрать его можно
по-прежнему:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-l3q.ps1
```

Результат — `dist\Launcher3QuickStep-0.1.0.apk`. Подробности правок —
в истории репозитория.

## Лицензия

`launcher3-q/` — AOSP Launcher3, Apache License 2.0 (см. `NOTICE` и
`MODULE_LICENSE_APACHE2`). `zerhome/` — код ZerOS.
