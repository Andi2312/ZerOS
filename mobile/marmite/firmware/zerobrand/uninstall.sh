#!/system/bin/sh
# zerobrand: откатить то, что модуль поменял при установке.
# Возвращаем только то, что с тех пор не меняли: если обои или тему
# пользователь уже переставил сам, ничего не трогаем.

MODDIR=${0%/*}
STATE=/data/adb/zerobrand
DIR=/data/system/users/0
WALL=$DIR/wallpaper
INFO=$DIR/wallpaper_info.xml
ASSET=$MODDIR/wallpaper/wallpaper.jpg
KEY=theme_customization_overlay_packages

# --- обои -------------------------------------------------------------
if [ -f "$STATE/wallpaper.applied" ]; then
    if cmp -s "$WALL" "$ASSET"; then
        if [ -f "$STATE/wallpaper.orig" ]; then
            cp -f "$STATE/wallpaper.orig" "$WALL"
            chmod 600 "$WALL" 2>/dev/null
            chown system:system "$WALL" 2>/dev/null
            restorecon "$WALL" 2>/dev/null
        fi
        if [ -f "$STATE/wallpaper_info.orig.xml" ]; then
            cp -f "$STATE/wallpaper_info.orig.xml" "$INFO"
            chmod 600 "$INFO" 2>/dev/null
            chown system:system "$INFO" 2>/dev/null
            restorecon "$INFO" 2>/dev/null
        fi
    fi
    rm -f "$STATE/wallpaper.applied"
fi

# --- тема -------------------------------------------------------------
if [ -f "$STATE/theme.applied" ]; then
    norm() {
        printf '%s' "$1" | sed 's/,"_applied_timestamp"[[:space:]]*:[[:space:]]*[0-9][0-9]*//'
    }
    cur=$(norm "$(settings get secure "$KEY" 2>/dev/null)")
    applied=$(norm "$(cat "$STATE/theme.json" 2>/dev/null)")

    if [ -n "$applied" ] && [ "$cur" = "$applied" ]; then
        # выбор не меняли - снимаем наши оверлеи и возвращаем прежний
        font=$(printf '%s' "$applied" | sed -n 's/.*"font"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        accent=$(printf '%s' "$applied" | sed -n 's/.*"accent_color"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        shape=$(printf '%s' "$applied" | sed -n 's/.*"adaptive_icon_shape"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        for pkg in "$font" "$accent" "$shape"; do
            [ -n "$pkg" ] && cmd overlay disable --user 0 "$pkg" >/dev/null 2>&1
        done

        # возвращаем только правдоподобный прежний выбор темы
        orig=$(cat "$STATE/theme.orig.json" 2>/dev/null)
        case "$orig" in
            *theme.customization*) ;;
            *) orig=null ;;
        esac
        if [ "$orig" != "null" ]; then
            settings put secure "$KEY" "$orig" >/dev/null 2>&1
            for pkg in $(printf '%s' "$orig" | sed -n 's/[^{}"]*"\(org\.lineageos\.overlay\.[^"]*\|com\.android\.theme\.[^"]*\)"/\1 /gp'); do
                cmd overlay enable --user 0 "$pkg" >/dev/null 2>&1
            done
        else
            settings delete secure "$KEY" >/dev/null 2>&1
        fi
    fi
    rm -f "$STATE/theme.applied"
fi

rm -rf "$STATE"
