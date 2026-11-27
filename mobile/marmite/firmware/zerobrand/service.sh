#!/system/bin/sh
# ZerOS branding: свои звуки по умолчанию - уведомление Glint, рингтон
# First Light, будильник Sunrise.
#
# Файлы кладёт сам модуль в /system/product/media/audio/{notifications,
# ringtones,alarms}. Здесь мы подставляем их в настройки, но только если
# пользователь ещё не выбрал свои (заводские - Argon / Orion / Argon).
# Отключить: settings put system <ringtone|alarm_alert|notification_sound> ...

MODDIR=${0%/*}

find_id() {
    content query --uri content://media/internal/audio/media \
        --projection _id:_display_name \
        --where "_display_name='$1'" 2>/dev/null \
        | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -n 1
}

# $1 - настройка, $2 - название, $3 - файл, $4 - что можно заменить (glob)
set_default() {
    setting=$1
    title=$2
    file=$3
    replaceable=$4

    cur=$(settings get system "$setting" 2>/dev/null)
    case "$cur" in
        *"$title"*)
            # уже наш звук; id мог смениться, если базу медиа пересоздали
            content query --uri "${cur%%\?*}" >/dev/null 2>&1 && return 0
            ;;
        null|"") ;;
        *)
            matched=0
            for pat in $replaceable; do
                case "$cur" in $pat) matched=1; break ;; esac
            done
            [ "$matched" = 1 ] || return 0
            ;;
    esac

    id=$(find_id "$file")
    [ -n "$id" ] || return 0
    settings put system "$setting" \
        "content://media/internal/audio/media/$id?title=$title&canonical=1"
}

# ждём завершения загрузки
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 90 ]; do
    sleep 2
    i=$((i + 1))
done

# внутренний том media сканируется на загрузке - ждём, пока появятся файлы
i=0
while [ -z "$(find_id FirstLight.ogg)" ] && [ $i -lt 30 ]; do
    sleep 2
    i=$((i + 1))
done

set_default notification_sound Glint Glint.ogg '*Argon* *ZerOS*'
set_default ringtone FirstLight FirstLight.ogg '*Orion*'
set_default alarm_alert Sunrise Sunrise.ogg '*Argon*'
