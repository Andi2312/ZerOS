# Снимок экрана XS15 с правильной геометрией.
#
# У этой панели буфер 480x640, а видимая область — центральные 360x640,
# поэтому screencap отдаёт картинку 480x640, растянутую по ширине в 4/3.
# Сжимаем обратно до 360x640 — получаем ровно то, что видно на экране.

param(
    [string]$Out = (Join-Path $PSScriptRoot 'dist\screenshot.png'),
    [int]$Width = 360
)

$ErrorActionPreference = 'Stop'

$adb = 'C:\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "adb not found: $adb" }

$remote = '/sdcard/zeroshot.png'
$tmp    = Join-Path $env:TEMP "zeroshot_$PID.png"

& $adb shell screencap -p $remote
if ($LASTEXITCODE -ne 0) { throw 'screencap failed' }
& $adb pull $remote $tmp | Out-Null
& $adb shell rm $remote

New-Item -ItemType Directory -Force (Split-Path -Parent $Out) | Out-Null
& python -c "import sys; from PIL import Image; im=Image.open(sys.argv[1]); w,h=im.size; im=im.resize((int(sys.argv[3]), h), Image.LANCZOS); im.save(sys.argv[2]); print(f'{w}x{h} -> {sys.argv[3]}x{h}')" $tmp $Out $Width
if ($LASTEXITCODE -ne 0) { throw 'resize failed' }

Remove-Item $tmp -Force
Write-Host "screenshot: $Out"
