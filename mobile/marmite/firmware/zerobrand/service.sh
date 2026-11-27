#!/system/bin/sh
# ZerOS branding: звук уведомления ZerOS по умолчанию.
#
# Файл /system/product/media/audio/notifications/ZerOS.ogg кладёт сам модуль.
# Здесь мы один раз подставляем его в настройку, но только если пользователь
# ещё не выбрал свой звук (заводское значение - Argon).
# Отключить: settings put system notification_sound <свой uri>.

[ -f /system/product/media/audio/notifications/ZerOS.ogg ] || exit 0

# ждём завершения загрузки
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 90 ]; do
    sleep 2
    i=$((i + 1))
done

cur=$(settings get system notification_sound 2>/dev/null)
case "$cur" in
    null|"") ;;
    *Argon*) ;;
    *ZerOS*)
        # уже наш звук - но id мог смениться, если базу медиа пересоздали
        content query --uri "${cur%%\?*}" >/dev/null 2>&1 && exit 0
        ;;
    *) exit 0 ;;
esac

# внутренний том media сканируется на загрузке - ждём, пока появится файл
i=0
id=
while [ -z "$id" ] && [ $i -lt 30 ]; do
    id=$(content query --uri content://media/internal/audio/media \
        --projection _id:_display_name \
        --where "_display_name='ZerOS.ogg'" 2>/dev/null \
        | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -n 1)
    [ -n "$id" ] || sleep 2
    i=$((i + 1))
done

[ -n "$id" ] || exit 0

settings put system notification_sound \
    "content://media/internal/audio/media/$id?title=ZerOS&canonical=1"
