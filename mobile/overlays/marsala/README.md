# ZerOS Marsala — RRO для Monet

Оверлей подменяет 60 системных цветов `android:color/system_accent{1,2,3}_*`
и `system_neutral{1,2}_*` — те самые, из которых Android 12+ (Monet) собирает
всю тему: акценты, фон, шторку, настройки.

> На LineageOS 20 (и любой прошивке с лосевским выбором цвета) этот APK не
> нужен: та же палитра получается штатно, через
> `Settings.Secure.theme_customization_overlay_packages` —
> см. [`../../docs/a5-tuning.md`](../../docs/a5-tuning.md). RRO остаётся для
> прошивок без такой настройки и для нашей сборки, где он ляжет в
> `/product/overlay`.

Палитра не «нарисована вручную»: она сгенерирована тем же алгоритмом, что и в
прошивке (`LineageOS 20`, `packages/SystemUI/monet/ColorScheme.kt` и
`Shades.java`), стиль **TONAL_SPOT**, сид — `#4A0A18` из `docs/palette.md`:

| Группа | Оттенок | Хрома |
|---|---|---|
| `accent1` | hue | 36 |
| `accent2` | hue | 16 |
| `accent3` | hue + 60° | 24 |
| `neutral1` | hue | 4 |
| `neutral2` | hue | 8 |

Тона — `99, 95, 90, 80, 70, 60, 49.6, 40, 30, 20, 10, 0`; для первых двух
хрома ограничена 40 (иначе жёлтые и синие оттенки «звенят» на светлом).

## Сборка

```powershell
cd mobile\overlays\marsala
.\build.ps1
```

Скрипт копирует исходники в `C:\work\zeros\overlay-marsala` (путь репозитория
может быть с кириллицей, aapt2 её не переваривает), собирает APK через `aapt2`
и подписывает platform-ключом.

Почему platform-ключ: оверлей бьёт по пакету `android`, а он подписан
платформенным ключом. На a5xelte (LineageOS 20, `dev-keys`) это стандартный
AOSP-ключ, он же лежит в `C:\work\settings_apk\keys\platform.*`. На релизной
сборке со своими ключами оверлей подписывается ими же и кладётся в
`/product/overlay` — тогда он статический и ставить APK в `/data` не нужно.

## Установка

```powershell
adb install -r C:\work\zeros\overlay-marsala\ZerOS-marsala.apk
adb shell cmd overlay enable org.zer.os.overlay.marsala
adb shell cmd overlay disable com.android.systemui:accent
adb shell cmd overlay disable com.android.systemui:neutral
adb shell killall com.android.systemui
```

`com.android.systemui:accent` и `:neutral` — оверлеи, которые SystemUI
фабрикует из обоев; у них максимальный приоритет, поэтому их надо выключить,
иначе они перекроют наши цвета.

Проверка, что легло:

```powershell
adb shell cmd overlay lookup android android:color/system_accent1_500
# ожидается #ffab6069 (accent1, тон 49.6)
```

## Откат

```powershell
adb shell cmd overlay disable org.zer.os.overlay.marsala
adb shell cmd overlay enable com.android.systemui:accent
adb shell cmd overlay enable com.android.systemui:neutral
adb shell killall com.android.systemui
adb uninstall org.zer.os.overlay.marsala
```

Если SystemUI снова сгенерирует свои оверлеи сам (смена обоев, переключение
светлой/тёмной темы), он их включит заново — тогда повторить `disable` и
`killall`.
