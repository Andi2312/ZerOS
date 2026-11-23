# Настройка A5 2016 (a5xelte, LineageOS 20)

Всё ниже сделано на `SM-A510F` (`a5xelte`), LineageOS 20 (Android 13),
`userdebug`, 1,8 ГБ ОЗУ, Mali-T720. Каждый пункт обратим и не требует
перепрошивки — значения лежат в `/data`, переживают перезагрузку, сбрасываются
одной командой.

## 1. Экран: меньше пикселей, тот же размер интерфейса

```powershell
adb shell wm size 810x1440
adb shell wm density 360
```

810/360 dp = 1080/480 dp, то есть вёрстка остаётся прежней, а рендерится
на 44% меньше пикселей. Проверка — в `dumpsys display`:

```
mOverrideDisplayInfo: real 810 x 1440, density 360
```

Промежуточный вариант, если картинка кажется слишком мягкой: `wm size 900x1600`
+ `wm density 400` (25% экономии). Если наоборот нужно больше контента на
экране: `wm density 420` при том же разрешении.

## 2. Отзывчивость

```powershell
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
adb shell cmd device_config put activity_manager max_cached_processes 20
```

`max_cached_processes` (было 32) ограничивает число кэшированных фоновых
процессов: при 1,8 ГБ ОЗУ система меньше уходит в swap. Значение живёт в
`device_config` и видно в `dumpsys activity settings` как
`CUR_MAX_CACHED_PROCESSES=20`.

Размытие на этой прошивке уже выключено (`mBlurEnabled=false`) — трогать нечего.

## 3. Monet в цветах ZerOS

На Android 12+ палитру собирает сам SystemUI: фабрикует оверлеи
`com.android.systemui:accent` и `:neutral` (target `android`,
`mPriority=2147483647`) из одного сида. На LineageOS 20 сид берётся из
`Settings.Secure.theme_customization_overlay_packages` — того же JSON, что
пишет штатный выбор цвета («Стили»). Достаточно подменить в нём три поля, и
SystemUI сгенерирует марсалу сама, без сторонних APK:

```json
"android.theme.customization.system_palette": "4A0A18",
"android.theme.customization.accent_color": "4A0A18",
"android.theme.customization.theme_style": "TONAL_SPOT",
"android.theme.customization.color_source": "preset"
```

- `theme_style` — стиль из `packages/SystemUI/monet/ColorScheme.kt`:
  `TONAL_SPOT`, `VIBRANT`, `EXPRESSIVE`, `FRUIT_SALAD`, `RAINBOW`, `SPRITZ`.
- `color_source: preset` запрещает перегенерировать палитру при смене обоев.
- `#` в значении не нужен, SystemUI подставит сам.
- Остальные поля JSON (шрифт, форма иконок, наборы иконок) не трогаем.

Применить (значение читается из файла — иначе кавычки JSON ломают `settings`):

```powershell
adb push theme_new.json /data/local/tmp/theme_new.json
'settings put secure theme_customization_overlay_packages "$(cat /data/local/tmp/theme_new.json)"' | adb shell
adb shell killall com.android.systemui
```

Проверка: `cmd overlay lookup android android:color/system_accent1_500`
→ `#ffab6069`, `system_neutral2_500` → `#ff837273`. Все 60 цветов совпадают
с TONAL_SPOT из `#4A0A18` (сверено по `dumpsys overlay`).

Цвета зашиты и в RRO — [`../overlays/marsala/`](../overlays/marsala/README.md);
он нужен для прошивок без лосевского выбора цвета (и для нашей сборки, где
оверлей ляжет в `/product/overlay`). На этой прошивке RRO не нужен: палитра
из настройки переживает перезагрузку и смену обоев.

## Замеры

Холодный старт «Настроек» (`am start -W`, три прогона, `TotalTime`):
0,83–0,90 с — после правок; до них 0,83–0,90 с тоже, то есть на этом
устройстве разницу даёт не время запуска, а плавность прокрутки и отсутствие
перерисовки в 1080p.

## Откат

```powershell
adb shell wm size reset
adb shell wm density reset
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0
adb shell cmd device_config delete activity_manager max_cached_processes
```

Цвета: в «Стилях» выбрать любой другой цвет или вернуть прежний JSON
(`system_palette` и `accent_color` = `CCEE99`, `theme_style` = `FRUIT_SALAD`),
затем `adb shell killall com.android.systemui`.

RRO, если он ставился:

```powershell
adb shell cmd overlay disable org.zer.os.overlay.marsala
adb shell cmd overlay enable com.android.systemui:accent
adb shell cmd overlay enable com.android.systemui:neutral
adb shell killall com.android.systemui
adb uninstall org.zer.os.overlay.marsala
```
