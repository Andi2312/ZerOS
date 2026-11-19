# Запуск AVD Pixel 4 для работы с ZerOS.
#
# Отличия от обычного запуска из Android Studio:
#   -writable-system  — позволяет `adb remount` и правку /system (root-сценарии)
#   -no-snapshot      — холодная загрузка, чтобы overlay-образ не конфликтовал
#
# Загрузочная анимация включена (это обычный запуск, как на устройстве).
# Без консольного окна запускает `ZerOS AVD.vbs` рядом с этим файлом.
#
# Vulkan отключён глобально в %USERPROFILE%\.android\advancedFeatures.ini,
# иначе emulator 37.x падает на GTX 960.

param(
    [string]$Avd = 'Pixel_4',
    [switch]$Wipe
)

$emulator = 'G:\Android-sdk\emulator\emulator.exe'
$adb = 'C:\platform-tools\adb.exe'

Get-Process -Name 'emulator*', 'qemu-system*' -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 4

$arguments = @('-avd', $Avd, '-no-snapshot', '-no-audio', '-writable-system')
if ($Wipe) { $arguments += '-wipe-data' }

$log = Join-Path $env:TEMP 'opencode\emu_writable.txt'
$err = Join-Path $env:TEMP 'opencode\emu_writable_err.txt'
Remove-Item $log, $err -Force -ErrorAction SilentlyContinue

Start-Process -FilePath $emulator -ArgumentList $arguments `
    -RedirectStandardOutput $log -RedirectStandardError $err | Out-Null

for ($i = 0; $i -lt 60; $i++) {
    $line = & $adb devices | Select-String -Pattern '^emulator-\d+\s+device'
    if ($line) { break }
    Start-Sleep -Seconds 2
}

for ($i = 0; $i -lt 80; $i++) {
    if ("$(& $adb shell getprop sys.boot_completed 2>$null)".Trim() -eq '1') { break }
    Start-Sleep -Seconds 3
}

& $adb root | Out-Null
Start-Sleep -Seconds 2
& $adb wait-for-device
& $adb remount 2>&1 | Select-Object -Last 2

"device : $((& $adb devices | Select-String 'emulator-\d+\s+device').Line)"
"boot   : $((& $adb shell getprop sys.boot_completed).Trim())"
"uid    : $((& $adb shell id).Trim())"
