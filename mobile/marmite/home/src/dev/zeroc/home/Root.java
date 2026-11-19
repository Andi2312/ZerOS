package dev.zeroc.home;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Команды через su. Нужны там, где обычному приложению система не даёт прав:
 * изменение системных настроек (яркость), системная тёмная тема, режим
 * «Не беспокоить» и выдача самому себе appop-разрешения.
 *
 * Важно: {@link #available()} только читает кеш и никогда не запускает su —
 * его зовут с главного потока. Проверка делается в {@link #check()} на фоновом
 * потоке: именно этот вызов и показывает в Magisk запрос «Разрешить root?».
 * Отрицательный ответ не кешируется, чтобы после выдачи прав в Magisk
 * приложение подхватило их без перезапуска.
 */
final class Root {

    private static volatile Boolean granted;

    private Root() {
    }

    /** Кешированный ответ; su не запускает. */
    static boolean available() {
        Boolean known = granted;
        return known != null && known;
    }

    /** Проверка с запуском su: вызывать только не с главного потока. */
    static boolean check() {
        Boolean known = granted;
        if (known != null) {
            return known;
        }
        boolean result = exec("id").contains("uid=0");
        if (result) {
            granted = true;
        }
        return result;
    }

    static String exec(String command) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("su", "-c", command);
            // Ошибки su и stderr самой команды читаем тем же потоком, иначе
            // процесс может повиснуть на заполненном буфере.
            builder.redirectErrorStream(true);
            process = builder.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
            reader.close();
            process.waitFor();
            return out.toString().trim();
        } catch (Exception e) {
            return "";
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /** Яркость + перевод автоподсветки в ручной режим. */
    static void setBrightness(int value255) {
        exec("settings put system screen_brightness_mode 0; "
                + "settings put system screen_brightness " + value255);
    }

    /** Системная тёмная тема LineageOS 17.1. */
    static void setNightMode(boolean dark) {
        exec("cmd uimode night " + (dark ? "yes" : "no"));
    }

    /** zen_mode: 0 — все уведомления, 1 — только приоритетные (DND). */
    static void setZenMode(boolean notificationsOn) {
        exec("settings put global zen_mode " + (notificationsOn ? 0 : 1));
    }

    /** Выдаём сами себе «Изменение системных настроек», чтобы слайдер работал без root. */
    static void grantWriteSettings(String packageName) {
        exec("cmd appops set " + packageName + " android:write_settings allow");
        exec("appops set " + packageName + " WRITE_SETTINGS allow");
    }

    /** То же для политики уведомлений (переключатель «Не беспокоить»). */
    static void grantNotificationPolicy(String packageName) {
        exec("cmd appops set " + packageName + " ACCESS_NOTIFICATION_POLICY allow");
        exec("appops set " + packageName + " ACCESS_NOTIFICATION_POLICY allow");
    }
}
