#!/system/bin/sh
# zerobrand: вернуть обои, которые стояли до установки модуля.
# Восстанавливаем только если с тех пор их не меняли: если картинка в
# /data/system/users/0/wallpaper уже не наша, ничего не трогаем.

MODDIR=${0%/*}
STATE=/data/adb/zerobrand
DIR=/data/system/users/0
WALL=$DIR/wallpaper
INFO=$DIR/wallpaper_info.xml
ASSET=$MODDIR/wallpaper/wallpaper.jpg

[ -f "$STATE/wallpaper.applied" ] || exit 0

if ! cmp -s "$WALL" "$ASSET"; then
    rm -rf "$STATE"
    exit 0
fi

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

rm -rf "$STATE"
