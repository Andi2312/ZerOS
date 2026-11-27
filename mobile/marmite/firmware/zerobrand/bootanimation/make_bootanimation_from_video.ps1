<#
make_bootanimation_from_video.ps1 - собирает bootanimation.zip из видео.

Кадры вытаскивает ffmpeg, архив пакует 7-Zip в режиме -mx0 (stored):
Android bootanimation читает только несжатые кадры, deflate даёт чёрный
экран. 7-Zip же кладёт в zip и запись каталога part0/ - это тоже требуется.

По умолчанию делается пинг-понг (вперёд + назад): если видео не является
бесшовной петлёй, стык последнего кадра с первым был бы виден, а так
анимация крутится без рывков. -NoPingPong - лить цикл как есть.

Запуск:
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation_from_video.ps1
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation_from_video.ps1 -Video zeros.mp4 -Out ..\bootanimation.zip

ffmpeg берётся из PATH, иначе из ShareX/CapCut; 7-Zip - из PATH, иначе из
Program Files\7-Zip или AOMEI. Пути можно задать параметрами.
#>

param(
    [string]$Video = '',
    [string]$Out = '',
    [int]$Width = 720,
    [int]$Height = 1280,
    [int]$Fps = 30,
    [string]$Ffmpeg = '',
    [string]$SevenZip = '',
    [switch]$NoPingPong
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Video) { $Video = Join-Path $Root 'zeros.mp4' }
if (-not (Test-Path $Video)) { throw "нет видео: $Video" }
if (-not $Out) { $Out = Join-Path (Split-Path -Parent $Root) 'bootanimation.zip' }

function Find-Tool([string]$given, [string]$name, [string[]]$candidates) {
    if ($given) {
        if (Test-Path $given) { return $given }
        throw "не найден $name : $given"
    }
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    throw "не найден $name : укажи путь параметром"
}

$ff = Find-Tool $Ffmpeg 'ffmpeg' @(
    'C:\Program Files\ShareX\ffmpeg.exe',
    "$env:LOCALAPPDATA\CapCut\Apps\9.5.0.4050\ffmpeg.exe"
)
$sz = Find-Tool $SevenZip '7z' @(
    'C:\Program Files\7-Zip\7z.exe',
    'C:\Program Files (x86)\7-Zip\7z.exe',
    'C:\Program Files (x86)\AOMEI Partition Assistant\7z.exe'
)

$Tmp = Join-Path 'C:\work' ('zeros-boot-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
$All = Join-Path $Tmp 'all'
$Pack = Join-Path $Tmp 'pack'
New-Item -ItemType Directory -Force -Path $All, (Join-Path $Pack 'part0') | Out-Null

try {
    # 1. кадры: без звука, ровно под экран, фиксированный fps
    $vf = 'scale={0}:{1}:force_original_aspect_ratio=decrease,pad={0}:{1}:(ow-iw)/2:(oh-ih)/2:black' -f $Width, $Height
    & $ff -hide_banner -loglevel error -i $Video -an -vf $vf -r $Fps (Join-Path $All 'frame_%04d.png')
    if ($LASTEXITCODE -ne 0) { throw 'ffmpeg не смог разобрать видео' }

    $frames = @(Get-ChildItem (Join-Path $All '*.png') | Sort-Object Name)
    if ($frames.Count -lt 2) { throw 'ffmpeg не выдал кадров' }

    # 2. порядок в part0: вперёд, потом назад (без повтора крайних кадров)
    $order = [System.Collections.Generic.List[object]]::new()
    foreach ($f in $frames) { $order.Add($f) }
    if (-not $NoPingPong) {
        for ($i = $frames.Count - 2; $i -ge 1; $i--) { $order.Add($frames[$i]) }
    }

    $part = Join-Path $Pack 'part0'
    for ($i = 0; $i -lt $order.Count; $i++) {
        $dst = Join-Path $part ('frame_{0:d4}.png' -f $i)
        New-Item -ItemType HardLink -Path $dst -Target $order[$i].FullName | Out-Null
    }

    # 3. desc.txt: экран, fps, одна часть - цикл
    $desc = "$Width $Height $Fps`np 0 0 part0`n"
    [System.IO.File]::WriteAllText((Join-Path $Pack 'desc.txt'), $desc, (New-Object System.Text.UTF8Encoding($false)))

    # 4. zip: -mx0 = stored; каталог part0/ попадает в архив при указании папки
    $zip = Join-Path $Tmp 'bootanimation.zip'
    Push-Location $Pack
    try {
        & $sz a -tzip -mx0 $zip 'desc.txt' 'part0' | Out-Null
        if ($LASTEXITCODE -ne 0) { throw '7z не смог собрать архив' }
    } finally { Pop-Location }

    # 5. проверка: все записи несжатые, каталог part0/ на месте
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $za = [System.IO.Compression.ZipFile]::OpenRead($zip)
    try {
        $entries = $za.Entries
        $packed = @($entries | Where-Object { $_.CompressedLength -ne $_.Length })
        if ($packed.Count) { throw ('в архиве сжатые записи: ' + ($packed.FullName -join ', ')) }
        if (-not ($entries | Where-Object { $_.FullName -eq 'part0/' })) { throw 'в архиве нет записи каталога part0/' }
        $count = @($entries | Where-Object { $_.FullName -like 'part0/*.png' }).Count
        Write-Host ("кадров в part0: {0} ({1} шт. видео, пинг-понг: {2})" -f $count, $frames.Count, (-not $NoPingPong))
    } finally { $za.Dispose() }

    if (Test-Path $Out) { Remove-Item $Out -Force }
    Move-Item $zip $Out
    Write-Host ("готово: {0} ({1:N1} МБ)" -f $Out, ((Get-Item $Out).Length / 1MB))
} finally {
    Remove-Item $Tmp -Recurse -Force -ErrorAction SilentlyContinue
}
