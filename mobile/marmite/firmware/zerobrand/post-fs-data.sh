#!/system/bin/sh
# ZerOS: обои по умолчанию ("Орбита", 720x1280 - ровно экран marmite).
#
# Картинка кладётся в /data/system/users/0/wallpaper один раз, при первом
# запуске после установки модуля. Дальше скрипт молчит: если пользователь
# потом выберет другие обои, они останутся. Маркер - /data/adb/zerobrand.
# Старые обои сохраняются там же, uninstall.sh вернёт их при удалении модуля.

MODDIR=${0%/*}
STATE=/data/adb/zerobrand
DIR=/data/system/users/0
WALL=$DIR/wallpaper
INFO=$DIR/wallpaper_info.xml
SRC=$MODDIR/wallpaper/wallpaper.jpg
INFO_SRC=$MODDIR/wallpaper/wallpaper_info.xml

[ -f "$STATE/wallpaper.applied" ] && exit 0
[ -f "$SRC" ] && [ -f "$INFO_SRC" ] || exit 0

# /data к этому моменту расшифрован, но на всякий случай подождём
i=0
while [ ! -d "$DIR" ] && [ $i -lt 30 ]; do
    sleep 1
    i=$((i + 1))
done
[ -d "$DIR" ] || exit 0

mkdir -p "$STATE"

# бэкап прежних обоев - для отката
if [ ! -f "$STATE/wallpaper.orig" ] && [ -f "$WALL" ]; then
    cp -f "$WALL" "$STATE/wallpaper.orig" 2>/dev/null
fi
if [ ! -f "$STATE/wallpaper_info.orig.xml" ] && [ -f "$INFO" ]; then
    cp -f "$INFO" "$STATE/wallpaper_info.orig.xml" 2>/dev/null
fi

# id оставляем прежним: для системы это те же обои, только картинка новая
id=$(sed -n 's/.*id="\([0-9][0-9]*\)".*/\1/p' "$INFO" 2>/dev/null | head -n 1)
[ -n "$id" ] || id=1

cp -f "$SRC" "$WALL"
chmod 600 "$WALL" 2>/dev/null
chown system:system "$WALL" 2>/dev/null
restorecon "$WALL" 2>/dev/null

sed "s/@ID@/$id/" "$INFO_SRC" > "$INFO"
chmod 600 "$INFO" 2>/dev/null
chown system:system "$INFO" 2>/dev/null
restorecon "$INFO" 2>/dev/null

touch "$STATE/wallpaper.applied"
