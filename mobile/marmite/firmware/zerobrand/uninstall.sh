#!/system/bin/sh
# zerobrand: вернуть заводской звук уведомления (Argon), если стоит ZerOS.
# Иначе после удаления модуля настройка указывала бы на пропавший файл.

cur=$(settings get system notification_sound 2>/dev/null)
case "$cur" in
    *ZerOS*) ;;
    *) exit 0 ;;
esac

i=0
id=
while [ -z "$id" ] && [ $i -lt 10 ]; do
    id=$(content query --uri content://media/internal/audio/media \
        --projection _id:_display_name \
        --where "_display_name='Argon.ogg'" 2>/dev/null \
        | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -n 1)
    [ -n "$id" ] || sleep 1
    i=$((i + 1))
done

[ -n "$id" ] || exit 0

settings put system notification_sound \
    "content://media/internal/audio/media/$id?title=Argon&canonical=1"
