# Палитра ZerOS

Основа — цвет иконок из Nothing OS 5 (скриншот 20.11.2026): тёмная марсала.
Пока это дизайн-токены: приложений ZerOS с собственным UI ещё нет, HomeWatch
остаётся стоковым AOSP, поэтому цвет применяется в новых экранах, виджетах и
кнопках, а не перекрашивает чужие иконки.

## Токены

| Токен | HEX | Где использовать |
|---|---|---|
| `zer-accent` | `#4A0A18` | фон акцентных иконок, заливка кнопок, выбранные состояния |
| `zer-on-accent` | `#F2A6A0` | глиф, текст и иконки на `zer-accent` |
| `zer-accent-pressed` | `#5C0D1E` | нажатие и hover |
| `zer-accent-dim` | `#2E060F` | disabled, неактивные плашки |

Контраст `zer-on-accent` на `zer-accent` — **7.9:1** (AAA для обычного текста),
белый на `zer-accent` — **15.6:1**. Для мелкого текста на марсале лучше
`zer-on-accent`, а не белый: он мягче и не «звенит».

## Android

```xml
<color name="zer_accent">#FF4A0A18</color>
<color name="zer_on_accent">#FFF2A6A0</color>
<color name="zer_accent_pressed">#FF5C0D1E</color>
<color name="zer_accent_dim">#FF2E060F</color>
```

Иконка в этом стиле — круг или сквиркл `zer_accent`, глиф `zer_on_accent`,
без градиентов и теней.

На Android 12+ этот же цвет может быть сидом для Monet: из него генерируются
60 системных цветов (`system_accent*`, `system_neutral*`), и тогда марсала
красит всю оболочку. Готовый RRO —
[`../overlays/marsala/`](../overlays/marsala/README.md), применение и замеры —
в [`a5-tuning.md`](a5-tuning.md).

## Web / `.zpk`

```css
:root {
  --zer-accent: #4A0A18;
  --zer-on-accent: #F2A6A0;
  --zer-accent-pressed: #5C0D1E;
  --zer-accent-dim: #2E060F;
}
```

## Где не применять

- Не перекрашивать иконки сторонних приложений в лаунчере: AOSP Launcher3 на
  Android 10 не поддерживает пакеты иконок, а Themed Icons появились только в
  Android 13.
- Не ставить `zer-accent` фоном под обычный текст — только под глифы, иконки и
  крупные кнопки.
- Системные элементы (шторка, настройки) красятся RRO-темой, а не этим файлом.
