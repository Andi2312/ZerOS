# Прошивка LineageOS 17.1 (Android 10) на Wileyfox Swift 2 (marmite) через TWRP.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File flash_los.ps1 -Yes
#
# Что делает:
#   1. проверяет, что аппарат на связи и загружен в TWRP (adb);
#   2. сохраняет сведения о текущей системе в logs\;
#   3. ждёт, пока поднимется GUI TWRP, затем `twrp wipe cache` / `wipe data` /
#      `format data` — данные и внутренняя память теряются;
#   4. заливает ROM, ставит его через `twrp install`;
#   5. ставит Magisk v31.0 (если не указан -SkipMagisk);
#   6. перезагружает в систему.
#
# Проверено 17.11.2026 на Swift 2 (a886e8b4): LineageOS 17.1 + Magisk 31.0
# встали с первого прохода. Требует паузу ~40 с после входа в recovery —
# без неё twrp-утилита висит на "TWRP does not appear to be running".
#
# Файл сохранён в UTF-8 **с BOM**: Windows PowerShell 5.1 без BOM читает .ps1
# как ANSI и падает на русских строках.
#
# Ром и Magisk по умолчанию берутся из Загрузок, можно передать свои:
#   -Rom D:\lineage.zip -Magisk D:\Magisk-v31.0.zip

param(
    [string]$Rom = (Join-Path $env:USERPROFILE 'Downloads\lineage-17.1-20210703-UNOFFICIAL-marmite.zip'),
    [string]$Magisk = (Join-Path $env:USERPROFILE 'Downloads\Magisk_v31.0 (2).zip'),
    [switch]$SkipMagisk,
    [switch]$Yes
)

$ErrorActionPreference = 'Stop'
$Adb = 'C:\platform-tools\adb.exe'
$LogDir = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory -Force $LogDir | Out-Null
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$Log = Join-Path $LogDir "flash-$Stamp.log"

function Say([string]$text) {
    $line = "[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $text
    Write-Host $line
    Add-Content -Path $Log -Value $line -Encoding utf8
}

function Adb([string[]]$arguments) {
    $out = & $Adb @arguments 2>&1
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        throw "adb $($arguments -join ' ') -> $code`n$out"
    }
    return $out
}

function Wait-ForState([string]$state, [int]$seconds) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        $list = (& $Adb devices 2>&1 | Out-String)
        if ($list -match "(?m)^\S+\s+$state\s*$") {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

foreach ($file in @($Rom, $Magisk)) {
    if ($SkipMagisk -and $file -eq $Magisk) { continue }
    if (-not (Test-Path $file)) { throw "нет файла: $file" }
}

if (-not $Yes) {
    Write-Host ''
    Write-Host 'ВНИМАНИЕ: data, cache и внутренняя память будут стёрты.'
    Write-Host "ROM   : $Rom"
    Write-Host "Magisk: $(if ($SkipMagisk) { 'не ставим' } else { $Magisk })"
    Write-Host ''
    Write-Host 'Запустите повторно с -Yes, если всё верно.'
    exit 1
}

Say "ROM: $Rom"
Say "Magisk: $(if ($SkipMagisk) { 'пропуск' } else { $Magisk })"

$devices = (& $Adb devices -l 2>&1 | Out-String)
Say "adb devices:`n$devices"
if ($devices -notmatch 'a886e8b4') {
    throw 'Swift 2 (a886e8b4) не найден по adb — подключите кабель и включите отладку'
}

# 1. перезагрузка в рекавери
$inRecovery = $false
if ($devices -match '(?m)^a886e8b4\s+recovery') {
    $inRecovery = $true
    Say 'аппарат уже в recovery'
} else {
    Say 'перезагружаю в recovery'
    & $Adb -s a886e8b4 reboot recovery | Out-Null
    Start-Sleep -Seconds 5
    if (-not (Wait-ForState 'recovery' 180)) {
        throw 'TWRP не поднялся за 180 секунд'
    }
    Say 'TWRP на связи'
}

$twrp = (& $Adb -s a886e8b4 shell getprop ro.twrp.version 2>&1 | Out-String).Trim()
Say "TWRP version: $twrp"
if ([string]::IsNullOrWhiteSpace($twrp)) {
    throw 'это не TWRP (нет ro.twrp.version) — прошивка через twrp install невозможна'
}

# 2. снимок текущей системы
$props = @(
    'ro.product.device', 'ro.build.display.id', 'ro.build.version.release',
    'ro.lineage.version', 'ro.cm.version', 'ro.crdroid.version', 'ro.build.date'
)
$snapshot = New-Object System.Collections.Generic.List[string]
$snapshot.Add("date=$Stamp")
foreach ($p in $props) {
    $value = (& $Adb -s a886e8b4 shell "getprop $p" 2>&1 | Out-String).Trim()
    $snapshot.Add("$p=$value")
}
$snapshotPath = Join-Path $LogDir "before-$Stamp.txt"
$snapshot | Set-Content -Path $snapshotPath -Encoding utf8
Say "снимок системы: $snapshotPath"

# 3. даём интерфейсу TWRP подняться.
# twrp-утилита — это обёртка над openrecoveryscript: она ждёт GUI, и если
# дёрнуть её сразу после подключения adb, висит на
# "TWRP does not appear to be running".
Say 'жду, пока поднимется интерфейс TWRP (40 с)'
Start-Sleep -Seconds 40

function Invoke-Twrp([string]$command) {
    $out = (& $Adb -s a886e8b4 shell "twrp $command" 2>&1 | Out-String).Trim()
    Say "twrp $command -> $out"
    if ($out -match 'does not appear to be running') {
        throw "twrp не видит запущенный TWRP ($command) — подождите и запустите снова"
    }
    return $out
}

# 4. очистка: cache, data, потом полный сброс внутренней памяти.
# 'wipe data' намеренно не трогает /data/media, поэтому нужен 'format data'.
Invoke-Twrp 'wipe cache' | Out-Null
Invoke-Twrp 'wipe data' | Out-Null
Invoke-Twrp 'format data' | Out-Null

if (-not (Wait-ForState 'recovery' 90)) {
    throw 'TWRP потерялся после очистки разделов'
}

# 4. ROM
$romName = Split-Path -Leaf $Rom
Say "заливаю $romName (это долго: ~600 МБ)"
& $Adb -s a886e8b4 push $Rom "/sdcard/$romName" | Out-Null
$size = (& $Adb -s a886e8b4 shell "stat -c %s /sdcard/$romName" 2>&1 | Out-String).Trim()
Say "на телефоне: $size байт (ожидалось $((Get-Item $Rom).Length))"
if ($size -ne "$((Get-Item $Rom).Length)") {
    throw 'размер файла на телефоне не совпал — прерываю'
}

Say 'устанавливаю ROM через twrp install'
$out = Invoke-Twrp "install '/sdcard/$romName'"
if ($out -match 'Error|failed|E1001|E2001|E3004') {
    throw 'TWRP сообщил об ошибке установки ROM'
}
if ($out -notmatch 'script succeeded') {
    throw 'в логе установки нет "script succeeded" — проверьте TWRP вручную'
}

# 5. Magisk
if (-not $SkipMagisk) {
    $magiskName = Split-Path -Leaf $Magisk
    Say "заливаю $magiskName"
    & $Adb -s a886e8b4 push $Magisk "/sdcard/$magiskName" | Out-Null
    $out = Invoke-Twrp "install '/sdcard/$magiskName'"
    if ($out -match 'Error|failed' -or $out -notmatch 'Done') {
        Say 'Magisk не встал — система останется без root (это не блокер)'
    }
}

# 6. загрузка
Say 'перезагружаю в систему'
& $Adb -s a886e8b4 reboot 2>&1 | Out-Null
Start-Sleep -Seconds 15
if (Wait-ForState 'device' 420) {
    $release = (& $Adb -s a886e8b4 shell getprop ro.build.version.release 2>&1 | Out-String).Trim()
    $build = (& $Adb -s a886e8b4 shell getprop ro.build.display.id 2>&1 | Out-String).Trim()
    Say "система поднялась: Android $release / $build"
} else {
    Say 'система не поднялась за 7 минут — смотрите экран телефона'
}

Say 'готово'
