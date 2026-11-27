#!/system/bin/sh
# zerobrand: вернуть заводские звуки, если стоят наши, иначе после удаления
# модуля настройки будут указывать на пропавшие файлы.

find_id() {
    content query --uri content://media/internal/audio/media \
        --projection _id:_display_name \
        --where "_display_name='$1'" 2>/dev/null \
        | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -n 1
}

# $1 - настройка, $2 - наше название, $3 - заводской файл, $4 - заводское название
restore() {
    cur=$(settings get system "$1" 2>/dev/null)
    case "$cur" in
        *"$2"*) ;;
        *) return 0 ;;
    esac

    i=0
    id=
    while [ -z "$id" ] && [ $i -lt 10 ]; do
        id=$(find_id "$3")
        [ -n "$id" ] || sleep 1
        i=$((i + 1))
    done
    [ -n "$id" ] || return 0
    settings put system "$1" \
        "content://media/internal/audio/media/$id?title=$4&canonical=1"
}

restore notification_sound Glint Argon.ogg Argon
restore ringtone FirstLight Orion.ogg Orion
restore alarm_alert Sunrise Argon.ogg Argon
