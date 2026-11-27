#!/system/bin/sh
# ZerOS: тема по умолчанию - та, что настроена на телефоне сейчас:
# акцент "pink", форма иконок "roundedrect", шрифт Rubik.
#
# Magisk-модуль не может переписать встроенную тему по умолчанию в ROM,
# поэтому тема выставляется один раз, после первой загрузки с модулем -
# теми же действиями, что делает "Стили" (ThemePicker): в каждой категории
# остаётся ровно один оверлей, выбор пишется в secure-настройки.
# Дальше скрипт молчит: если пользователь сменит тему, она останется.
#
# Маркер - /data/adb/zerobrand/theme.applied, выставленная тема -
# theme.json, прежний выбор - theme.orig.json (uninstall.sh вернёт его).

MODDIR=${0%/*}
STATE=/data/adb/zerobrand
MARK=$STATE/theme.applied
KEY=theme_customization_overlay_packages

FONT=org.lineageos.overlay.font.rubik
ACCENT=org.lineageos.overlay.accent.pink
SHAPE=com.android.theme.icon.roundedrect

[ -f "$MARK" ] && exit 0

JSON="{\"android.theme.customization.font\":\"$FONT\","\
"\"android.theme.customization.accent_color\":\"$ACCENT\","\
"\"android.theme.customization.adaptive_icon_shape\":\"$SHAPE\","\
"\"_applied_timestamp\":$(date +%s)000}"

# system_server поднимается не сразу: до этого нет ни settings, ни cmd overlay
i=0
while [ "$(getprop sys.boot_completed)" != 1 ] && [ $i -lt 300 ]; do
    sleep 1
    i=$((i + 1))
done
[ "$(getprop sys.boot_completed)" = 1 ] || exit 0

sleep 3
mkdir -p "$STATE"

# прежний выбор темы - только один раз, для отката. Сохраняем только
# похожее на выбор темы: если settings в этот момент не ответил, в файл
# попадёт пустая строка или текст ошибки, и откатывать будет нечего.
if [ ! -f "$STATE/theme.orig.json" ]; then
    old=$(settings get secure "$KEY" 2>/dev/null)
    case "$old" in
        *theme.customization*) printf '%s\n' "$old" > "$STATE/theme.orig.json" ;;
    esac
fi

# Снимаем чужие акценты, шрифты и формы - в категории должен остаться
# один оверлей, иначе система выберет его сама, не спросив "Стили".
# enable-exclusive из cmd overlay для этого не годится: без --category он
# гасит и соседние категории (проверено на marmite).
for pkg in $(cmd overlay list 2>/dev/null | sed -n 's/^\[[ x]\] \([^ ][^ ]*\)$/\1/p'); do
    case "$pkg" in
        "$ACCENT"|"$FONT"|"$SHAPE") continue ;;
        org.lineageos.overlay.accent.*|com.android.theme.color.*|\
        org.lineageos.overlay.font.*|com.android.theme.font.*|\
        com.android.theme.icon.*)
            cmd overlay disable --user 0 "$pkg" >/dev/null 2>&1
            ;;
    esac
done

# включаем и проверяем результат: на заблокированном экране команда
# может не пройти, поэтому повторяем, а не верим одному коду возврата
n=0
while [ $n -lt 40 ]; do
    n=$((n + 1))
    for pkg in "$FONT" "$ACCENT" "$SHAPE"; do
        cmd overlay enable --user 0 "$pkg" >/dev/null 2>&1
    done
    settings put secure "$KEY" "$JSON" >/dev/null 2>&1

    ok=1
    list=$(cmd overlay list 2>/dev/null)
    for pkg in "$FONT" "$ACCENT" "$SHAPE"; do
        printf '%s\n' "$list" | grep -q "^\[x\] $pkg\$" || ok=0
    done
    saved=$(settings get secure "$KEY" 2>/dev/null)
    [ "$saved" = "$JSON" ] || ok=0
    if [ $ok = 1 ]; then
        printf '%s\n' "$JSON" > "$STATE/theme.json"
        touch "$MARK"
        break
    fi
    sleep 5
done
