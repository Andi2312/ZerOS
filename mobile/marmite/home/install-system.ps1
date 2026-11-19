# Установка ZerOS Home как системного приложения в /system/priv-app.
#
# Требует AVD, запущенный с -writable-system (см. avd.ps1) — иначе remount
# отдаёт «Read-only file system» и скрипт останавливается.
#
# После установки делает reboot: PackageManager сканирует /system только при
# загрузке, поэтому до перезагрузки приложение остаётся прежним.

param(
    [string]$Apk = (Join-Path $PSScriptRoot 'dist\ZerOS_Home.apk'),
    [string]$Serial = 'emulator-5554',
    [switch]$NoReboot
)

$adb = 'C:\platform-tools\adb.exe'
$pkg = 'dev.zeroc.home'
$target = '/system/priv-app/ZerOSHome'

if (-not (Test-Path $Apk)) { throw "APK не найден: $Apk" }

& $adb -s $Serial root | Out-Null
Start-Sleep -Seconds 2
& $adb -s $Serial wait-for-device

$remount = (& $adb -s $Serial remount) 2>&1 | Out-String
if ($remount -match 'remount failed') {
    throw 'remount не удался: AVD запущен без -writable-system. Запусти shell\avd.ps1'
}

# пользовательскую копию убираем, иначе система может отдать /data/app вместо /system
& $adb -s $Serial uninstall $pkg 2>&1 | Out-Null

& $adb -s $Serial shell "rm -rf $target; mkdir -p $target"
& $adb -s $Serial push $Apk "$target/ZerOSHome.apk" | Out-Null
& $adb -s $Serial shell "chmod 644 $target/ZerOSHome.apk; chown root:root $target/ZerOSHome.apk; restorecon -R $target"

"--- в /system ---"
& $adb -s $Serial shell "ls -lZ $target"

if (-not $NoReboot) {
    & $adb -s $Serial reboot | Out-Null

    # На AVD `adb reboot` иногда просто гасит эмулятор вместо перезагрузки гостя.
    # Ждём с дедлайном и, если устройство не вернулось, поднимаем AVD заново.
    $back = $false
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        if (& $adb devices | Select-String -Pattern "^$Serial\s+device") { $back = $true; break }
        Start-Sleep -Seconds 3
    }

    if (-not $back) {
        "adb reboot закрыл эмулятор — поднимаю заново через avd.ps1"
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'avd.ps1') | Out-Null
    }

    for ($i = 0; $i -lt 80; $i++) {
        if ("$(& $adb -s $Serial shell getprop sys.boot_completed 2>$null)".Trim() -eq '1') { break }
        Start-Sleep -Seconds 3
    }
}

"--- где живёт пакет ---"
& $adb -s $Serial shell "pm path $pkg"
& $adb -s $Serial shell "cmd package set-home-activity $pkg/.MainActivity"
"--- домашний экран ---"
& $adb -s $Serial shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1"
